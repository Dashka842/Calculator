package com.example.myapplication

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import android.widget.LinearLayout
import android.widget.Toast
import android.content.Context
import android.content.ClipboardManager
import android.content.ClipData
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var tvExpression: EditText
    private lateinit var tvDisplay: TextView
    private lateinit var tvHistoryArrow: TextView
    private lateinit var btnBackSmall: TextView
    private lateinit var btnBracket: Button
    private lateinit var btnDegree: TextView

    private val state = ExpressionState()
    private val engine = CalculatorEngine()

    private var repeatButton: Button? = null
    private var easterEggStep = 0

    // Все тригонометрические функции (для валидации)
    private val allTrigFunctions = listOf(
        "arcsin", "arccos", "arctan", "arccot",
        "sinh", "cosh", "tanh", "coth",
        "sin", "cos", "tan", "cot",
        "log", "ln", "√", "abs", "exp", "fact"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Устанавливаем цвет статус-бара
        window.statusBarColor = ContextCompat.getColor(this, R.color.colorStatusBar)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)

        tvExpression = findViewById(R.id.tvExpression)
        tvDisplay = findViewById(R.id.tvDisplay)
        tvHistoryArrow = findViewById(R.id.tvHistoryArrow)
        btnBackSmall = findViewById(R.id.btnBackSmall)
        btnBracket = findViewById(R.id.btnBracket)
        btnDegree = findViewById(R.id.btnDegree)
        btnDegree.visibility = View.GONE

        // Блокировка клавиатуры
        tvExpression.showSoftInputOnFocus = false
        tvExpression.isFocusable = true
        tvExpression.isFocusableInTouchMode = true
        tvExpression.isCursorVisible = true
        tvExpression.setTextIsSelectable(true)
        window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)

        // Разрешаем прокрутку пальцем
        tvExpression.movementMethod = android.text.method.ScrollingMovementMethod()

        // Отслеживаем перемещение курсора
        tvExpression.setOnTouchListener { _, _ ->
            tvExpression.postDelayed({ updateDegreeButtonVisibility() }, 50)
            false
        }

        // Кнопка удаления
        btnBackSmall.setOnClickListener { onBackClick() }

        // Кнопка скобок
        btnBracket.setOnClickListener { onBracketClick(forceOpen = false) }
        btnBracket.setOnLongClickListener {
            onBracketClick(forceOpen = true)
            true
        }

        // Цифры
        val digitIds = listOf(
            R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4,
            R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9, R.id.btnDot
        )
        for (id in digitIds) {
            val button = findViewById<Button>(id)
            val digit = button.text.toString()

            val repeatRunnable = object : Runnable {
                override fun run() {
                    onDigitClick(digit)
                    button.postDelayed(this, 80)
                }
            }

            button.setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        onDigitClick(digit)
                        repeatButton = button
                        button.postDelayed(repeatRunnable, 400)
                        true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        repeatButton = null
                        button.removeCallbacks(repeatRunnable)
                        false
                    }
                    else -> false
                }
            }
        }

        // === ЛОГИКА НОВЫХ КНОПОК ===
        var advancedPopup: android.widget.PopupWindow? = null

        val btnAdvancedToggle = findViewById<TextView>(R.id.btnAdvancedToggle)
        btnAdvancedToggle.setOnClickListener {
            if (advancedPopup == null) {
                val popupView = layoutInflater.inflate(R.layout.popup_advanced_functions, null)

                val page1 = popupView.findViewById<android.widget.GridLayout>(R.id.page1)
                val page2 = popupView.findViewById<android.widget.GridLayout>(R.id.page2)
                val btnNext = popupView.findViewById<Button>(R.id.btnNextPage)
                val btnPrev = popupView.findViewById<Button>(R.id.btnPrevPage)

                advancedPopup = android.widget.PopupWindow(
                    popupView,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                    true
                )
                advancedPopup?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
                advancedPopup?.isOutsideTouchable = true
                advancedPopup?.isFocusable = true

                // Переключение страниц
                btnNext.setOnClickListener {
                    page1.visibility = android.view.View.GONE
                    page2.visibility = android.view.View.VISIBLE
                    btnNext.visibility = android.view.View.GONE
                    btnPrev.visibility = android.view.View.VISIBLE
                }
                btnPrev.setOnClickListener {
                    page2.visibility = android.view.View.GONE
                    page1.visibility = android.view.View.VISIBLE
                    btnPrev.visibility = android.view.View.GONE
                    btnNext.visibility = android.view.View.VISIBLE
                }

                // === СТРАНИЦА 1 ===
                popupView.findViewById<Button>(R.id.popupX2).setOnClickListener { insertText("^2"); advancedPopup?.dismiss() }
                popupView.findViewById<Button>(R.id.popupX3).setOnClickListener { insertText("^3"); advancedPopup?.dismiss() }

                // === СТРАНИЦА 1: sin, cos, tg, ctg, √, !, log, ln ===
                popupView.findViewById<Button>(R.id.popupSin).setOnClickListener { insertFunction("sin("); advancedPopup?.dismiss() }
                popupView.findViewById<Button>(R.id.popupCos).setOnClickListener { insertFunction("cos("); advancedPopup?.dismiss() }
                
                // Добавляем tg и ctg - создадим кнопки динамически или используем существующие
                // Для этого нужно обновить layout, но пока используем долгие нажатия
                popupView.findViewById<Button>(R.id.popupTan).setOnClickListener { insertFunction("tan("); advancedPopup?.dismiss() }
                
                // Долгое нажатие на tan для ctg
                popupView.findViewById<Button>(R.id.popupTan).setOnLongClickListener {
                    insertFunction("cot(")
                    advancedPopup?.dismiss()
                    true
                }
                
                popupView.findViewById<Button>(R.id.popupSqrt).setOnClickListener { insertFunction("√("); advancedPopup?.dismiss() }
                popupView.findViewById<Button>(R.id.popupFact).setOnClickListener {
                    val cursorPos = tvExpression.selectionStart
                    val expr = state.expression
                    val canInsertFactorial = cursorPos > 0 &&
                            (expr[cursorPos - 1].isDigit() || expr[cursorPos - 1] == ')' || expr[cursorPos - 1] == 'π' || expr[cursorPos - 1] == 'e')
                    if (canInsertFactorial) {
                        insertText("!")
                    }
                    advancedPopup?.dismiss()
                }
                popupView.findViewById<Button>(R.id.popupLog).setOnClickListener { insertFunction("log("); advancedPopup?.dismiss() }
                
                // Долгое нажатие на log для ln
                popupView.findViewById<Button>(R.id.popupLog).setOnLongClickListener {
                    insertFunction("ln(")
                    advancedPopup?.dismiss()
                    true
                }
                
                popupView.findViewById<Button>(R.id.popupPi).setOnClickListener { insertText("π"); advancedPopup?.dismiss() }
                
                // Долгое нажатие на π - открывает окно с числом π до 1000 знаков
                popupView.findViewById<Button>(R.id.popupPi).setOnLongClickListener {
                    showPiDialog()
                    advancedPopup?.dismiss()
                    true
                }

                // Долгое нажатие для тригонометрии с выбором угла
                popupView.findViewById<Button>(R.id.popupSin).setOnLongClickListener {
                    showAngleSelector("sin")
                    advancedPopup?.dismiss()
                    true
                }
                popupView.findViewById<Button>(R.id.popupCos).setOnLongClickListener {
                    showAngleSelector("cos")
                    advancedPopup?.dismiss()
                    true
                }

                // === СТРАНИЦА 2: arcsin, arccos, arctg, arcctg, sh, ch, th, |x|, e^x, e ===
                popupView.findViewById<Button>(R.id.popupAsin).setOnClickListener { insertFunction("arcsin("); advancedPopup?.dismiss() }
                popupView.findViewById<Button>(R.id.popupAcos).setOnClickListener { insertFunction("arccos("); advancedPopup?.dismiss() }
                popupView.findViewById<Button>(R.id.popupAtan).setOnClickListener { insertFunction("arctan("); advancedPopup?.dismiss() }
                
                // Долгое нажатие на arctan для arcctg
                popupView.findViewById<Button>(R.id.popupAtan).setOnLongClickListener {
                    insertFunction("arccot(")
                    advancedPopup?.dismiss()
                    true
                }
                
                popupView.findViewById<Button>(R.id.popupSinh).setOnClickListener { insertFunction("sinh("); advancedPopup?.dismiss() }
                popupView.findViewById<Button>(R.id.popupCosh).setOnClickListener { insertFunction("cosh("); advancedPopup?.dismiss() }
                popupView.findViewById<Button>(R.id.popupTanh).setOnClickListener { insertFunction("tanh("); advancedPopup?.dismiss() }
                
                // Долгое нажатие на tanh для coth
                popupView.findViewById<Button>(R.id.popupTanh).setOnLongClickListener {
                    insertFunction("coth(")
                    advancedPopup?.dismiss()
                    true
                }
                
                popupView.findViewById<Button>(R.id.popupAbs).setOnClickListener { insertFunction("abs("); advancedPopup?.dismiss() }
                popupView.findViewById<Button>(R.id.popupEx).setOnClickListener { insertFunction("exp("); advancedPopup?.dismiss() }
                popupView.findViewById<Button>(R.id.popupE).setOnClickListener { insertText("e"); advancedPopup?.dismiss() }
            }

            advancedPopup?.showAsDropDown(btnAdvancedToggle, 0, -500)
        }

        // Кнопка возведения в степень (иконка ^)
        val btnPower = findViewById<TextView>(R.id.btnPower)
        btnPower.setOnClickListener {
            val cursorPos = tvExpression.selectionStart
            val expr = state.expression

            // 1. Нельзя вставлять в самом начале
            if (cursorPos == 0) return@setOnClickListener

            val charBefore = expr[cursorPos - 1]

            // 2. Нельзя вставлять после другого ^, операторов, скобок или факториала
            val invalidChars = listOf('^', '+', '−', '×', '÷', '(', '!', '°')
            if (charBefore in invalidChars) return@setOnClickListener

            // Если всё ок, вставляем
            insertText("^")
        }

        // Кнопка градуса (иконка °)
        btnDegree.setOnClickListener {
            val cursorPos = tvExpression.selectionStart
            val expr = state.expression

            // 1. Нельзя вставлять в самом начале
            if (cursorPos == 0) return@setOnClickListener

            val charBefore = expr[cursorPos - 1]

            // 2. Нельзя вставлять после другого °, операторов, скобок или ^
            val invalidChars = listOf('°', '^', '+', '−', '×', '÷', '(', '!', 'π', 'e')
            if (charBefore in invalidChars) return@setOnClickListener

            // Если всё ок, вставляем
            val (newExpr, newPos) = state.insertAtCursor("°", cursorPos)
            state.setExpression(newExpr)
            tvExpression.setText(newExpr)
            tvExpression.setSelection(newPos)
            updateLiveResult()
        }

        // Шестеренка настроек
        findViewById<TextView>(R.id.btnSettings).setOnClickListener {
            Toast.makeText(this, "Настройки в разработке!", Toast.LENGTH_SHORT).show()
        }

        // Кнопки операторов
        findViewById<Button>(R.id.btnAdd).setOnClickListener { onOperatorClick("+") }
        findViewById<Button>(R.id.btnSub).setOnClickListener { onOperatorClick("−") }
        findViewById<Button>(R.id.btnMul).setOnClickListener { onOperatorClick("×") }
        findViewById<Button>(R.id.btnDiv).setOnClickListener { onOperatorClick("÷") }

        // Остальные кнопки
        findViewById<Button>(R.id.btnAns).setOnClickListener { onAnsClick() }
        findViewById<Button>(R.id.btnEqual).setOnClickListener { onEqualClick() }

        findViewById<Button>(R.id.btnEqual).setOnLongClickListener {
            if (easterEggStep == 3) {
                val cursorPos = tvExpression.selectionStart
                val (newExpr, newPos) = state.insertAtCursor("=", cursorPos)
                state.setExpression(newExpr)
                tvExpression.setText(newExpr)
                tvExpression.setSelection(newPos)
            }
            checkEasterEgg("=", true)
            true
        }

        findViewById<Button>(R.id.btnClear).setOnClickListener { onClearClick() }
        tvHistoryArrow.setOnClickListener { onHistoryClick() }

        updateDisplays()
    }

    // ==========================================
    // ЛОГИКА СКОБОК (с багфиксом 7)
    // ==========================================
    private fun onBracketClick(forceOpen: Boolean) {
        if (isCursorBlockedByDegree()) return
        if (state.isResultShown) {
            state.setExpression("")
            state.resetResultFlag()
            tvHistoryArrow.visibility = View.GONE
        }

        val cursorPos = tvExpression.selectionStart.coerceIn(0, state.expression.length)
        val textBeforeCursor = state.expression.substring(0, cursorPos)

        // БАГ 7 ИСПРАВЛЕН: Если зажали и перед курсором '(', вставляем ')'
        if (forceOpen && textBeforeCursor.isNotEmpty() && textBeforeCursor.last() == '(') {
            val (newExpr, newPos) = state.insertAtCursor(")", cursorPos)
            state.setExpression(newExpr)
            tvExpression.setText(newExpr)
            tvExpression.setSelection(newPos)
            updateLiveResult()
            return
        }

        val openCount = state.expression.count { it == '(' }
        val closeCount = state.expression.count { it == ')' }
        val needsClosing = openCount > closeCount

        val bracket = if (forceOpen) {
            "("
        } else if (needsClosing && shouldCloseBracket(textBeforeCursor)) {
            ")"
        } else {
            "("
        }

        if (bracket == "(") {
            if (textBeforeCursor.isNotEmpty()) {
                val lastChar = textBeforeCursor.last()
                if (lastChar.isDigit() || lastChar == ')' || lastChar == '°' || lastChar == '!' || lastChar == 'π' || lastChar == 'e') {
                    val (newExpr, newPos) = state.insertAtCursor("×(", cursorPos)
                    state.setExpression(newExpr)
                    tvExpression.setText(newExpr)
                    tvExpression.setSelection(newPos)
                    updateLiveResult()
                    return
                }
            }
        } else {
            val openBeforeCursor = textBeforeCursor.count { it == '(' }
            val closeBeforeCursor = textBeforeCursor.count { it == ')' }
            if (openBeforeCursor <= closeBeforeCursor) return

            val textAfterCursor = state.expression.substring(cursorPos)
            if (textAfterCursor.isNotEmpty()) {
                val nextChar = textAfterCursor.first()
                if (nextChar.isDigit() || nextChar == '(' || nextChar == 'π' || nextChar == 'e') {
                    val (newExpr, newPos) = state.insertAtCursor(")×", cursorPos)
                    state.setExpression(newExpr)
                    tvExpression.setText(newExpr)
                    tvExpression.setSelection(newPos)
                    updateLiveResult()
                    return
                }
            }
        }

        val (newExpr, newPos) = state.insertAtCursor(bracket, cursorPos)
        state.setExpression(newExpr)
        tvExpression.setText(newExpr)
        tvExpression.setSelection(newPos)
        updateLiveResult()
    }

    private fun shouldCloseBracket(textBeforeCursor: String): Boolean {
        if (textBeforeCursor.isEmpty()) return false
        val lastChar = textBeforeCursor.last()
        return lastChar.isDigit() || lastChar == ')' || lastChar == '.' || lastChar == '°' || lastChar == '!'
    }

    // ==========================================
    // ОСТАЛЬНАЯ ЛОГИКА ВВОДА
    // ==========================================
    private fun onDigitClick(digit: String) {
        if (isCursorBlockedByDegree()) return
        // БАГ 3 ИСПРАВЛЕН: Блокируем ввод, если курсор внутри названия функции
        if (isCursorInsideFunctionName()) return

        if (state.isResultShown) {
            state.setExpression("")
            state.resetResultFlag()
            tvHistoryArrow.visibility = View.GONE
        }

        val cursorPos = tvExpression.selectionStart
        val textBeforeCursor = state.expression.substring(0, cursorPos.coerceIn(0, state.expression.length))
        val currentNum = state.getCurrentNumberBeforeCursor(cursorPos)

        // === АВТО-УМНОЖЕНИЕ: если перед цифрой стоит ), ! или °, вставляем × ===
        if (textBeforeCursor.isNotEmpty() &&
            (textBeforeCursor.last() == ')' || textBeforeCursor.last() == '!' || textBeforeCursor.last() == '°' || textBeforeCursor.last() == 'π' || textBeforeCursor.last() == 'e')) {
            val (newExpr, newPos) = state.insertAtCursor("×$digit", cursorPos)
            state.setExpression(newExpr)
            tvExpression.setText(newExpr)
            tvExpression.setSelection(newPos)
            updateLiveResult()
            return
        }

        when {
            digit == "." -> handleDotClick(cursorPos, currentNum)
            digit == "0" -> handleZeroClick(cursorPos, currentNum)
            else -> handleDigitClick(cursorPos, currentNum, digit)
        }
        checkEasterEgg(digit)
    }

    private fun handleDotClick(cursorPos: Int, currentNum: String) {
        val textBeforeCursor = state.expression.substring(0, cursorPos.coerceIn(0, state.expression.length))

        if (textBeforeCursor.isEmpty() || engine.isOperator(textBeforeCursor.last().toString()) || textBeforeCursor.last() == '(') {
            val (numberBlock, numStart, numEnd) = state.getNumberBlockAtCursor(cursorPos)

            if (numberBlock.contains(".")) {
                val dotIndexInBlock = numberBlock.indexOf(".")
                val absoluteDotIndex = numStart + dotIndexInBlock

                state.setExpression(
                    state.expression.substring(0, absoluteDotIndex) +
                            state.expression.substring(absoluteDotIndex + 1)
                )

                val adjustedCursorPos = if (absoluteDotIndex < cursorPos) cursorPos - 1 else cursorPos
                val (newExpr, newPos) = state.insertAtCursor("0.", adjustedCursorPos)
                state.setExpression(newExpr)
                tvExpression.setText(newExpr)
                tvExpression.setSelection(newPos)
                updateLiveResult()
                return
            } else {
                val (newExpr, newPos) = state.insertAtCursor("0.", cursorPos)
                state.setExpression(newExpr)
                tvExpression.setText(newExpr)
                tvExpression.setSelection(newPos)
                updateLiveResult()
                return
            }
        }

        val (numberBlock, numStart, numEnd) = state.getNumberBlockAtCursor(cursorPos)

        if (numberBlock.contains(".")) {
            val dotIndexInBlock = numberBlock.indexOf(".")
            val absoluteDotIndex = numStart + dotIndexInBlock

            state.setExpression(
                state.expression.substring(0, absoluteDotIndex) +
                        state.expression.substring(absoluteDotIndex + 1)
            )

            val newCursorPos = if (absoluteDotIndex < cursorPos) cursorPos - 1 else cursorPos
            val (newExpr, _) = state.insertAtCursor(".", newCursorPos)
            state.setExpression(newExpr)

            val (newNumberBlock, newNumStart, newNumEnd) = state.getNumberBlockAtCursor(newCursorPos + 1)
            val cleanedBlock = state.cleanLeadingZeros(newNumberBlock)
            val lengthDiff = newNumberBlock.length - cleanedBlock.length

            state.setExpression(
                state.expression.substring(0, newNumStart) +
                        cleanedBlock +
                        state.expression.substring(newNumEnd)
            )

            val finalCursorPos = (newCursorPos + 1 - lengthDiff).coerceIn(0, state.expression.length)
            tvExpression.setText(state.expression)
            tvExpression.setSelection(finalCursorPos)
            updateLiveResult()
            return
        }

        val (newExpr, newPos) = state.insertAtCursor(".", cursorPos)
        state.setExpression(newExpr)
        tvExpression.setText(newExpr)
        tvExpression.setSelection(newPos)
        updateLiveResult()
    }

    private fun handleZeroClick(cursorPos: Int, currentNum: String) {
        when {
            currentNum.isEmpty() -> insertDigit("0", cursorPos)
            currentNum == "0" -> return
            currentNum == "0." -> insertDigit("0", cursorPos)
            currentNum.startsWith("0") && currentNum.length > 1 -> insertDigit("0", cursorPos)
            else -> insertDigit("0", cursorPos)
        }
    }

    private fun handleDigitClick(cursorPos: Int, currentNum: String, digit: String) {
        val textBeforeCursor = state.expression.substring(0, cursorPos.coerceIn(0, state.expression.length))

        if (currentNum == "0" && !textBeforeCursor.endsWith("0.")) {
            val beforeNum = textBeforeCursor.dropLast(1)
            state.setExpression(beforeNum + state.expression.substring(cursorPos))
            tvExpression.setText(state.expression)
            tvExpression.setSelection(beforeNum.length)
            insertDigit(digit, beforeNum.length)
        } else {
            insertDigit(digit, cursorPos)
        }
    }

    private fun insertDigit(digit: String, cursorPos: Int) {
        val (newExpr, newPos) = state.insertAtCursor(digit, cursorPos)
        state.setExpression(newExpr)
        tvExpression.setText(newExpr)
        tvExpression.setSelection(newPos)
        updateLiveResult()
    }

    private fun onOperatorClick(op: String) {
        if (isCursorBlockedByDegree()) return
        if (isCursorInsideFunctionName()) return
        if (state.expression.isEmpty() && op != "−") return

        if (state.expression.isEmpty() && op == "−") {
            val (newExpr, newPos) = state.insertAtCursor(op, 0)
            state.setExpression(newExpr)
            tvExpression.setText(newExpr)
            tvExpression.setSelection(newPos)
            updateLiveResult()
            return
        }

        if (state.isResultShown) {
            state.setExpression(tvDisplay.text.toString() + op)
            state.resetResultFlag()
            tvHistoryArrow.visibility = View.GONE
            val (newExpr, newPos) = state.insertAtCursor("", state.expression.length)
            tvExpression.setText(newExpr)
            tvExpression.setSelection(newPos)
        } else {
            val cursorPos = tvExpression.selectionStart
            val textBeforeCursor = state.expression.substring(0, cursorPos.coerceIn(0, state.expression.length))

            if (textBeforeCursor.isNotEmpty() && engine.isOperator(textBeforeCursor.last().toString())) {
                state.setExpression(
                    textBeforeCursor.dropLast(1) + op + state.expression.substring(cursorPos)
                )
                tvExpression.setText(state.expression)
                tvExpression.setSelection(cursorPos)
            } else {
                val (newExpr, newPos) = state.insertAtCursor(op, cursorPos)
                state.setExpression(newExpr)
                tvExpression.setText(newExpr)
                tvExpression.setSelection(newPos)
            }
        }
        updateLiveResult()
        checkEasterEgg(op)
    }

    private fun onEqualClick() {
        if (isCursorBlockedByDegree()) return
        val result = state.calculateFinal(engine)
        if (result.isEmpty()) return

        tvExpression.setText(state.expression)
        tvExpression.setSelection(state.expression.length)
        tvDisplay.text = state.expression
        tvHistoryArrow.visibility = View.VISIBLE

        findViewById<Button>(R.id.btnAns).isEnabled = true
        findViewById<Button>(R.id.btnAns).alpha = 1.0f
        checkEasterEgg("=", false)
    }

    private fun onHistoryClick() {
        state.restoreHistory()
        state.resetResultFlag()
        tvHistoryArrow.visibility = View.GONE
        updateDisplays()
    }

    private fun onBackClick() {
        if (state.isResultShown) {
            onClearClick()
            return
        }

        val start = tvExpression.selectionStart
        val end = tvExpression.selectionEnd
        val expr = state.expression

        // 1. Если выделен фрагмент текста, просто удаляем его (стандартное поведение)
        if (start != end) {
            val (newExpr, newPos) = state.removeAtCursor(start, end)
            state.setExpression(newExpr)
            tvExpression.setText(newExpr)
            tvExpression.setSelection(newPos)
            updateLiveResult()
            return
        }

        // 2. Если курсор в самом начале, удалять нечего
        if (start == 0) return

        val charBefore = expr[start - 1]

        // 3. === УМНОЕ УДАЛЕНИЕ ФУНКЦИЙ ===
        // Если перед курсором стоит '(', проверяем, не является ли она частью функции
        if (charBefore == '(') {
            // Список функций, отсортированный по длине (чтобы "arcsin" проверялся раньше "sin")
            val functions = listOf(
                "arcsin", "arccos", "arctan",
                "sinh", "cosh", "tanh", "coth",
                "sin", "cos", "tan", "cot",
                "log", "ln", "√", "abs", "fact"
            )

            val prefix = expr.substring(0, start - 1)
            for (func in functions) {
                if (prefix.endsWith(func)) {
                    // Нашли совпадение! Удаляем и название функции, и скобку
                    val deleteLength = func.length + 1
                    val newStart = start - deleteLength
                    val newExpr = expr.substring(0, newStart) + expr.substring(start)

                    state.setExpression(newExpr)
                    tvExpression.setText(newExpr)
                    tvExpression.setSelection(newStart)
                    updateLiveResult()
                    return // Выходим, удаление выполнено
                }
                // Проверяем случай с умножением перед функцией: ×func(
                if (prefix.endsWith("×$func")) {
                    // Удаляем ×, название функции и скобку
                    val deleteLength = func.length + 2 // × + func + (
                    val newStart = start - deleteLength
                    val newExpr = expr.substring(0, newStart) + expr.substring(start)

                    state.setExpression(newExpr)
                    tvExpression.setText(newExpr)
                    tvExpression.setSelection(newStart)
                    updateLiveResult()
                    return // Выходим, удаление выполнено
                }
            }
        }

        // 3.1. === УДАЛЕНИЕ ФУНКЦИИ, ЕСЛИ КУРСОР ПЕРЕД СКОБКОЙ ===
        // Проверяем случай, когда курсор стоит перед '(' функции (например: sin|( )
        if (start < expr.length && expr[start] == '(') {
            val functions = listOf(
                "arcsin", "arccos", "arctan",
                "sinh", "cosh", "tanh", "coth",
                "sin", "cos", "tan", "cot",
                "log", "ln", "√", "abs", "fact"
            )

            val prefix = expr.substring(0, start)
            for (func in functions) {
                if (prefix.endsWith(func)) {
                    // Нашли совпадение! Удаляем название функции
                    val deleteLength = func.length
                    val newStart = start - deleteLength
                    val newExpr = expr.substring(0, newStart) + expr.substring(start)

                    state.setExpression(newExpr)
                    tvExpression.setText(newExpr)
                    tvExpression.setSelection(newStart)
                    updateLiveResult()
                    return // Выходим, удаление выполнено
                }
                // Проверяем случай с умножением перед функцией: ×func|(
                if (prefix.endsWith("×$func")) {
                    // Удаляем × и название функции
                    val deleteLength = func.length + 1 // × + func
                    val newStart = start - deleteLength
                    val newExpr = expr.substring(0, newStart) + expr.substring(start)

                    state.setExpression(newExpr)
                    tvExpression.setText(newExpr)
                    tvExpression.setSelection(newStart)
                    updateLiveResult()
                    return // Выходим, удаление выполнено
                }
            }
        }

        // 4. Если это не функция, или курсор внутри названия функции, блокируем удаление букв
        // (чтобы нельзя было стереть "si" из "sin", оставив "n(")
        if (isCursorInsideFunctionName()) return

        // 5. Стандартное удаление одного символа
        val (newExpr, newPos) = state.removeAtCursor(start, end)
        state.setExpression(newExpr)
        tvExpression.setText(newExpr)
        tvExpression.setSelection(newPos)
        updateLiveResult()
    }

    private fun onClearClick() {
        easterEggStep = 0
        state.clearAll()
        tvHistoryArrow.visibility = View.GONE
        findViewById<Button>(R.id.btnAns).isEnabled = false
        findViewById<Button>(R.id.btnAns).alpha = 0.5f
        updateDisplays()
    }

    private fun onAnsClick() {
        val ans = state.insertLastAnswer()
        if (ans.isEmpty()) return

        if (state.isResultShown) {
            updateDisplays()
        } else {
            val cursorPos = tvExpression.selectionStart
            val (newExpr, newPos) = state.insertAtCursor(ans, cursorPos)
            tvExpression.setText(newExpr)
            tvExpression.setSelection(newPos)
            updateLiveResult()
        }
    }

    private fun updateDisplays() {
        tvExpression.setText(state.expression)
        val safePos = tvExpression.selectionStart.coerceIn(0, state.expression.length)
        tvExpression.setSelection(safePos)
        updateLiveResult()
        adjustTextSize()
    }

    private fun updateLiveResult() {
        val liveResult = engine.evaluateMath(state.expression)
        tvDisplay.text = if (state.expression.isEmpty()) "" else engine.formatResult(liveResult)
        adjustTextSize()
        updateDegreeButtonVisibility()
    }

    private fun adjustTextSize() {
        val exprLength = state.expression.length
        when {
            exprLength > 20 -> tvExpression.textSize = 24f
            exprLength > 15 -> tvExpression.textSize = 30f
            exprLength > 10 -> tvExpression.textSize = 36f
            else -> tvExpression.textSize = 42f
        }

        val displayText = tvDisplay.text.toString()
        when {
            displayText.length > 15 -> tvDisplay.textSize = 20f
            displayText.length > 10 -> tvDisplay.textSize = 24f
            else -> tvDisplay.textSize = 28f
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("expression", state.expression)
        outState.putString("lastExpression", state.lastExpression)
        outState.putString("lastAnswer", state.lastAnswer)
        outState.putBoolean("isResultShown", state.isResultShown)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        state.setExpression(savedInstanceState.getString("expression", ""))
        state.setLastExpression(savedInstanceState.getString("lastExpression", ""))
        state.setLastAnswer(savedInstanceState.getString("lastAnswer", ""))
        if (savedInstanceState.getBoolean("isResultShown", false)) {
            state.setResultShown(savedInstanceState.getBoolean("isResultShown", false))
        }
        updateDisplays()
    }

    // === СЕКРЕТНАЯ КОМБИНАЦИЯ ===
    private fun checkEasterEgg(input: String, isLongPress: Boolean = false) {
        val expr = state.expression

        val isCorrect = when (easterEggStep) {
            0 -> input == "2" && expr == "2"
            1 -> input == "+" && expr == "2+"
            2 -> input == "2" && expr == "2+2"
            3 -> input == "=" && isLongPress && expr == "2+2="
            4 -> input == "5" && expr == "2+2=5"
            else -> false
        }

        if (isCorrect) {
            easterEggStep++
            if (easterEggStep == 5) {
                showMadeByDialog()
                easterEggStep = 0
            }
        } else {
            if (easterEggStep == 4) {
                easterEggStep = 0
                onClearClick()
            } else {
                easterEggStep = 0
            }
        }
    }

    private fun showMadeByDialog() {
        startActivity(android.content.Intent(this, EasterEggActivity::class.java))
    }

    override fun onResume() {
        super.onResume()
        if (easterEggStep == 0 && state.expression.isNotEmpty()) {
            onClearClick()
        }
    }

    // === ВСПОМОГАТЕЛЬНЫЕ ФУНКЦИИ ===

    private fun insertText(text: String) {
        val cursorPos = tvExpression.selectionStart
        val (newExpr, newPos) = state.insertAtCursor(text, cursorPos)
        state.setExpression(newExpr)
        tvExpression.setText(newExpr)
        tvExpression.setSelection(newPos)
        updateLiveResult()
    }

    // БАГ 4 и 5 ИСПРАВЛЕНЫ: Авто-умножение + правильное позиционирование курсора
    private fun insertFunction(func: String) {
        val cursorPos = tvExpression.selectionStart
        val textBeforeCursor = state.expression.substring(0, cursorPos.coerceIn(0, state.expression.length))

        // Авто-умножение, если перед функцией число, ')', '°', '!', 'π', 'e'
        val needsMultiply = textBeforeCursor.isNotEmpty() &&
                (textBeforeCursor.last().isDigit() ||
                        textBeforeCursor.last() == ')' ||
                        textBeforeCursor.last() == '°' ||
                        textBeforeCursor.last() == '!' ||
                        textBeforeCursor.last() == 'π' ||
                        textBeforeCursor.last() == 'e')

        // === ГЛАВНОЕ ИСПРАВЛЕНИЕ: Если функция передается с "(", добавляем ")" в конец ===
        val finalFunc = if (func.endsWith("(")) "$func)" else func
        val textToInsert = if (needsMultiply) "×$finalFunc" else finalFunc

        val (newExpr, newPos) = state.insertAtCursor(textToInsert, cursorPos)
        state.setExpression(newExpr)
        tvExpression.setText(newExpr)

        // Ставим курсор строго ВНУТРИ скобок (между "(" и ")")
        val funcStartInNewExpr = newPos - textToInsert.length
        val openBracketIndex = newExpr.indexOf('(', funcStartInNewExpr)
        if (openBracketIndex != -1) {
            tvExpression.setSelection(openBracketIndex + 1)
        } else {
            tvExpression.setSelection(newPos)
        }
        updateLiveResult()
    }

    private fun updateDegreeButtonVisibility() {
        val cursorPos = tvExpression.selectionStart
        val expr = state.expression

        if (cursorPos < 0 || cursorPos >= expr.length) {
            btnDegree.visibility = View.GONE
            return
        }

        // Сразу после курсора должна стоять ')'
        if (expr[cursorPos] != ')') {
            btnDegree.visibility = View.GONE
            return
        }

        // Идём назад от курсора. Разрешены только цифры и одна точка.
        var i = cursorPos - 1
        var hasDot = false

        while (i >= 0) {
            val char = expr[i]
            if (char.isDigit()) {
                i--
            } else if (char == '.') {
                if (hasDot) break
                hasDot = true
                i--
            } else if (char == '°') {
                // Если значок градуса уже есть, кнопку не показываем
                btnDegree.visibility = View.GONE
                return
            } else {
                break
            }
        }

        // После пропуска цифр и точки мы должны упираться в '('
        if (i < 0 || expr[i] != '(') {
            btnDegree.visibility = View.GONE
            return
        }

        // === ИСПРАВЛЕНИЕ: Проверяем точное название функции перед '(' ===
        val beforeBracket = expr.substring(0, i)

        // Функции, которые МОГУТ принимать градусы (только прямая тригонометрия)
        val trigFunctions = listOf("sin", "cos", "tan", "cot", "sinh", "cosh", "tanh", "coth")

        // Проверяем, что beforeBracket ЗАКАНЧИВАЕТСЯ на одну из функций,
        // но НЕ является её частью (например, "arcsin" не должен матчить "sin")
        var isSupported = false
        for (func in trigFunctions) {
            if (beforeBracket.endsWith(func)) {
                // Проверяем, что перед функцией нет буквы (иначе это часть другого слова)
                val prefix = beforeBracket.substring(0, beforeBracket.length - func.length)
                if (prefix.isEmpty() || !prefix.last().isLetter()) {
                    isSupported = true
                    break
                }
            }
        }

        btnDegree.visibility = if (isSupported) View.VISIBLE else View.GONE
    }

    // БАГ 3 ИСПРАВЛЕН: Проверяем, находится ли курсор внутри названия функции
    private fun isCursorInsideFunctionName(): Boolean {
        val cursorPos = tvExpression.selectionStart
        val expr = state.expression

        if (cursorPos <= 0 || cursorPos > expr.length) return false

        // Специальная проверка для символа корня √
        if (cursorPos > 0 && expr[cursorPos - 1] == '√') {
            return true
        }

        // Идём назад от курсора, собираем буквы
        var i = cursorPos - 1
        while (i >= 0 && (expr[i].isLetter() || expr[i] == '√')) {
            i--
        }

        // Проверяем, является ли собранное слово функцией
        val word = expr.substring(i + 1, cursorPos)
        return allTrigFunctions.any { it.startsWith(word) && word.isNotEmpty() }
    }

    private fun showAngleSelector(funcName: String) {
        val dialog = android.app.Dialog(this)
        dialog.setContentView(R.layout.dialog_angle_selector)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val tvAngle = dialog.findViewById<TextView>(R.id.tvAngleValue)
        val angleCircle = dialog.findViewById<AngleCircleView>(R.id.angleCircle)
        val btnMode = dialog.findViewById<Button>(R.id.btnToggleMode)
        val btnConfirm = dialog.findViewById<Button>(R.id.btnConfirm)

        var angleDegrees = 0f
        var isDegree = true

        fun formatRadians(degrees: Float): String {
            val ratio = degrees / 180f
            fun isClose(a: Float, b: Float): Boolean = Math.abs(a - b) < 0.01f

            return when {
                isClose(ratio, 0f) -> "0"
                isClose(ratio, 1f) -> "π"
                isClose(ratio, -1f) -> "-π"
                isClose(ratio, 0.5f) -> "π/2"
                isClose(ratio, -0.5f) -> "-π/2"
                isClose(ratio, 2f) -> "2π"
                isClose(ratio, -2f) -> "-2π"
                isClose(ratio, 1.5f) -> "3π/2"
                isClose(ratio, -1.5f) -> "-3π/2"
                isClose(ratio, 0.25f) -> "π/4"
                isClose(ratio, -0.25f) -> "-π/4"
                isClose(ratio, 0.3333f) -> "π/3"
                isClose(ratio, -0.3333f) -> "-π/3"
                isClose(ratio, 0.6667f) -> "2π/3"
                isClose(ratio, -0.6667f) -> "-2π/3"
                isClose(ratio, 0.75f) -> "3π/4"
                isClose(ratio, -0.75f) -> "-3π/4"
                isClose(ratio, 0.1667f) -> "π/6"
                isClose(ratio, -0.1667f) -> "-π/6"
                isClose(ratio, 0.8333f) -> "5π/6"
                isClose(ratio, -0.8333f) -> "-5π/6"
                isClose(ratio, 1.1667f) -> "7π/6"
                isClose(ratio, -1.1667f) -> "-7π/6"
                isClose(ratio, 1.25f) -> "5π/4"
                isClose(ratio, -1.25f) -> "-5π/4"
                isClose(ratio, 1.3333f) -> "4π/3"
                isClose(ratio, -1.3333f) -> "-4π/3"
                isClose(ratio, 1.6667f) -> "5π/3"
                isClose(ratio, -1.6667f) -> "-5π/3"
                isClose(ratio, 1.75f) -> "7π/4"
                isClose(ratio, -1.75f) -> "-7π/4"
                else -> String.format("%.2fπ", ratio)
            }
        }

        angleCircle.setOnAngleChangedListener { degrees ->
            angleDegrees = degrees
            if (isDegree) {
                tvAngle.text = "${angleDegrees.toInt()}°"
            } else {
                tvAngle.text = formatRadians(angleDegrees)
            }
        }

        btnMode.setOnClickListener {
            isDegree = !isDegree
            btnMode.text = if (isDegree) "DEG" else "RAD"
            angleCircle.setMode(isDegree)
            if (isDegree) {
                tvAngle.text = "${angleDegrees.toInt()}°"
            } else {
                tvAngle.text = formatRadians(angleDegrees)
            }
        }

        btnConfirm.setOnClickListener {
            dialog.dismiss()
        }

        // При закрытии диалога вставляем функцию
        dialog.setOnDismissListener {
            // Если выбраны градусы, добавляем значок °. Парсер САМ переведет это в радианы!
            val suffix = if (isDegree) "°" else ""
            insertFunction("$funcName(${angleDegrees.toInt()}$suffix)")
        }

        dialog.show()
    }

    private fun showPiDialog() {
        val dialog = android.app.Dialog(this)
        dialog.setContentView(R.layout.dialog_pi)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val tvPiValue = dialog.findViewById<TextView>(R.id.tvPiValue)
        val btnCopyPi = dialog.findViewById<Button>(R.id.btnCopyPi)

        // Число π до 1000 знаков после запятой
        val piValue = "3.1415926535897932384626433832795028841971693993751058209749445923078164062862089986280348253421170679821480865132823066470938446095505822317253594081284811174502841027019385211055596446229489549303819644288109756659334461284756482337867831652712019091456485669234603486104543266482133936072602491412737245870066063155881748815209209628292540917153643678925903600113305305488204665213841469519415116094330572703657595919530921861173819326117931051185480744623799627495673518857527248912279381830119491298336733624406566430860213949463952247371907021798609437027705392171762931767523846748184676694051320005681271452635608277857713427577896091736371787214684409012249534301465495853710507922796892589235420199561121290219608640344181598136297747713099605187072113499999983729780499510597317328160963185950244594553469083026425223082533446850352619311881710100031378387528865875332083814206171776691473035982534904287554687311595628638823537875937519577818577805321712268066130019278766111959092164201989"

        tvPiValue.text = piValue

        btnCopyPi.setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Pi value", piValue)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Число π скопировано в буфер обмена", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun isCursorBlockedByDegree(): Boolean {
        val cursor = tvExpression.selectionStart
        val expr = state.expression
        if (cursor > 0 && cursor < expr.length) {
            if (expr[cursor - 1] == '°' && expr[cursor] == ')') return true
        }
        return false
    }
}