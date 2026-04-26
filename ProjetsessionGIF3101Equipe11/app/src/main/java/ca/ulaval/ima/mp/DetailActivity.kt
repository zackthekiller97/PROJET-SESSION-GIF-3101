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

class DetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        setupNavigation()

        val btnLogout = findViewById<ImageView>(R.id.btn_logout)

        btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()

            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }

    private fun setupNavigation() {
        val nav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        nav.selectedItemId = R.id.nav_detail
        nav.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.nav_friends -> startActivity(Intent(this, FriendsActivity::class.java))
                R.id.nav_steps -> startActivity(Intent(this, MainActivity::class.java))
                R.id.nav_stats -> startActivity(Intent(this, StatsActivity::class.java))
                R.id.nav_calories -> startActivity(Intent(this, CaloriesActivity::class.java))
            }
            true
        }
    }
}