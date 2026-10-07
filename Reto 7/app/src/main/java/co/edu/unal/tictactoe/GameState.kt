package co.edu.unal.tictactoe

data class GameState(
    val id: String = "",
    val status: String = STATUS_WAITING,
    val hostId: String = "",
    val hostName: String = "",
    val guestId: String = "",
    val guestName: String = "",
    val board: String = EMPTY_BOARD,
    val turn: Char = SYMBOL_X,
    val winner: String = "",
    val createdAt: Long = 0L,
    val forfeitBy: String = ""
) {

    fun symbolOf(playerId: String): Char? = when (playerId) {
        hostId -> SYMBOL_X
        guestId -> SYMBOL_O
        else -> null
    }

    fun isParticipant(playerId: String): Boolean = symbolOf(playerId) != null

    fun isHost(playerId: String): Boolean = hostId == playerId

    fun nameOf(symbol: Char): String = when (symbol) {
        SYMBOL_X -> hostName
        SYMBOL_O -> guestName
        else -> ""
    }

    fun isEmptyCell(position: Int): Boolean =
        position in 0 until BOARD_SIZE && board.getOrNull(position) == EMPTY_CELL

    fun moveCount(): Int = board.count { it != EMPTY_CELL }

    fun isJoinable(): Boolean = status == STATUS_WAITING && guestId.isEmpty()

    fun isOver(): Boolean = status == STATUS_FINISHED || status == STATUS_LEFT

    fun toMap(): Map<String, Any> = mapOf(
        "status" to status,
        "hostId" to hostId,
        "hostName" to hostName,
        "guestId" to guestId,
        "guestName" to guestName,
        "board" to board,
        "turn" to turn.toString(),
        "winner" to winner,
        "createdAt" to createdAt,
        "forfeitBy" to forfeitBy
    )

    companion object {

        const val STATUS_WAITING = "waiting"
        const val STATUS_PLAYING = "playing"
        const val STATUS_FINISHED = "finished"
        const val STATUS_LEFT = "left"

        const val SYMBOL_X = 'X'
        const val SYMBOL_O = 'O'
        const val SYMBOL_TIE = "tie"

        const val EMPTY_CELL = ' '
        const val EMPTY_BOARD = "         "
        const val BOARD_SIZE = 9

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

        fun fromMap(id: String, value: Any?): GameState? {
            val map = value as? Map<*, *> ?: return null
            val hostId = (map["hostId"] as? String).orEmpty()
            if (hostId.isEmpty()) return null

            return GameState(
                id = id,
                status = map["status"] as? String ?: STATUS_WAITING,
                hostId = hostId,
                hostName = map["hostName"] as? String ?: "",
                guestId = map["guestId"] as? String ?: "",
                guestName = map["guestName"] as? String ?: "",
                board = normalizeBoard(map["board"] as? String),
                turn = (map["turn"] as? String)?.firstOrNull() ?: SYMBOL_X,
                winner = map["winner"] as? String ?: "",
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: 0L,
                forfeitBy = map["forfeitBy"] as? String ?: ""
            )
        }

        fun normalizeBoard(raw: String?): String {
            val source = raw.orEmpty()
            return (source + EMPTY_BOARD).substring(0, BOARD_SIZE)
        }

        fun otherSymbol(symbol: Char): Char =
            if (symbol == SYMBOL_X) SYMBOL_O else SYMBOL_X

        fun applyMove(state: GameState, position: Int, symbol: Char): GameState? {
            if (state.status != STATUS_PLAYING) return null
            if (state.turn != symbol) return null
            if (!state.isEmptyCell(position)) return null

            val newBoard = state.board.substring(0, position) + symbol +
                state.board.substring(position + 1)
            val result = resolveWinner(newBoard)

            return state.copy(
                board = newBoard,
                status = if (result.isEmpty()) STATUS_PLAYING else STATUS_FINISHED,
                turn = if (result.isEmpty()) otherSymbol(symbol) else state.turn,
                winner = result
            )
        }

        fun resolveWinner(board: String): String {
            for (combo in WIN_COMBOS) {
                val a = board.getOrNull(combo[0]) ?: continue
                val b = board.getOrNull(combo[1]) ?: continue
                val c = board.getOrNull(combo[2]) ?: continue
                if (a != EMPTY_CELL && a == b && b == c) return a.toString()
            }
            if (board.length >= BOARD_SIZE && board.none { it == EMPTY_CELL }) return SYMBOL_TIE
            return ""
        }
    }
}
