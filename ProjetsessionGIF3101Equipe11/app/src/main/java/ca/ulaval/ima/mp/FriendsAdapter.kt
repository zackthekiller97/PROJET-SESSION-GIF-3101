package ca.ulaval.ima.mp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class FriendsAdapter(private val friendsList: List<User>) :
    RecyclerView.Adapter<FriendsAdapter.FriendViewHolder>() {

    class FriendViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val emailText: TextView = itemView.findViewById(R.id.friend_email)
        val stepsText: TextView = itemView.findViewById(R.id.friend_steps)
        val caloriesText: TextView = itemView.findViewById(R.id.friend_calories)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_friend, parent, false)
        return FriendViewHolder(view)
    }

    override fun onBindViewHolder(holder: FriendViewHolder, position: Int) {
        val friend = friendsList[position]
        holder.emailText.text = friend.email
        holder.stepsText.text = "Pas : ${friend.lastSteps}"
        holder.caloriesText.text = "Cal : ${friend.lastCalories} kcal"
    }

    override fun getItemCount(): Int = friendsList.size
}