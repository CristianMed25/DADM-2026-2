package co.edu.unal.tictactoe

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class AndroidTicTacToeActivity : AppCompatActivity() {

    private val game = TicTacToeGame()
    private lateinit var boardButtons: Array<Button>
    private lateinit var textStatus: TextView
    private lateinit var textScoreHuman: TextView
    private lateinit var textScoreTie: TextView
    private lateinit var textScoreAndroid: TextView
    private var gameOver = false
    private var humanGoesFirst = true

    private var humanWins = 0
    private var computerWins = 0
    private var ties = 0

    private val buttonIds = intArrayOf(
        R.id.button1, R.id.button2, R.id.button3,
        R.id.button4, R.id.button5, R.id.button6,
        R.id.button7, R.id.button8, R.id.button9
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textStatus = findViewById(R.id.textStatus)
        textScoreHuman = findViewById(R.id.textScoreHuman)
        textScoreTie = findViewById(R.id.textScoreTie)
        textScoreAndroid = findViewById(R.id.textScoreAndroid)

        boardButtons = Array(9) { i ->
            findViewById<Button>(buttonIds[i])
        }

        for (i in boardButtons.indices) {
            boardButtons[i].setOnClickListener { onButtonClick(i) }
        }

        startNewGame()
    }

    private fun startNewGame() {
        game.clearBoard()
        gameOver = false

        for (button in boardButtons) {
            button.text = ""
            button.setTextColor(ContextCompat.getColor(this, R.color.text_on_surface))
            button.isEnabled = true
            button.setBackgroundResource(R.drawable.bg_button)
        }

        updateScoreDisplay()

        if (humanGoesFirst) {
            textStatus.text = getString(R.string.you_go_first)
        } else {
            textStatus.text = getString(R.string.android_go_first)
            disableBoard()
            Handler(Looper.getMainLooper()).postDelayed({
                computerMove()
            }, 500)
        }

        humanGoesFirst = !humanGoesFirst
    }

    private fun onButtonClick(location: Int) {
        if (gameOver) return
        if (game.getBoardChar(location) != ' ') return

        game.setMove(TicTacToeGame.HUMAN, location)
        updateButton(location, TicTacToeGame.HUMAN)

        val result = game.checkForWinner()
        if (result != 0) {
            handleResult(result)
            return
        }

        textStatus.text = getString(R.string.android_turn)
        disableBoard()

        Handler(Looper.getMainLooper()).postDelayed({
            computerMove()
        }, 500)
    }

    private fun computerMove() {
        val move = game.getComputerMove()
        if (move != -1) {
            game.setMove(TicTacToeGame.COMPUTER, move)
            updateButton(move, TicTacToeGame.COMPUTER)
        }

        val result = game.checkForWinner()
        if (result != 0) {
            handleResult(result)
            return
        }

        textStatus.text = getString(R.string.your_turn)
        enableBoard()
    }

    private fun updateButton(location: Int, player: Char) {
        val button = boardButtons[location]
        button.text = player.toString()
        button.isEnabled = false
        button.setBackgroundResource(R.drawable.bg_button_disabled)

        if (player == TicTacToeGame.HUMAN) {
            button.setTextColor(ContextCompat.getColor(this, R.color.green_x))
        } else {
            button.setTextColor(ContextCompat.getColor(this, R.color.red_o))
        }
    }

    private fun handleResult(result: Int) {
        gameOver = true
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
        disableBoard()
    }

    private fun updateScoreDisplay() {
        textScoreHuman.text = getString(R.string.score_human, humanWins)
        textScoreTie.text = getString(R.string.score_tie, ties)
        textScoreAndroid.text = getString(R.string.score_android, computerWins)
    }

    private fun disableBoard() {
        for (button in boardButtons) {
            button.isEnabled = false
        }
    }

    private fun enableBoard() {
        for (i in boardButtons.indices) {
            if (game.getBoardChar(i) == ' ') {
                boardButtons[i].isEnabled = true
            }
        }
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
            else -> super.onOptionsItemSelected(item)
        }
    }
}
