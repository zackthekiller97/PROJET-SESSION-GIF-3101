package ca.ulaval.ima.mp

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Button

import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue

class FriendsActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var friendsAdapter: FriendsAdapter
    private val friendsList = mutableListOf<User>()

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_friends)

        val emailInput = findViewById<EditText>(R.id.email_input)
        val addButton = findViewById<Button>(R.id.btn_add_friend)
        val recyclerView = findViewById<RecyclerView>(R.id.recycler_friends)

        friendsAdapter = FriendsAdapter(friendsList)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = friendsAdapter

        friendsAdapter.notifyDataSetChanged()

        addButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            if (email.isNotEmpty()) {
                addFriendByEmail(email)
            }
        }

        loadFriendsData()

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.selectedItemId = R.id.nav_friends
        bottomNav.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.nav_friends -> true
                R.id.nav_steps -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_calories -> {
                    startActivity(Intent(this, CaloriesActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_stats -> {
                    startActivity(Intent(this, StatsActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_logout -> {
                    auth.signOut()
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }
    }

    private fun addFriendByEmail(email: String) {
        val currentUserId = auth.currentUser?.uid ?: return

        db.collection("users").whereEqualTo("email", email).get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Toast.makeText(this, "Utilisateur non trouvé", Toast.LENGTH_SHORT).show()
                } else {
                    val friendId = documents.documents[0].id
                    db.collection("users").document(currentUserId)
                        .update("friends", FieldValue.arrayUnion(friendId))
                        .addOnSuccessListener {
                            Toast.makeText(this, "Ami ajouté !", Toast.LENGTH_SHORT).show()
                            loadFriendsData() // Recharger la liste
                        }
                }
            }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun loadFriendsData() {
        val currentUserId = auth.currentUser?.uid ?: return

        db.collection("users").document(currentUserId).get()
            .addOnSuccessListener { document ->

                val friendIds = document.get("friends") as? List<String> ?: listOf()

                if (friendIds.isEmpty()) {
                    friendsList.clear()
                    friendsAdapter.notifyDataSetChanged()
                    return@addOnSuccessListener
                }

                db.collection("users")
                    .whereIn(FieldPath.documentId(), friendIds)
                    .addSnapshotListener { snapshots, e ->
                        if (e != null) return@addSnapshotListener

                        friendsList.clear()

                        snapshots?.forEach { doc ->
                            val user = doc.toObject(User::class.java)
                            friendsList.add(user)
                        }

                        friendsAdapter.notifyDataSetChanged()
                    }
            }
    }
}