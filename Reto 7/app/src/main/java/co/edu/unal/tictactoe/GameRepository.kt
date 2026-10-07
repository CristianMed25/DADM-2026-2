package co.edu.unal.tictactoe

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.FirebaseApp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Query
import com.google.firebase.database.ServerValue
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import java.util.UUID

class GameRepository private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val database: FirebaseDatabase = initDatabase(context.applicationContext)

    private val gamesRef = database.getReference(GAMES_PATH)

    private val waitingQuery: Query =
        gamesRef.orderByChild(KEY_STATUS).equalTo(GameState.STATUS_WAITING)

    val playerId: String = loadPlayerId()

    var playerName: String
        get() = prefs.getString(KEY_PLAYER_NAME, null).orEmpty()
        set(value) {
            prefs.edit().putString(KEY_PLAYER_NAME, value.trim()).apply()
        }

    var currentGameId: String?
        get() = prefs.getString(KEY_CURRENT_GAME, null)
        set(value) {
            prefs.edit().putString(KEY_CURRENT_GAME, value).apply()
        }

    fun displayName(): String {
        val saved = playerName
        if (saved.isNotEmpty()) return saved
        return DEFAULT_PLAYER_NAME + playerId.takeLast(4).uppercase()
    }

    fun observeGames(onChanged: (List<GameState>) -> Unit): ValueEventListener {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val games = snapshot.children
                    .mapNotNull { child ->
                        GameState.fromMap(child.key.orEmpty(), child.value)
                    }
                    .filter { it.isJoinable() }
                    .sortedByDescending { it.createdAt }
                onChanged(games)
            }

            override fun onCancelled(error: DatabaseError) {
                onChanged(emptyList())
            }
        }
        waitingQuery.addValueEventListener(listener)
        return listener
    }

    fun stopObservingGames(listener: ValueEventListener) {
        waitingQuery.removeEventListener(listener)
    }

    fun observeGame(gameId: String, onChanged: (GameState?) -> Unit): ValueEventListener {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                onChanged(GameState.fromMap(gameId, snapshot.value))
            }

            override fun onCancelled(error: DatabaseError) {
                onChanged(null)
            }
        }
        gamesRef.child(gameId).addValueEventListener(listener)
        return listener
    }

    fun stopObservingGame(gameId: String, listener: ValueEventListener) {
        gamesRef.child(gameId).removeEventListener(listener)
    }

    fun createGame(onSuccess: (String) -> Unit, onFailure: (String) -> Unit) {
        val gameId = gamesRef.push().key
        if (gameId == null) {
            onFailure(ERROR_CREATE)
            return
        }

        val state = GameState(
            id = gameId,
            status = GameState.STATUS_WAITING,
            hostId = playerId,
            hostName = displayName(),
            board = GameState.EMPTY_BOARD,
            turn = GameState.SYMBOL_X
        )

        val data = state.toMap().toMutableMap()
        data[KEY_CREATED_AT] = ServerValue.TIMESTAMP

        gamesRef.child(gameId).setValue(data)
            .addOnSuccessListener {
                currentGameId = gameId
                onSuccess(gameId)
            }
            .addOnFailureListener { onFailure(it.message ?: ERROR_CREATE) }
    }

    fun joinGame(gameId: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        gamesRef.child(gameId).runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val state = GameState.fromMap(gameId, currentData.value)
                if (state == null || !state.isJoinable() || state.hostId == playerId) {
                    return Transaction.abort()
                }
                currentData.value = state.copy(
                    guestId = playerId,
                    guestName = displayName(),
                    status = GameState.STATUS_PLAYING,
                    turn = GameState.SYMBOL_X
                ).toMap()
                return Transaction.success(currentData)
            }

            override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {
                when {
                    error != null -> onFailure(error.message ?: ERROR_JOIN)
                    !committed -> onFailure(ERROR_JOIN)
                    else -> {
                        currentGameId = gameId
                        onSuccess()
                    }
                }
            }
        })
    }

    fun makeMove(gameId: String, position: Int, onFailure: (String) -> Unit) {
        gamesRef.child(gameId).runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val state = GameState.fromMap(gameId, currentData.value)
                    ?: return Transaction.abort()
                val symbol = state.symbolOf(playerId)
                    ?: return Transaction.abort()
                val updated = GameState.applyMove(state, position, symbol)
                    ?: return Transaction.abort()

                currentData.value = updated.toMap()
                return Transaction.success(currentData)
            }

            override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {
                when {
                    error != null -> onFailure(error.message ?: ERROR_MOVE)
                    !committed -> onFailure(ERROR_MOVE)
                }
            }
        })
    }

    fun leaveGame(onDone: () -> Unit) {
        val gameId = currentGameId
        currentGameId = null
        if (gameId.isNullOrEmpty()) {
            onDone()
            return
        }

        val ref = gamesRef.child(gameId)
        ref.get()
            .addOnSuccessListener { snapshot ->
                val state = GameState.fromMap(gameId, snapshot.value)
                when {
                    state == null -> onDone()
                    state.status == GameState.STATUS_WAITING && state.isHost(playerId) -> {
                        ref.removeValue().addOnCompleteListener { onDone() }
                    }
                    state.status == GameState.STATUS_PLAYING && state.isParticipant(playerId) -> {
                        ref.updateChildren(
                            mapOf(
                                KEY_STATUS to GameState.STATUS_LEFT,
                                KEY_FORFEIT to playerId
                            )
                        ).addOnCompleteListener { onDone() }
                    }
                    else -> onDone()
                }
            }
            .addOnFailureListener { onDone() }
    }

    fun resumeGame(onResult: (GameState?) -> Unit) {
        val gameId = currentGameId
        if (gameId.isNullOrEmpty()) {
            onResult(null)
            return
        }

        gamesRef.child(gameId).get()
            .addOnSuccessListener { snapshot ->
                val state = GameState.fromMap(gameId, snapshot.value)
                if (state == null || !state.isParticipant(playerId) || state.isOver()) {
                    currentGameId = null
                    onResult(null)
                } else {
                    onResult(state)
                }
            }
            .addOnFailureListener {
                currentGameId = null
                onResult(null)
            }
    }

    fun pruneStaleGames() {
        val now = System.currentTimeMillis()
        gamesRef.get().addOnSuccessListener { snapshot ->
            snapshot.children.forEach { child ->
                val state = GameState.fromMap(child.key.orEmpty(), child.value)
                    ?: return@forEach
                val age = now - state.createdAt
                if (state.createdAt <= 0L) return@forEach
                val staleWaiting = state.status == GameState.STATUS_WAITING && age > WAITING_TTL_MS
                val staleClosed = state.isOver() && age > CLOSED_TTL_MS
                if (staleWaiting || staleClosed) child.ref.removeValue()
            }
        }
    }

    private fun loadPlayerId(): String {
        prefs.getString(KEY_PLAYER_ID, null)?.let { return it }
        val generated = UUID.randomUUID().toString()
        prefs.edit().putString(KEY_PLAYER_ID, generated).apply()
        return generated
    }

    private fun initDatabase(context: Context): FirebaseDatabase {
        val app = try {
            FirebaseApp.getInstance()
        } catch (ignored: IllegalStateException) {
            FirebaseApp.initializeApp(context)
        } ?: throw IllegalStateException("FirebaseApp is not initialized")

        val url = app.options.databaseUrl
        return if (url.isNullOrBlank()) {
            FirebaseDatabase.getInstance(DEFAULT_DATABASE_URL)
        } else {
            FirebaseDatabase.getInstance(app)
        }
    }

    companion object {
        private const val PREFS_NAME = "online_prefs"
        private const val KEY_PLAYER_ID = "player_id"
        private const val KEY_PLAYER_NAME = "player_name"
        private const val KEY_CURRENT_GAME = "current_game_id"

        private const val GAMES_PATH = "triqui/games"
        private const val KEY_STATUS = "status"
        private const val KEY_CREATED_AT = "createdAt"
        private const val KEY_FORFEIT = "forfeitBy"

        private const val DEFAULT_PLAYER_NAME = "Player "
        private const val DEFAULT_DATABASE_URL =
            "https://tictactoeunal-7e723-default-rtdb.firebaseio.com"

        private const val WAITING_TTL_MS = 10 * 60 * 1000L
        private const val CLOSED_TTL_MS = 60 * 60 * 1000L

        private const val ERROR_CREATE = "Could not create the game"
        private const val ERROR_JOIN = "Could not join the game"
        private const val ERROR_MOVE = "Could not make the move"

        @Volatile
        private var instance: GameRepository? = null

        fun getInstance(context: Context): GameRepository =
            instance ?: synchronized(this) {
                instance ?: GameRepository(context).also { instance = it }
            }
    }
}
