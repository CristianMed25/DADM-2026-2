package co.edu.unal.tictactoe

class TicTacToeGame {

    companion object {
        const val HUMAN = 'X'
        const val COMPUTER = 'O'

        private val WIN_COMBOS = arrayOf(
            intArrayOf(0, 1, 2),
            intArrayOf(3, 4, 5),
            intArrayOf(6, 7, 8),
            intArrayOf(0, 3, 6),
            intArrayOf(1, 4, 7),
            intArrayOf(2, 5, 8),
            intArrayOf(0, 4, 8),
            intArrayOf(2, 4, 6)
        )
    }

    private val board = CharArray(9) { ' ' }

    fun clearBoard() {
        for (i in board.indices) {
            board[i] = ' '
        }
    }

    fun setMove(player: Char, location: Int): Boolean {
        if (location in 0..8 && board[location] == ' ') {
            board[location] = player
            return true
        }
        return false
    }

    fun getComputerMove(): Int {
        val winMove = findWinningMove(COMPUTER)
        if (winMove != -1) return winMove

        val blockMove = findWinningMove(HUMAN)
        if (blockMove != -1) return blockMove

        val available = mutableListOf<Int>()
        for (i in board.indices) {
            if (board[i] == ' ') available.add(i)
        }
        return if (available.isNotEmpty()) available.random() else -1
    }

    private fun findWinningMove(player: Char): Int {
        for (combo in WIN_COMBOS) {
            val a = combo[0]
            val b = combo[1]
            val c = combo[2]

            val playerCount = listOf(board[a], board[b], board[c]).count { it == player }
            val emptyCount = listOf(board[a], board[b], board[c]).count { it == ' ' }

            if (playerCount == 2 && emptyCount == 1) {
                if (board[a] == ' ') return a
                if (board[b] == ' ') return b
                if (board[c] == ' ') return c
            }
        }
        return -1
    }

    fun checkForWinner(): Int {
        for (combo in WIN_COMBOS) {
            val a = combo[0]
            val b = combo[1]
            val c = combo[2]

            if (board[a] != ' ' && board[a] == board[b] && board[b] == board[c]) {
                return if (board[a] == HUMAN) 2 else 3
            }
        }

        if (board.none { it == ' ' }) return 1

        return 0
    }

    fun getBoardChar(location: Int): Char = board[location]
}
