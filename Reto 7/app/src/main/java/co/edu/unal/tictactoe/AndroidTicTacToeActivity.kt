package co.edu.unal.tictactoe

import android.content.SharedPreferences
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class AndroidTicTacToeActivity : AppCompatActivity(), View.OnTouchListener {

    private val game = TicTacToeGame()
    private lateinit var boardView: BoardView
    private lateinit var textStatus: TextView
    private lateinit var textScoreHuman: TextView
    private lateinit var textScoreTie: TextView
    private lateinit var textScoreAndroid: TextView

    private lateinit var prefs: SharedPreferences

    private val handler = Handler(Looper.getMainLooper())

    private var humanSound: MediaPlayer? = null
    private var computerSound: MediaPlayer? = null

    private var gameOver = false
    private var boardEnabled = true
    private var humanGoesFirst = true

    private var humanWins = 0
    private var computerWins = 0
    private var ties = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        textStatus = findViewById(R.id.textStatus)
        textScoreHuman = findViewById(R.id.textScoreHuman)
        textScoreTie = findViewById(R.id.textScoreTie)
        textScoreAndroid = findViewById(R.id.textScoreAndroid)

        boardView = findViewById(R.id.boardView)
        boardView.setGame(game)
        boardView.setOnTouchListener(this)

        restorePreferences()

        if (savedInstanceState == null) {
            startNewGame()
        } else {
            restoreInstanceState(savedInstanceState)
        }
    }

    override fun onResume() {
        super.onResume()
        humanSound = MediaPlayer.create(this, R.raw.move_user)
        computerSound = MediaPlayer.create(this, R.raw.move_android)
    }

    override fun onPause() {
        super.onPause()
        humanSound?.release()
        humanSound = null
        computerSound?.release()
        computerSound = null
    }

    override fun onStop() {
        super.onStop()
        savePreferences()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putCharArray(KEY_BOARD, game.getBoardState())
        outState.putBoolean(KEY_GAME_OVER, gameOver)
        outState.putBoolean(KEY_BOARD_ENABLED, boardEnabled)
        outState.putCharSequence(KEY_INFO, textStatus.text)
        outState.putInt(KEY_HUMAN_WINS, humanWins)
        outState.putInt(KEY_COMPUTER_WINS, computerWins)
        outState.putInt(KEY_TIES, ties)
        outState.putBoolean(KEY_GO_FIRST, humanGoesFirst)
    }

    private fun restoreInstanceState(savedInstanceState: Bundle) {
        game.setBoardState(savedInstanceState.getCharArray(KEY_BOARD))
        gameOver = savedInstanceState.getBoolean(KEY_GAME_OVER, false)
        boardEnabled = savedInstanceState.getBoolean(KEY_BOARD_ENABLED, true)
        humanGoesFirst = savedInstanceState.getBoolean(KEY_GO_FIRST, true)
        humanWins = savedInstanceState.getInt(KEY_HUMAN_WINS, humanWins)
        computerWins = savedInstanceState.getInt(KEY_COMPUTER_WINS, computerWins)
        ties = savedInstanceState.getInt(KEY_TIES, ties)
        textStatus.text = savedInstanceState.getCharSequence(KEY_INFO)
            ?: getString(R.string.you_go_first)

        updateScoreDisplay()
        boardView.invalidate()

        if (!gameOver && !boardEnabled) {
            handler.postDelayed({ computerMove() }, COMPUTER_MOVE_DELAY_MS)
        }
    }

    private fun restorePreferences() {
        humanWins = prefs.getInt(KEY_HUMAN_WINS, 0)
        computerWins = prefs.getInt(KEY_COMPUTER_WINS, 0)
        ties = prefs.getInt(KEY_TIES, 0)

        val savedDifficulty = prefs.getInt(KEY_DIFFICULTY, game.difficulty.ordinal)
        game.difficulty = DifficultyLevel.entries.getOrElse(savedDifficulty) { game.difficulty }
    }

    private fun savePreferences() {
        prefs.edit()
            .putInt(KEY_HUMAN_WINS, humanWins)
            .putInt(KEY_COMPUTER_WINS, computerWins)
            .putInt(KEY_TIES, ties)
            .putInt(KEY_DIFFICULTY, game.difficulty.ordinal)
            .apply()
    }

    private fun playSound(sound: MediaPlayer?) {
        sound?.seekTo(0)
        sound?.start()
    }

    private fun startNewGame() {
        handler.removeCallbacksAndMessages(null)
        game.clearBoard()
        boardView.invalidate()

        gameOver = false
        boardEnabled = true

        updateScoreDisplay()

        if (humanGoesFirst) {
            textStatus.text = getString(R.string.you_go_first)
        } else {
            textStatus.text = getString(R.string.android_go_first)
            boardEnabled = false
            handler.postDelayed({ computerMove() }, COMPUTER_MOVE_DELAY_MS)
        }

        humanGoesFirst = !humanGoesFirst
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
        if (gameOver || !boardEnabled) return

        val col = (event.x / boardView.getCellWidth()).toInt()
        val row = (event.y / boardView.getCellHeight()).toInt()

        if (row !in 0..2 || col !in 0..2) return

        val position = row * 3 + col
        if (game.getBoardChar(position) != ' ') return
        if (!game.setMove(TicTacToeGame.HUMAN, position)) return

        boardView.invalidate()
        playSound(humanSound)

        val result = game.checkForWinner()
        if (result != 0) {
            handleResult(result)
            return
        }

        textStatus.text = getString(R.string.android_turn)
        boardEnabled = false
        handler.postDelayed({ computerMove() }, COMPUTER_MOVE_DELAY_MS)
    }

    private fun computerMove() {
        if (gameOver) return

        val move = game.getComputerMove()
        if (move != -1 && game.setMove(TicTacToeGame.COMPUTER, move)) {
            boardView.invalidate()
            playSound(computerSound)
        }

        val result = game.checkForWinner()
        if (result != 0) {
            handleResult(result)
            return
        }

        textStatus.text = getString(R.string.your_turn)
        boardEnabled = true
    }

    private fun handleResult(result: Int) {
        gameOver = true
        boardEnabled = false
        when (result) {
            1 -> {
                ties++
                textStatus.text = getString(R.string.its_tie)
            }
            2 -> {
                humanWins++
                textStatus.text = getString(R.string.you_won)
            }
            3 -> {
                computerWins++
                textStatus.text = getString(R.string.android_won)
            }
        }
        updateScoreDisplay()
        savePreferences()
    }

    private fun updateScoreDisplay() {
        textScoreHuman.text = getString(R.string.score_human, humanWins)
        textScoreTie.text = getString(R.string.score_tie, ties)
        textScoreAndroid.text = getString(R.string.score_android, computerWins)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_new_game -> {
                startNewGame()
                true
            }
            R.id.action_difficulty -> {
                showDifficultyDialog()
                true
            }
            R.id.action_reset_scores -> {
                showResetScoresDialog()
                true
            }
            R.id.action_about -> {
                showAboutDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showDifficultyDialog() {
        val difficulties = arrayOf(
            getString(R.string.easy),
            getString(R.string.harder),
            getString(R.string.expert)
        )
        val difficultyLevels = DifficultyLevel.entries.toTypedArray()
        val currentOrdinal = game.difficulty.ordinal

        AlertDialog.Builder(this)
            .setTitle(R.string.difficulty_title)
            .setSingleChoiceItems(difficulties, currentOrdinal) { dialog, which ->
                game.difficulty = difficultyLevels[which]
                savePreferences()
                Toast.makeText(
                    this,
                    getString(R.string.difficulty_selected, difficulties[which]),
                    Toast.LENGTH_SHORT
                ).show()
                dialog.dismiss()
            }
            .setNegativeButton(R.string.no, null)
            .show()
    }

    private fun showResetScoresDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.reset_scores_title)
            .setMessage(R.string.reset_scores_message)
            .setPositiveButton(R.string.yes) { _, _ ->
                humanWins = 0
                computerWins = 0
                ties = 0
                updateScoreDisplay()
                savePreferences()
            }
            .setNegativeButton(R.string.no, null)
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

    private companion object {
        const val COMPUTER_MOVE_DELAY_MS = 1000L

        const val PREFS_NAME = "ttt_prefs"

        const val KEY_BOARD = "board"
        const val KEY_GAME_OVER = "mGameOver"
        const val KEY_BOARD_ENABLED = "boardEnabled"
        const val KEY_INFO = "info"
        const val KEY_HUMAN_WINS = "mHumanWins"
        const val KEY_COMPUTER_WINS = "mComputerWins"
        const val KEY_TIES = "mTies"
        const val KEY_GO_FIRST = "mGoFirst"
        const val KEY_DIFFICULTY = "mDifficulty"
    }
}
