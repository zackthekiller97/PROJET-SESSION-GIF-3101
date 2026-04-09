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
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity(), SensorEventListener {

    private var sensorManager: SensorManager? = null
    private var stepSensor: Sensor? = null
    private var initialSteps = -1f
    private val db = FirebaseFirestore.getInstance()
    private val currentUser = FirebaseAuth.getInstance().currentUser


    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) setupStepCounter()
        else Toast.makeText(this, "Permission de mouvement refusée", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkPermissions()
        setupNavigation()
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
                R.id.nav_logout -> {
                    FirebaseAuth.getInstance().signOut()
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
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
        var simulatedSteps = 0
        val handler = android.os.Handler(mainLooper)
        val runnable = object : Runnable {
            override fun run() {
                simulatedSteps += 50  // +50 pas toutes les secondes
                updateUIAndFirebase(simulatedSteps)
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(runnable)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            if (initialSteps < 0) initialSteps = it.values[0]
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
        val percent = (steps.toFloat() / 10000f * 100).toInt()

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
                "date" to today
            )
            db.collection("users").document(user.uid)
                .collection("dailyStats").document(today)
                .set(data)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}