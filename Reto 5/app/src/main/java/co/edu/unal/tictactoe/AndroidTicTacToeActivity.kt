package co.edu.unal.tictactoe

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

        textStatus = findViewById(R.id.textStatus)
        textScoreHuman = findViewById(R.id.textScoreHuman)
        textScoreTie = findViewById(R.id.textScoreTie)
        textScoreAndroid = findViewById(R.id.textScoreAndroid)

        boardView = findViewById(R.id.boardView)
        boardView.setGame(game)
        boardView.setOnTouchListener(this)

        startNewGame()
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

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
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
            R.id.action_quit -> {
                showQuitDialog()
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

    private fun showQuitDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.quit_title)
            .setMessage(R.string.quit_message)
            .setPositiveButton(R.string.yes) { _, _ -> finish() }
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
    }
}
