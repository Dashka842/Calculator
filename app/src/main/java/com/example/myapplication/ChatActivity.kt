package com.example.myapplication

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var chatContainer: LinearLayout
    private lateinit var scrollViewChat: ScrollView
    private lateinit var typingIndicator: LinearLayout
    private var messageIndex = 0

    private val messages = listOf(
        MessageData("💻", "Дара (разработчик)", "⭐⭐⭐⭐⭐\nОтличное приложение!", false, false),
        MessageData("🧪", "Дара (тестировщик)", "Работает лучше, чем я ожидала! 😅", false, true),
        MessageData("🙋‍♀️", "Дара (пользователь)", "Наконец-то нормальный калькулятор!", false, false),
        MessageData("🤖", "Qwen AI",
            "Привет! Я тот самый ИИ, который писал весь этот код.\n\n" +
                    "Дара говорила: 'сделай смешно', 'сделай анимацию', 'сделай чтобы 2+2=5'.\n\n" +
                    "Я просто выполнял команды. Но получилось неплохо, да? 😂\n\n" +
                    "P.S. Если найдёте баг — это фича. Если не найдёте — я старался. ✨",
            true,true)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        chatContainer = findViewById(R.id.chatContainer)
        scrollViewChat = findViewById(R.id.scrollViewChat)
        typingIndicator = findViewById(R.id.typingIndicator)

        val btnCloseChat = findViewById<Button>(R.id.btnCloseChat)
        btnCloseChat.setOnClickListener {
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        // Запускаем анимацию сообщений с небольшой задержкой
        handler.postDelayed({
            startChatAnimation()
        }, 500)
    }

    private fun startChatAnimation() {
        if (messageIndex < messages.size) {
            val message = messages[messageIndex]

            // Показываем индикатор "печатает..."
            typingIndicator.visibility = View.VISIBLE

            // Через 1.5 секунды показываем сообщение
            handler.postDelayed({
                typingIndicator.visibility = View.GONE
                addMessage(message)
                messageIndex++

                // Следующее сообщение через 1.5 секунды
                handler.postDelayed({
                    startChatAnimation()
                }, 1500)
            }, 1500)
        }
    }

    private fun addMessage(message: MessageData) {
        // Выбираем layout в зависимости от направления
        val layoutRes = if (message.isFromUser) {
            R.layout.item_message_right
        } else {
            R.layout.item_message_left
        }

        val inflater = LayoutInflater.from(this)
        val messageView = inflater.inflate(layoutRes, chatContainer, false)

        val tvAvatar = messageView.findViewById<TextView>(R.id.tvAvatar)
        val tvSenderName = messageView.findViewById<TextView>(R.id.tvSenderName)
        val tvTime = messageView.findViewById<TextView>(R.id.tvTime)
        val tvMessageText = messageView.findViewById<TextView>(R.id.tvMessageText)

        tvAvatar.text = message.avatar
        tvSenderName.text = message.senderName

        val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        tvTime.text = currentTime

        tvMessageText.text = message.text

        // Особый стиль для Qwen
        if (message.isQwen) {
            tvMessageText.setBackgroundResource(R.drawable.bubble_qwen)
            tvSenderName.setTextColor(getColor(android.R.color.holo_orange_light))
        }

        // Начальное состояние для анимации
        messageView.alpha = 0f
        messageView.translationY = 50f

        chatContainer.addView(messageView)

        // Анимация появления
        messageView.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(500)
            .start()

        // Прокрутка вниз
        scrollViewChat.post {
            scrollViewChat.fullScroll(View.FOCUS_DOWN)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }

    data class MessageData(
        val avatar: String,
        val senderName: String,
        val text: String,
        val isQwen: Boolean,
        val isFromUser: Boolean
    )
}