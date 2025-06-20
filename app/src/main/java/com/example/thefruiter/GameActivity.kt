package com.example.thefruiter

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*

enum class FruitType {
    NORMAL,
    BOMB
}

data class FruitObject(
    val imageView: ImageView,
    val type: FruitType
)

class GameActivity : AppCompatActivity() {

    private val activeFruits = mutableListOf<FruitObject>()
    private var score = 0
    private var gameOver = false
    private var fallSpeed = 10f
    private var objectsPerCycle = 1
    private var inactivityJob: Job? = null
    private val inactivityLimit = 5000L
    private lateinit var tTimer: TextView


    private lateinit var tScore: TextView
    private lateinit var fruitZone: FrameLayout

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_game)

        tScore = findViewById(R.id.tScore)
        fruitZone = findViewById(R.id.fruitZone)
        tTimer = findViewById(R.id.tTimer)

        resetInactivityTimer()


        fruitZone.post {
            lifecycleScope.launch {
                while (isActive && !gameOver) {

                    repeat(objectsPerCycle) {
                        val type = if ((1..10).random() <= 7) FruitType.NORMAL else FruitType.BOMB
                        spawnFruit(fruitZone, type)
                    }

                    delay(900L)

                    if (fallSpeed < 200) fallSpeed += 1f
                    if (objectsPerCycle < 2) objectsPerCycle++

                }
            }
        }
    }

    private fun spawnFruit(container: FrameLayout, type: FruitType) {
        if (gameOver) return

        val fruit = ImageView(this)
        val drawable = when (type) {
            FruitType.NORMAL -> R.drawable.apple
            FruitType.BOMB -> R.drawable.bomb
        }
        fruit.setImageResource(drawable)

        val size = 300
        val screenWidth = container.width
        val screenHeight = resources.displayMetrics.heightPixels


        val fruitObj = FruitObject(fruit, type)
        activeFruits.add(fruitObj)

        val layoutParams = FrameLayout.LayoutParams(size, size)
        fruit.layoutParams = layoutParams


        val randomX = (0 until screenWidth - size).random()
        fruit.x = randomX.toFloat()
        fruit.y = -size.toFloat()

        container.addView(fruit)

        if (type == FruitType.NORMAL) {
            fruit.setOnClickListener {
                if (!gameOver) {
                    score += 10
                    tScore.text = "Score: $score"
                    (fruit.parent as? ViewGroup)?.removeView(fruit)
                    activeFruits.remove(fruitObj)
                    resetInactivityTimer()
                }
            }
        }

        if (type == FruitType.BOMB) {
            fruit.setOnClickListener {
                if (!gameOver) {
                    gameOver = true
                    tScore.text = "Game Over"
                    val intent = Intent(this, GameOver::class.java)
                    intent.putExtra("score", score)
                    startActivity(intent)
                    finish()
                }
            }
        }

        lifecycleScope.launch {
            while (fruit.y < screenHeight && !gameOver) {
                delay(10L)
                fruit.y += fallSpeed
            }
            if (!gameOver) {
                (fruit.parent as? ViewGroup)?.removeView(fruit)
                activeFruits.remove(fruitObj)
            }
        }
    }
    private fun resetInactivityTimer() {
        inactivityJob?.cancel()
        inactivityJob = lifecycleScope.launch {
            var timeLeft = (inactivityLimit / 1000).toInt()

            while (timeLeft > 0 && !gameOver) {
                tTimer.text = "Time left: $timeLeft"
                delay(1000L)
                timeLeft--
            }

            if (!gameOver) {
                gameOver = true
                tScore.text = "Game Over"
                val intent = Intent(this@GameActivity, GameOver::class.java)
                intent.putExtra("score", score)
                startActivity(intent)
                finish()
            }
        }
    }


}
