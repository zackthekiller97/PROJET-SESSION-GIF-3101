package ca.ulaval.ima.mp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class StatsActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private lateinit var lineChart: LineChart
    private lateinit var tvTotalSteps: TextView
    private lateinit var tvTotalCals: TextView
    private lateinit var tvAvgSteps: TextView
    private lateinit var tvAvgCals: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stats)


        setupNavigation()
        tvTotalSteps = findViewById(R.id.tv_total_steps)
        tvTotalCals = findViewById(R.id.tv_total_cals)
        tvAvgSteps = findViewById(R.id.tv_avg_steps)
        tvAvgCals = findViewById(R.id.tv_avg_cals)
        lineChart = findViewById(R.id.caloriesChart)


        val btn7Days = findViewById<View>(R.id.btn_7_days)
        val btn30Days = findViewById<View>(R.id.btn_30_days)

        btn7Days.setOnClickListener { fetchDataForRange(7) }
        btn30Days.setOnClickListener { fetchDataForRange(30) }


        fetchDataForRange(7)
    }

    private fun fetchDataForRange(days: Int) {
        val uid = auth.currentUser?.uid ?: return
        val entries = mutableListOf<Entry>()

        var sumSteps = 0
        var sumCalories = 0
        var daysWithSteps = 0
        var daysWithCals = 0

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()


        val daysToFetch = (0 until days).map {
            val date = sdf.format(calendar.time)
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            date
        }.reversed()

        var processedDays = 0
        daysToFetch.forEachIndexed { index, date ->
            db.collection("users").document(uid)
                .collection("dailyStats").document(date).get()
                .addOnSuccessListener { document ->
                    // Récupération des pas
                    val steps = document.getLong("steps")?.toInt() ?: 0
                    if (steps > 0) {
                        sumSteps += steps
                        daysWithSteps++
                    }


                    db.collection("users").document(uid)
                        .collection("dailyStats").document(date)
                        .collection("meals").get()
                        .addOnSuccessListener { mealSnapshot ->
                            val dayCalories = mealSnapshot.toObjects(Meal::class.java).sumOf { it.calories }
                            if (dayCalories > 0) {
                                sumCalories += dayCalories
                                daysWithCals++
                            }

                            entries.add(Entry(index.toFloat(), dayCalories.toFloat()))
                            processedDays++

                            if (processedDays == days) {
                                updateUI(entries, sumSteps, sumCalories, daysWithSteps, daysWithCals)
                            }
                        }
                }
        }
    }

    private fun updateUI(entries: List<Entry>, totalSteps: Int, totalCals: Int, daysSteps: Int, daysCals: Int) {
        tvTotalSteps.text = totalSteps.toString()
        tvTotalCals.text = totalCals.toString()


        val avgSteps = if (daysSteps > 0) totalSteps / daysSteps else 0
        val avgCals = if (daysCals > 0) totalCals / daysCals else 0

        tvAvgSteps.text = "Moy: $avgSteps/jour"
        tvAvgCals.text = "Moy: $avgCals/jour"

        updateChart(entries)
    }

    private fun updateChart(entries: List<Entry>) {
        if (entries.isEmpty()) return

        val dataSet = LineDataSet(entries, "Calories consommées").apply {
            color = android.graphics.Color.BLUE
            setDrawCircles(entries.size <= 7) // Cercles uniquement si peu de points
            valueTextSize = 10f
            setDrawFilled(true)
            fillColor = android.graphics.Color.LTGRAY
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }

        lineChart.data = LineData(dataSet)
        lineChart.description.isEnabled = false
        lineChart.animateY(1000)
        lineChart.invalidate()
    }

    private fun setupNavigation() {
        val nav = findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNavigation)
        nav.selectedItemId = R.id.nav_stats
        nav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_steps -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    true
                }
                R.id.nav_calories -> {
                    startActivity(Intent(this, CaloriesActivity::class.java))
                    true
                }
                R.id.nav_stats -> true
                R.id.nav_logout -> {
                    auth.signOut()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }
    }
}