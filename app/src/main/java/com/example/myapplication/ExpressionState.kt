package com.example.myapplication

class ExpressionState {

    var expression = ""
        private set

    var lastExpression = ""
        private set

    var isResultShown = false
        private set

    var lastAnswer = ""
        private set

    fun setExpression(newExpression: String) {
        expression = newExpression
    }

    fun insertAtCursor(text: String, cursorPos: Int): Pair<String, Int> {
        val safePos = cursorPos.coerceIn(0, expression.length)
        val beforeCursor = expression.substring(0, safePos)
        val afterCursor = expression.substring(safePos)

        expression = beforeCursor + text + afterCursor
        return Pair(expression, safePos + text.length)
    }

    fun removeAtCursor(start: Int, end: Int): Pair<String, Int> {
        val safeStart = start.coerceIn(0, expression.length)
        val safeEnd = end.coerceIn(0, expression.length)

        if (safeStart == safeEnd && safeStart > 0) {
            expression = expression.substring(0, safeStart - 1) + expression.substring(safeEnd)
            return Pair(expression, safeStart - 1)
        } else if (safeStart != safeEnd) {
            expression = expression.substring(0, safeStart) + expression.substring(safeEnd)
            return Pair(expression, safeStart)
        }
        return Pair(expression, safeStart)
    }

    // ОБНОВЛЕНО: добавлены скобки () в разделители, чтобы корректно определять начало числа после них
    fun getCurrentNumberBeforeCursor(cursorPos: Int): String {
        val textBeforeCursor = expression.substring(0, cursorPos.coerceIn(0, expression.length))
        return textBeforeCursor.split(Regex("[+\\-×÷()]")).lastOrNull() ?: ""
    }

    fun getNumberBlockAtCursor(cursorPos: Int): Triple<String, Int, Int> {
        val safePos = cursorPos.coerceIn(0, expression.length)

        var numStart = safePos
        while (numStart > 0 && (expression[numStart - 1].isDigit() || expression[numStart - 1] == '.')) {
            numStart--
        }
        var numEnd = safePos
        while (numEnd < expression.length && (expression[numEnd].isDigit() || expression[numEnd] == '.')) {
            numEnd++
        }

        return Triple(expression.substring(numStart, numEnd), numStart, numEnd)
    }

    fun cleanLeadingZeros(num: String): String {
        if (num == "0" || num == "0.") return num
        var cleaned = num.trimStart('0')
        if (cleaned.isEmpty() || cleaned.startsWith(".")) {
            cleaned = "0" + cleaned
        }
        return cleaned
    }

    fun calculateFinal(engine: CalculatorEngine): String {
        if (expression.isEmpty()) return ""

        val result = engine.evaluateMath(expression)
        val formattedResult = engine.formatResult(result)

        lastExpression = expression
        lastAnswer = formattedResult
        expression = formattedResult
        isResultShown = true

        return formattedResult
    }

    fun restoreHistory() {
        expression = lastExpression
        isResultShown = false
    }

    fun clearAll() {
        expression = ""
        lastExpression = ""
        lastAnswer = ""
        isResultShown = false
    }

    fun setResultShown(shown: Boolean) {
        isResultShown = shown
    }

    fun setLastExpression(expr: String) {
        lastExpression = expr
    }

    fun setLastAnswer(answer: String) {
        lastAnswer = answer
    }

    fun resetResultFlag() {
        isResultShown = false
    }

    fun insertLastAnswer(): String {
        if (lastAnswer.isEmpty()) return ""

        if (isResultShown) {
            expression = lastAnswer
            isResultShown = false
        } else {
            expression += lastAnswer
        }
        return lastAnswer
    }
}