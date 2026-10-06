package co.edu.unal.tictactoe

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class BoardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var game: TicTacToeGame? = null

    private val boardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.board_background)
        style = Paint.Style.FILL
    }

    private val cellStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.board_line)
        style = Paint.Style.STROKE
        strokeWidth = dp(2f)
    }

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.board_line)
        style = Paint.Style.STROKE
        strokeWidth = dp(3f)
        strokeCap = Paint.Cap.ROUND
    }

    private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    private val xBitmap: Bitmap =
        BitmapFactory.decodeResource(resources, R.drawable.x)

    private val oBitmap: Bitmap =
        BitmapFactory.decodeResource(resources, R.drawable.o)

    private val boardRect = RectF()
    private val cellRect = RectF()

    fun setGame(game: TicTacToeGame) {
        this.game = game
        invalidate()
    }

    fun getCellWidth(): Float = width / BOARD_ROWS.toFloat()

    fun getCellHeight(): Float = height / BOARD_ROWS.toFloat()

    override fun performClick(): Boolean {
        return super.performClick()
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val cellWidth = getCellWidth()
        val cellHeight = getCellHeight()

        val radius = dp(CELL_RADIUS_DP)
        val margin = dp(CELL_MARGIN_DP)
        boardRect.set(0f, 0f, width.toFloat(), height.toFloat())
        canvas.drawRoundRect(boardRect, radius, radius, boardPaint)

        for (position in 0 until BOARD_ROWS * BOARD_COLS) {
            val col = position % BOARD_COLS
            val row = position / BOARD_COLS
            cellRect.set(
                col * cellWidth + margin,
                row * cellHeight + margin,
                (col + 1) * cellWidth - margin,
                (row + 1) * cellHeight - margin
            )
            canvas.drawRoundRect(cellRect, radius, radius, cellStrokePaint)
        }

        for (i in 1 until BOARD_ROWS) {
            canvas.drawLine(cellWidth * i, 0f, cellWidth * i, height.toFloat(), gridPaint)
            canvas.drawLine(0f, cellHeight * i, width.toFloat(), cellHeight * i, gridPaint)
        }

        val currentGame = game ?: return

        for (position in 0 until BOARD_ROWS * BOARD_COLS) {
            val col = position % BOARD_COLS
            val row = position / BOARD_COLS

            when (currentGame.getBoardChar(position)) {
                TicTacToeGame.HUMAN ->
                    drawCell(canvas, xBitmap, col, row, cellWidth, cellHeight)
                TicTacToeGame.COMPUTER ->
                    drawCell(canvas, oBitmap, col, row, cellWidth, cellHeight)
            }
        }
    }

    private fun drawCell(
        canvas: Canvas,
        bitmap: Bitmap,
        col: Int,
        row: Int,
        cellWidth: Float,
        cellHeight: Float
    ) {
        val padding = minOf(cellWidth, cellHeight) * CELL_PADDING_RATIO
        cellRect.set(
            col * cellWidth + padding,
            row * cellHeight + padding,
            (col + 1) * cellWidth - padding,
            (row + 1) * cellHeight - padding
        )
        canvas.drawBitmap(bitmap, null, cellRect, bitmapPaint)
    }

    private companion object {
        const val BOARD_ROWS = 3
        const val BOARD_COLS = 3
        const val CELL_PADDING_RATIO = 0.12f
        const val CELL_MARGIN_DP = 4f
        const val CELL_RADIUS_DP = 12f
    }
}
