package ca.ulaval.ima.mp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class FriendsAdapter(private val friendsList: List<User>) :
    RecyclerView.Adapter<FriendsAdapter.FriendViewHolder>() {

    class FriendViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val rank: TextView = itemView.findViewById(R.id.friend_rank)
        val avatar: TextView = itemView.findViewById(R.id.friend_avatar)
        val name: TextView = itemView.findViewById(R.id.friend_name)
        val stats: TextView = itemView.findViewById(R.id.friend_stats)
        val goal: TextView = itemView.findViewById(R.id.friend_goal)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_friend, parent, false)
        return FriendViewHolder(view)
    }

    override fun onBindViewHolder(holder: FriendViewHolder, position: Int) {
        val friend = friendsList[position]

        // Rang (1, 2, 3, ...)
        holder.rank.text = (position + 1).toString()

        // Nom (si tu n’as que email → fallback)
        val displayName = friend.email.substringBefore("@")
        holder.name.text = displayName

        // Initiales
        val initials = displayName
            .split(".", "_", " ")
            .filter { it.isNotEmpty() }
            .map { it[0].uppercaseChar() }
            .take(2)
            .joinToString("")
        holder.avatar.text = initials

        // Stats (format comme dans l’image)
        holder.stats.text = "${friend.lastSteps} pas · ${friend.lastCalories} kcal"

        // Objectif (%) → exemple simple
        val goalSteps = 10000 // tu peux changer dynamiquement
        val percent = if (goalSteps > 0)
            (friend.lastSteps * 100 / goalSteps)
        else 0

        holder.goal.text = "$percent%"
    }

    override fun getItemCount(): Int = friendsList.size
}