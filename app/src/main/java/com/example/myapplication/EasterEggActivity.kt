package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.view.animation.RotateAnimation
import android.view.animation.ScaleAnimation
import android.view.animation.TranslateAnimation
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class EasterEggActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_easter_egg)

        // Устанавливаем цвет статус-бара
        window.statusBarColor = ContextCompat.getColor(this, R.color.colorStatusBar)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)

        startAnimationSequence()
    }

    private fun startAnimationSequence() {
        val tvSecretMessage = findViewById<TextView>(R.id.tvSecretMessage)
        val scrollViewContent = findViewById<ScrollView>(R.id.scrollViewContent)
        val tvTitle = findViewById<TextView>(R.id.tvTitle)
        val tvSubtitle = findViewById<TextView>(R.id.tvSubtitle)
        val blockInfo = findViewById<LinearLayout>(R.id.blockInfo)
        val blockRequirements = findViewById<LinearLayout>(R.id.blockRequirements)
        val tvOpenChat = findViewById<TextView>(R.id.tvOpenChat)
        val tvFooter = findViewById<TextView>(R.id.tvFooter)
        val btnClose = findViewById<Button>(R.id.btnClose)

        // 1. Показываем "Секретная комбинация активирована!"
        tvSecretMessage.visibility = View.VISIBLE
        val fadeInSecret = AlphaAnimation(0f, 1f).apply {
            this.duration = 500
            fillAfter = true
        }
        tvSecretMessage.startAnimation(fadeInSecret)

        // 2. Через 2 секунды исчезает
        handler.postDelayed({
            val fadeOutSecret = AlphaAnimation(1f, 0f).apply {
                this.duration = 800
                fillAfter = true
            }
            tvSecretMessage.startAnimation(fadeOutSecret)
        }, 2000)

        // 3. Показываем основной контент
        handler.postDelayed({
            tvSecretMessage.visibility = View.GONE
            scrollViewContent.visibility = View.VISIBLE

            // 3.1. Заголовок "Made by" с вращением
            tvTitle.visibility = View.VISIBLE
            val rotate = RotateAnimation(0f, 360f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f).apply {
                this.duration = 800
                fillAfter = true
            }
            val scale = ScaleAnimation(0f, 1f, 0f, 1f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f).apply {
                this.duration = 800
                fillAfter = true
            }
            val animationSet = AnimationSet(false).apply {
                addAnimation(rotate)
                addAnimation(scale)
            }
            tvTitle.startAnimation(animationSet)
        }, 2800)

        // 3.2. Подзаголовок
        handler.postDelayed({
            tvSubtitle.visibility = View.VISIBLE
            fadeIn(tvSubtitle, 600)
        }, 3600)

        // 3.3. Блок информации
        handler.postDelayed({
            slideInFromLeft(blockInfo, 600)
        }, 4200)

        // 3.4. Системные требования
        handler.postDelayed({
            slideInFromRight(blockRequirements, 600)
        }, 5000)

        // 3.5. Надпись "Ваше мнение"
        handler.postDelayed({
            tvOpenChat.visibility = View.VISIBLE
            val rotate = RotateAnimation(-10f, 0f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f).apply {
                this.duration = 500
                fillAfter = true
            }
            val scale = ScaleAnimation(0.9f, 1f, 0.9f, 1f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f).apply {
                this.duration = 500
                fillAfter = true
            }
            val animationSet = AnimationSet(false).apply {
                addAnimation(rotate)
                addAnimation(scale)
            }
            tvOpenChat.startAnimation(animationSet)
            fadeIn(tvOpenChat, 500)

            // Обработчик клика
            tvOpenChat.setOnClickListener {
                startActivity(Intent(this@EasterEggActivity, ChatActivity::class.java))
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            }
        }, 5800)

        // 3.6. Подвал
        handler.postDelayed({
            tvFooter.visibility = View.VISIBLE
            fadeIn(tvFooter, 400)
        }, 6600)

        // 3.7. Кнопка
        handler.postDelayed({
            btnClose.visibility = View.VISIBLE
            val bounce = ScaleAnimation(0f, 1.2f, 0f, 1.2f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f).apply {
                this.duration = 300
                repeatCount = 1
                repeatMode = Animation.REVERSE
                fillAfter = true
            }
            btnClose.startAnimation(bounce)
            fadeIn(btnClose, 300)

            btnClose.setOnClickListener {
                finish()
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            }
        }, 7000)
    }

    private fun fadeIn(view: View, animDuration: Long) {
        val animation = AlphaAnimation(0f, 1f).apply {
            this.duration = animDuration
            fillAfter = true
        }
        view.startAnimation(animation)
    }

    private fun slideInFromLeft(view: View, animDuration: Long) {
        view.visibility = View.VISIBLE
        val animation = TranslateAnimation(-200f, 0f, 0f, 0f).apply {
            this.duration = animDuration
            fillAfter = true
        }
        fadeIn(view, animDuration)
        view.startAnimation(animation)
    }

    private fun slideInFromRight(view: View, animDuration: Long) {
        view.visibility = View.VISIBLE
        val animation = TranslateAnimation(200f, 0f, 0f, 0f).apply {
            this.duration = animDuration
            fillAfter = true
        }
        fadeIn(view, animDuration)
        view.startAnimation(animation)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}