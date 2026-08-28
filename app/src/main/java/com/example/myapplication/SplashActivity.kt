package com.example.myapplication

import android.content.Intent
import android.gesture.GestureOverlayView
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class SplashActivity : AppCompatActivity(), GestureDetector.OnGestureListener {

    private lateinit var videoView: VideoView
    private lateinit var gestureDetector: GestureDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        // ВАЖНО: Устанавливаем тему ДО super.onCreate()
        window.setBackgroundDrawableResource(android.R.color.black)
        setTheme(R.style.Theme_Splash)

        super.onCreate(savedInstanceState)

        // Устанавливаем цвет статус-бара
        window.statusBarColor = ContextCompat.getColor(this, R.color.colorStatusBar)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)

        supportActionBar?.hide()

        // Создаем GestureDetector для обработки свайпов
        gestureDetector = GestureDetector(this, this)

        // Создаем контейнер
        val frameLayout = FrameLayout(this)
        frameLayout.setBackgroundColor(android.graphics.Color.parseColor("#1E1E1E"))

        // VideoView
        videoView = VideoView(this)
        val videoParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
        videoView.layoutParams = videoParams
        frameLayout.addView(videoView)

        setContentView(frameLayout)

        // Загружаем видео
        val videoUri = Uri.parse("android.resource://${packageName}/${R.raw.splash_video}")
        videoView.setVideoURI(videoUri)

        videoView.setOnCompletionListener {
            goToMainActivity()
        }

        // Добавляем обработчик касаний на весь экран
        frameLayout.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }

        videoView.start()
    }

    // Переход к MainActivity
    private fun goToMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
        // Добавляем анимацию перехода (опционально)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    // Переход к MainActivity
    private fun goToMainActivity_() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    // ========== МЕТОРЫ GestureDetector ==========

    override fun onDown(e: MotionEvent): Boolean = true

    override fun onShowPress(e: MotionEvent) {}

    override fun onSingleTapUp(e: MotionEvent): Boolean {
        // По одиночному тапу тоже переходим
        goToMainActivity_()
        return true
    }

    override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
        // Если свайп достаточно большой — переходим
        if (e1 != null) {
            val deltaX = e2.x - e1.x
            val deltaY = e2.y - e1.y
            val minSwipeDistance = 100f // Минимальная дистанция свайпа в пикселях

            if (kotlin.math.abs(deltaX) > minSwipeDistance || kotlin.math.abs(deltaY) > minSwipeDistance) {
                goToMainActivity()
                return true
            }
        }
        return false
    }

    override fun onLongPress(e: MotionEvent) {}

    override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
        // Быстрый свайп (fling) тоже срабатывает
        goToMainActivity_()
        return true
    }

    override fun onBackPressed() {
        // По кнопке "назад" тоже выходим
        goToMainActivity()
    }
}