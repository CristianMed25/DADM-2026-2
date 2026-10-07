package co.edu.unal.tictactoe

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GameStateTest {

    private fun playingGame(
        board: String = GameState.EMPTY_BOARD,
        turn: Char = GameState.SYMBOL_X
    ) = GameState(
        id = "game1",
        status = GameState.STATUS_PLAYING,
        hostId = "host",
        hostName = "Host",
        guestId = "guest",
        guestName = "Guest",
        board = board,
        turn = turn
    )

    @Test
    fun applyMovePlacesSymbolAndSwitchesTurn() {
        val result = GameState.applyMove(playingGame(), 4, GameState.SYMBOL_X)

        assertNotNull(result)
        assertEquals(GameState.SYMBOL_X, result!!.board[4])
        assertEquals(GameState.SYMBOL_O, result.turn)
        assertEquals(GameState.STATUS_PLAYING, result.status)
        assertEquals("", result.winner)
        assertEquals(1, result.moveCount())
    }

    @Test
    fun applyMoveRejectsTurnOutOfOrder() {
        assertNull(GameState.applyMove(playingGame(turn = GameState.SYMBOL_O), 0, GameState.SYMBOL_X))
    }

    @Test
    fun applyMoveRejectsOccupiedCell() {
        val board = "         ".toCharArray().also { it[4] = GameState.SYMBOL_O }.concatToString()
        assertNull(GameState.applyMove(playingGame(board = board), 4, GameState.SYMBOL_X))
    }

    @Test
    fun applyMoveRejectsWhenGameHasNotStarted() {
        val waiting = playingGame().copy(status = GameState.STATUS_WAITING)
        assertNull(GameState.applyMove(waiting, 0, GameState.SYMBOL_X))
    }

    @Test
    fun applyMoveRejectsMoveOutsideBoard() {
        assertNull(GameState.applyMove(playingGame(), -1, GameState.SYMBOL_X))
        assertNull(GameState.applyMove(playingGame(), 9, GameState.SYMBOL_X))
    }

    @Test
    fun applyMoveDetectsWin() {
        val board = "XX" + " ".repeat(7)
        val result = GameState.applyMove(playingGame(board = board), 2, GameState.SYMBOL_X)

        assertNotNull(result)
        assertEquals("X", result!!.winner)
        assertEquals(GameState.STATUS_FINISHED, result.status)
        assertEquals(3, result.moveCount())
    }

    @Test
    fun applyMoveDetectsTie() {
        val board = "XOXOXOOX "
        val result = GameState.applyMove(
            playingGame(board = board, turn = GameState.SYMBOL_O),
            8,
            GameState.SYMBOL_O
        )

        assertNotNull(result)
        assertEquals(GameState.SYMBOL_TIE, result!!.winner)
        assertEquals(GameState.STATUS_FINISHED, result.status)
    }

    @Test
    fun resolveWinnerFindsRowsColumnsAndDiagonals() {
        assertEquals("X", GameState.resolveWinner("XXX      "))
        assertEquals("O", GameState.resolveWinner("   OOO   "))
        assertEquals("X", GameState.resolveWinner("X   X   X"))
        assertEquals("O", GameState.resolveWinner("  O O O  "))
        assertEquals("X", GameState.resolveWinner("X  X  X  "))
        assertEquals(GameState.SYMBOL_TIE, GameState.resolveWinner("XOXOXOOXO"))
        assertEquals("", GameState.resolveWinner(GameState.EMPTY_BOARD))
    }

    @Test
    fun resolveWinnerIgnoresIncompleteCombos() {
        assertEquals("", GameState.resolveWinner("XX O     "))
        assertEquals("", GameState.resolveWinner("XOXOXOOX "))
    }

    @Test
    fun mapRoundTripKeepsAllFields() {
        val state = GameState(
            id = "abc",
            status = GameState.STATUS_PLAYING,
            hostId = "host",
            hostName = "Host",
            guestId = "guest",
            guestName = "Guest",
            board = "X OX     ",
            turn = GameState.SYMBOL_O,
            winner = "",
            createdAt = 1234L,
            forfeitBy = ""
        )

        val parsed = GameState.fromMap("abc", state.toMap())

        assertEquals(state, parsed)
    }

    @Test
    fun fromMapRejectsInvalidData() {
        assertNull(GameState.fromMap("abc", null))
        assertNull(GameState.fromMap("abc", "not a map"))
        assertNull(GameState.fromMap("abc", mapOf("status" to GameState.STATUS_WAITING)))
    }

    @Test
    fun normalizeBoardPadsShortBoard() {
        val board = GameState.normalizeBoard("X")

        assertEquals(GameState.BOARD_SIZE, board.length)
        assertEquals(GameState.SYMBOL_X, board[0])
        assertTrue(board.drop(1).all { it == GameState.EMPTY_CELL })
    }

    @Test
    fun symbolsAreAssignedByPlayerId() {
        val state = playingGame().copy(guestId = "", status = GameState.STATUS_WAITING)

        assertEquals(GameState.SYMBOL_X, state.symbolOf("host"))
        assertNull(state.symbolOf("guest"))
        assertTrue(state.isHost("host"))
        assertTrue(state.isJoinable())

        val joined = state.copy(guestId = "guest", status = GameState.STATUS_PLAYING)

        assertEquals(GameState.SYMBOL_O, joined.symbolOf("guest"))
        assertTrue(joined.isParticipant("guest"))
        assertFalse(joined.isJoinable())
        assertTrue(joined.isEmptyCell(0))
        assertFalse(joined.isEmptyCell(9))
    }
}
