package ca.ulaval.ima.mp

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

class CaloriesActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var mealAdapter: MealAdapter
    private val dailyGoal = 2000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calories)

        setupNavigation()
        setupRecyclerView()

        findViewById<MaterialButton>(R.id.btn_add_food).setOnClickListener {
            showAddFoodDialog()
        }

        listenToDailyCalories()
    }

    private fun setupRecyclerView() {
        val rv = findViewById<RecyclerView>(R.id.rv_meals)
        mealAdapter = MealAdapter(emptyList())
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = mealAdapter
    }

    private fun showAddFoodDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Ajouter un aliment")

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
        }

        val inputName = EditText(this).apply { hint = "Nom de l'aliment" }
        val inputCal = EditText(this).apply {
            hint = "Calories"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }

        layout.addView(inputName)
        layout.addView(inputCal)
        builder.setView(layout)

        builder.setPositiveButton("Ajouter") { _, _ ->
            val name = inputName.text.toString()
            val cal = inputCal.text.toString().toIntOrNull() ?: 0
            if (name.isNotEmpty()) saveMeal(name, cal)
        }
        builder.show()
    }

    private fun saveMeal(name: String, calories: Int) {
        val uid = auth.currentUser?.uid ?: return
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        val meal = hashMapOf(
            "name" to name,
            "calories" to calories,
            "timestamp" to System.currentTimeMillis()
        )

        db.collection("users").document(uid)
            .collection("dailyStats").document(today)
            .collection("meals").add(meal)
    }

    private fun listenToDailyCalories() {
        val uid = auth.currentUser?.uid ?: return
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        db.collection("users").document(uid)
            .collection("dailyStats").document(today)
            .collection("meals")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, _ ->
                val meals = snapshots?.toObjects(Meal::class.java) ?: emptyList()
                mealAdapter.updateList(meals)
                updateUI(meals.sumOf { it.calories })
            }
    }

    private fun updateUI(total: Int) {
        findViewById<TextView>(R.id.tv_calories_count).text = total.toString()
        val pb = findViewById<ProgressBar>(R.id.pb_calories_horiz)
        pb.max = dailyGoal
        pb.progress = total
    }

    private fun setupNavigation() {
        val nav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        nav.selectedItemId = R.id.nav_calories
        nav.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.nav_friends -> startActivity(Intent(this, FriendsActivity::class.java))
                R.id.nav_steps -> startActivity(Intent(this, MainActivity::class.java))
                R.id.nav_stats -> startActivity(Intent(this, StatsActivity::class.java))
                R.id.nav_logout -> {
                    auth.signOut()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                }
            }
            true
        }
    }
}