package co.edu.unal.tictactoe

import android.media.MediaPlayer
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.ValueEventListener

class OnlineGameActivity : AppCompatActivity(), View.OnTouchListener {

    companion object {
        const val EXTRA_GAME_ID = "game_id"
    }

    private val game = TicTacToeGame()

    private lateinit var repository: GameRepository
    private lateinit var boardView: BoardView
    private lateinit var textStatus: TextView
    private lateinit var textPlayerX: TextView
    private lateinit var textPlayerO: TextView
    private lateinit var textMoves: TextView

    private var gameId: String = ""
    private var state: GameState? = null
    private var mySymbol: Char = GameState.SYMBOL_X
    private var boardEnabled = false
    private var gameListener: ValueEventListener? = null

    private var myMoveSound: MediaPlayer? = null
    private var opponentMoveSound: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_online_game)

        gameId = intent.getStringExtra(EXTRA_GAME_ID).orEmpty()
        if (gameId.isEmpty()) {
            finish()
            return
        }

        repository = GameRepository.getInstance(this)

        boardView = findViewById(R.id.boardView)
        textStatus = findViewById(R.id.textStatus)
        textPlayerX = findViewById(R.id.textPlayerX)
        textPlayerO = findViewById(R.id.textPlayerO)
        textMoves = findViewById(R.id.textMoves)

        boardView.setGame(game)
        boardView.setOnTouchListener(this)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                confirmLeave()
            }
        })
    }

    override fun onStart() {
        super.onStart()
        if (gameId.isEmpty()) return
        gameListener = repository.observeGame(gameId) { newState -> render(newState) }
    }

    override fun onStop() {
        super.onStop()
        gameListener?.let { repository.stopObservingGame(gameId, it) }
        gameListener = null
    }

    override fun onResume() {
        super.onResume()
        myMoveSound = MediaPlayer.create(this, R.raw.move_user)
        opponentMoveSound = MediaPlayer.create(this, R.raw.move_android)
    }

    override fun onPause() {
        super.onPause()
        myMoveSound?.release()
        myMoveSound = null
        opponentMoveSound?.release()
        opponentMoveSound = null
    }

    private fun render(newState: GameState?) {
        if (newState == null) {
            Toast.makeText(this, R.string.game_missing, Toast.LENGTH_SHORT).show()
            repository.currentGameId = null
            finish()
            return
        }

        val previous = state
        state = newState
        mySymbol = newState.symbolOf(repository.playerId) ?: GameState.SYMBOL_X

        game.setBoardState(newState.board.toCharArray())
        boardView.invalidate()

        val hostLabel = newState.hostName.ifEmpty { getString(R.string.opponent_placeholder) }
        val guestLabel = newState.guestName.ifEmpty { getString(R.string.opponent_placeholder) }
        textPlayerX.text = getString(
            R.string.player_with_symbol,
            hostLabel,
            GameState.SYMBOL_X.toString()
        )
        textPlayerO.text = getString(
            R.string.player_with_symbol,
            guestLabel,
            GameState.SYMBOL_O.toString()
        )
        textMoves.text = getString(R.string.moves_count, newState.moveCount())

        val currentTurn: Char? = when {
            newState.isOver() -> null
            newState.status == GameState.STATUS_PLAYING -> newState.turn
            else -> null
        }
        textPlayerX.alpha = if (currentTurn == GameState.SYMBOL_X) 1f else 0.55f
        textPlayerO.alpha = if (currentTurn == GameState.SYMBOL_O) 1f else 0.55f

        boardEnabled = !newState.isOver() &&
            newState.status == GameState.STATUS_PLAYING &&
            newState.turn == mySymbol

        textStatus.text = statusText(newState)

        if (previous != null && previous.board != newState.board) {
            playSound(if (lastMoveWasMine(newState)) myMoveSound else opponentMoveSound)
        }
    }

    private fun lastMoveWasMine(newState: GameState): Boolean {
        if (newState.isOver()) return newState.winner == mySymbol.toString()
        return newState.turn != mySymbol
    }

    private fun statusText(current: GameState): CharSequence {
        val opponentName = current.nameOf(GameState.otherSymbol(mySymbol))
            .ifEmpty { getString(R.string.opponent_placeholder) }

        return when {
            current.status == GameState.STATUS_WAITING -> getString(R.string.status_waiting)
            current.status == GameState.STATUS_LEFT && current.forfeitBy != repository.playerId ->
                getString(R.string.status_opponent_left)
            current.status == GameState.STATUS_FINISHED -> when (current.winner) {
                GameState.SYMBOL_TIE -> getString(R.string.status_tie)
                mySymbol.toString() -> getString(R.string.status_you_won)
                else -> getString(R.string.status_opponent_won, opponentName)
            }
            current.turn == mySymbol -> getString(R.string.status_your_turn, mySymbol.toString())
            else -> getString(R.string.status_opponent_turn, opponentName)
        }
    }

    private fun playSound(sound: MediaPlayer?) {
        sound?.seekTo(0)
        sound?.start()
    }

    override fun onTouch(view: View, event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_UP -> {
                view.performClick()
                return true
            }
            MotionEvent.ACTION_DOWN -> handleBoardTouch(event)
        }
        return true
    }

    private fun handleBoardTouch(event: MotionEvent) {
        if (!boardEnabled) return
        val current = state ?: return

        val col = (event.x / boardView.getCellWidth()).toInt()
        val row = (event.y / boardView.getCellHeight()).toInt()
        if (row !in 0..2 || col !in 0..2) return

        val position = row * 3 + col
        if (!current.isEmptyCell(position)) return

        boardEnabled = false
        repository.makeMove(gameId, position) { message ->
            boardEnabled = state?.let {
                !it.isOver() &&
                    it.status == GameState.STATUS_PLAYING &&
                    it.turn == mySymbol
            } == true
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun confirmLeave() {
        val current = state
        val inProgress = current != null &&
            (current.status == GameState.STATUS_WAITING ||
                current.status == GameState.STATUS_PLAYING)

        if (!inProgress) {
            leaveGame()
            return
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.leave_title)
            .setMessage(R.string.leave_message)
            .setPositiveButton(R.string.leave_game) { _, _ -> leaveGame() }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun leaveGame() {
        repository.leaveGame { finish() }
    }

    private fun showAboutDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_about, null)
        AlertDialog.Builder(this)
            .setTitle(R.string.about)
            .setView(dialogView)
            .setPositiveButton(R.string.ok) { dialog, _ -> dialog.dismiss() }
            .show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_online, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_leave -> {
                confirmLeave()
                true
            }
            R.id.action_about -> {
                showAboutDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
