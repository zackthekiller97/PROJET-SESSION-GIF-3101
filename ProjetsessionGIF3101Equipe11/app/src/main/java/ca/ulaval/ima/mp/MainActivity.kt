package ca.ulaval.ima.mp

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class MainActivity : AppCompatActivity(), SensorEventListener {

    private var sensorManager: SensorManager? = null
    private var stepSensor: Sensor? = null
    private var initialSteps = -1f
    private var currentSteps = 0
    private var isDataLoaded = false
    private val db = FirebaseFirestore.getInstance()
    private val currentUser = FirebaseAuth.getInstance().currentUser
    private var userGoal = 10000


    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) setupStepCounter()
        else Toast.makeText(this, "Permission de mouvement refusée", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        loadTodaySteps()
        setupNavigation()
        setupGoalFeature()

        val btnLogout = findViewById<ImageView>(R.id.btn_logout)

        btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()

            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        stepSensor?.also {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager?.unregisterListener(this)
    }

    private fun setupNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.selectedItemId = R.id.nav_steps
        bottomNav.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.nav_steps -> true
                R.id.nav_calories -> {
                    startActivity(Intent(this, CaloriesActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.nav_friends -> {
                    startActivity(Intent(this, FriendsActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.nav_stats -> {
                    startActivity(Intent(this, StatsActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.nav_detail -> {
                    startActivity(Intent(this, DetailActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                else -> false
            }
        }
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION)
                != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
            } else {
                setupStepCounter()
            }
        } else {
            setupStepCounter()
        }
    }

    private fun setupStepCounter() {
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        if (stepSensor != null) {
            // Vrai appareil → capteur réel
            sensorManager?.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_UI)
        } else {
            // Émulateur → simulation manuelle
            startSimulation()
        }
    }

    private fun startSimulation() {
        var simulatedSteps = currentSteps
        val handler = android.os.Handler(mainLooper)
        val runnable = object : Runnable {
            override fun run() {
                simulatedSteps += 10
                updateUIAndFirebase(simulatedSteps)
                handler.postDelayed(this, 10000)
            }
        }
        handler.post(runnable)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            if (initialSteps < 0) {
                initialSteps = it.values[0]
            }

            val currentSteps = (it.values[0] - initialSteps).toInt()
            updateUIAndFirebase(currentSteps)
        }
    }

    private fun updateUIAndFirebase(steps: Int) {

        val tvSteps = findViewById<TextView>(R.id.tv_stepsTaken)
        val progressBar = findViewById<ProgressBar>(R.id.progress_bar)
        val tvKm = findViewById<TextView>(R.id.tv_kmWalked)
        val tvKcal = findViewById<TextView>(R.id.tv_kcalBurned)
        val tvPercent = findViewById<TextView>(R.id.tv_goalPercent)
        val horizontalBar = findViewById<ProgressBar>(R.id.goal_progress_horiz)

        tvSteps?.text = steps.toString()
        progressBar?.progress = steps


        val km = (steps * 0.762) / 1000
        val kcal = steps * 0.04
        val percent = (steps.toFloat() / userGoal * 100).toInt()
        progressBar?.max = userGoal

        tvKm?.text = String.format("%.2f", km)
        tvKcal?.text = kcal.toInt().toString()
        tvPercent?.text = "$percent% de l'objectif atteint"
        horizontalBar?.progress = percent

        saveStepsToFirebase(steps, kcal.toInt())
    }

    private fun saveStepsToFirebase(steps: Int, calories: Int) {
        currentUser?.let { user ->
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            val data = hashMapOf(
                "steps" to steps,
                "caloriesBurned" to calories,
                "sensorBase" to initialSteps,
                "date" to today
            )

            db.collection("users").document(user.uid)
                .collection("dailyStats").document(today)
                .set(data, SetOptions.merge())

            db.collection("users").document(user.uid)
                .set(
                    mapOf(
                        "lastSteps" to steps,
                        "lastCalories" to calories
                    ),
                    SetOptions.merge()
                )
        }
    }
    private fun setupGoalFeature() {
        val etGoal = findViewById<EditText>(R.id.et_goalSteps)
        val btnSetGoal = findViewById<MaterialButton>(R.id.btn_startTracking)

        // Charger l'objectif existant depuis Firebase
        loadGoalFromFirebase(etGoal)

        btnSetGoal.setOnClickListener {
            val goalText = etGoal.text.toString()

            if (goalText.isEmpty()) {
                Toast.makeText(this, "Entrez un objectif valide", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val goal = goalText.toInt()

            if (goal <= 0) {
                Toast.makeText(this, "Objectif invalide", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            userGoal = goal

            updateUIAndFirebase(currentSteps)

            saveGoalToFirebase(goal)
        }
    }

    private fun saveGoalToFirebase(goal: Int) {
        currentUser?.let { user ->
            db.collection("users")
                .document(user.uid)
                .set(mapOf("goalSteps" to goal), SetOptions.merge())
                .addOnSuccessListener {
                    Toast.makeText(this, "Objectif enregistré", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Erreur lors de l'enregistrement", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun loadTodaySteps() {
        currentUser?.let { user ->
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            db.collection("users").document(user.uid)
                .collection("dailyStats").document(today)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val steps = document.getLong("steps")?.toInt() ?: 0
                        val savedBase = document.getDouble("sensorBase")?.toFloat()

                        if (savedBase != null) {
                            initialSteps = savedBase
                        }
                        currentSteps = steps
                        isDataLoaded = true
                        updateUIAndFirebase(currentSteps)
                        checkPermissions()
                    }
                }
        }
    }

    private fun loadGoalFromFirebase(etGoal: EditText) {
        currentUser?.let { user ->
            db.collection("users")
                .document(user.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists() && document.contains("goalSteps")) {
                        val goal = document.getLong("goalSteps")?.toInt() ?: 10000
                        etGoal.setText(goal.toString())
                        userGoal = goal
                    }
                }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}