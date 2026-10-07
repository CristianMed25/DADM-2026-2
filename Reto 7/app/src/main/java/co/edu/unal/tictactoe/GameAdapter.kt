package co.edu.unal.tictactoe

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import android.text.format.DateFormat
import java.util.Date

class GameAdapter(
    private val onGameClicked: (GameState) -> Unit
) : ListAdapter<GameState, GameAdapter.GameViewHolder>(DIFF) {

    fun submitGames(games: List<GameState>) {
        submitList(games)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GameViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_game, parent, false)
        return GameViewHolder(view)
    }

    override fun onBindViewHolder(holder: GameViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class GameViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val textTitle: TextView = itemView.findViewById(R.id.textGameTitle)
        private val textInfo: TextView = itemView.findViewById(R.id.textGameInfo)
        private val textJoin: TextView = itemView.findViewById(R.id.textJoin)

        fun bind(game: GameState) {
            val context = itemView.context
            textTitle.text = context.getString(R.string.game_of, game.hostName)

            val createdAt = if (game.createdAt > 0L) {
                DateFormat.getTimeFormat(context).format(Date(game.createdAt))
            } else {
                context.getString(R.string.just_now)
            }
            textInfo.text = context.getString(R.string.game_info, createdAt)
            textJoin.text = context.getString(R.string.join)

            itemView.setOnClickListener { onGameClicked(game) }
            textJoin.setOnClickListener { onGameClicked(game) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<GameState>() {
            override fun areItemsTheSame(oldItem: GameState, newItem: GameState): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: GameState, newItem: GameState): Boolean =
                oldItem == newItem
        }
    }
}
