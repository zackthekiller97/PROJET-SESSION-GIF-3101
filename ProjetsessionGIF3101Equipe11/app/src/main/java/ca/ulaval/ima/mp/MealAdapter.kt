package ca.ulaval.ima.mp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MealAdapter(private var meals: List<Meal>) : RecyclerView.Adapter<MealAdapter.MealViewHolder>() {

    class MealViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // Ces IDs doivent exister dans ton fichier item_meal.xml
        val name: TextView = view.findViewById(R.id.tv_meal_name)
        val calories: TextView = view.findViewById(R.id.tv_meal_calories)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MealViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_meal, parent, false)
        return MealViewHolder(view)
    }

    override fun onBindViewHolder(holder: MealViewHolder, position: Int) {
        val meal = meals[position]
        holder.name.text = meal.name
        holder.calories.text = "${meal.calories} kcal"
    }

    override fun getItemCount() = meals.size

    fun updateList(newMeals: List<Meal>) {
        this.meals = newMeals
        notifyDataSetChanged()
    }
}