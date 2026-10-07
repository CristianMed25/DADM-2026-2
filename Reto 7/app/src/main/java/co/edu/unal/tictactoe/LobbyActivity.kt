package co.edu.unal.tictactoe

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.ValueEventListener

class LobbyActivity : AppCompatActivity() {

    private lateinit var repository: GameRepository
    private lateinit var adapter: GameAdapter
    private lateinit var recyclerGames: RecyclerView
    private lateinit var textEmpty: TextView
    private lateinit var textPlayerName: TextView
    private lateinit var progressLoading: ProgressBar
    private lateinit var btnCreateGame: View

    private var gamesListener: ValueEventListener? = null
    private var observing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lobby)

        repository = GameRepository.getInstance(this)

        recyclerGames = findViewById(R.id.recyclerGames)
        textEmpty = findViewById(R.id.textEmpty)
        textPlayerName = findViewById(R.id.textPlayerName)
        progressLoading = findViewById(R.id.progressLoading)
        btnCreateGame = findViewById(R.id.btnCreateGame)

        adapter = GameAdapter { game -> confirmJoin(game) }
        recyclerGames.layoutManager = LinearLayoutManager(this)
        recyclerGames.adapter = adapter

        btnCreateGame.setOnClickListener { createGame() }
        findViewById<View>(R.id.btnVsAndroid).setOnClickListener {
            startActivity(Intent(this, AndroidTicTacToeActivity::class.java))
        }

        updatePlayerLabel()
        repository.pruneStaleGames()

        if (savedInstanceState == null && repository.playerName.isEmpty()) {
            showNameDialog()
        }
    }

    override fun onStart() {
        super.onStart()
        startObserving()
    }

    override fun onStop() {
        super.onStop()
        stopObserving()
    }

    private fun startObserving() {
        if (observing) return
        observing = true

        progressLoading.visibility = View.VISIBLE
        textEmpty.visibility = View.GONE
        btnCreateGame.isEnabled = false

        gamesListener = repository.observeGames { games ->
            progressLoading.visibility = View.GONE
            btnCreateGame.isEnabled = true
            adapter.submitGames(games)
            textEmpty.visibility = if (games.isEmpty()) View.VISIBLE else View.GONE
        }

        repository.resumeGame { state ->
            if (state != null) openGame(state.id)
        }
    }

    private fun stopObserving() {
        gamesListener?.let { repository.stopObservingGames(it) }
        gamesListener = null
        observing = false
    }

    private fun updatePlayerLabel() {
        textPlayerName.text = getString(R.string.playing_as, repository.displayName())
    }

    private fun createGame() {
        btnCreateGame.isEnabled = false
        repository.createGame(
            onSuccess = { gameId -> openGame(gameId) },
            onFailure = { message ->
                btnCreateGame.isEnabled = true
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun confirmJoin(game: GameState) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.game_of, game.hostName))
            .setMessage(R.string.join_message)
            .setPositiveButton(R.string.join) { _, _ -> joinGame(game.id) }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun joinGame(gameId: String) {
        repository.joinGame(
            gameId,
            onSuccess = { openGame(gameId) },
            onFailure = { message -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show() }
        )
    }

    private fun openGame(gameId: String) {
        startActivity(
            Intent(this, OnlineGameActivity::class.java).putExtra(
                OnlineGameActivity.EXTRA_GAME_ID,
                gameId
            )
        )
    }

    private fun showNameDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_player_name, null)
        val editName = dialogView.findViewById<EditText>(R.id.editName)
        editName.setText(repository.displayName())
        editName.setSelection(editName.text.length)

        AlertDialog.Builder(this)
            .setTitle(R.string.name_title)
            .setMessage(R.string.name_message)
            .setView(dialogView)
            .setPositiveButton(R.string.save) { _, _ ->
                val name = editName.text.toString().trim()
                repository.playerName =
                    if (name.isEmpty()) repository.displayName() else name
                updatePlayerLabel()
            }
            .setNegativeButton(R.string.cancel) { _, _ -> updatePlayerLabel() }
            .setOnDismissListener { updatePlayerLabel() }
            .show()
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
        menuInflater.inflate(R.menu.menu_lobby, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_change_name -> {
                showNameDialog()
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
