package ca.ulaval.ima.mp

data class User(
    val id: String = "",
    val email: String = "",
    val lastSteps: Int = 0,
    val lastCalories: Int = 0
)