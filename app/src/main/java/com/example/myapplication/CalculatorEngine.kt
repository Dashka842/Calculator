package com.example.myapplication

import kotlin.math.*

class CalculatorEngine {

    fun evaluateMath(expr: String): Double {
        if (expr.isEmpty()) return 0.0

        val sanitized = expr
            .replace("×", "*")
            .replace("÷", "/")
            .replace("−", "-")
            .replace(" ", "")
        if (sanitized.isEmpty()) return 0.0

        val cleanExpr = sanitized.trimEnd('+', '-', '*', '/', '%', '^')
        if (cleanExpr.isEmpty()) return 0.0

        val openCount = cleanExpr.count { it == '(' }
        val closeCount = cleanExpr.count { it == ')' }
        val autoClosed = if (openCount > closeCount) {
            cleanExpr + ")".repeat(openCount - closeCount)
        } else {
            cleanExpr
        }

        if (!isValidExpression(expr)) {
            return Double.NaN
        }

        class Parser(val s: String) {
            var pos = 0

            fun parse(): Double = parseExpression()

            fun parseExpression(): Double {
                var result = parseTerm()
                while (pos < s.length && (s[pos] == '+' || s[pos] == '-')) {
                    val op = s[pos]
                    pos++
                    val right = parseTerm()
                    result = if (op == '+') result + right else result - right
                }
                return result
            }

            fun parseTerm(): Double {
                var result = parsePower()
                while (pos < s.length && (s[pos] == '*' || s[pos] == '/')) {
                    val op = s[pos]
                    pos++
                    val right = parsePower()
                    result = if (op == '*') result * right else if (right != 0.0) result / right else Double.NaN
                }
                return result
            }

            fun parsePower(): Double {
                var result = parseFactor()
                if (pos < s.length && s[pos] == '^') {
                    pos++
                    val exponent = parsePower()
                    result = result.pow(exponent)
                }
                return result
            }

            fun parseFactor(): Double {
                if (pos < s.length && s[pos] == '-') {
                    pos++
                    var res = parsePrimary()
                    while (pos < s.length && s[pos] == '!') {
                        pos++
                        res = factorial(res)
                    }
                    return -res
                }

                var res = parsePrimary()
                while (pos < s.length && s[pos] == '!') {
                    pos++
                    res = factorial(res)
                }
                return res
            }

            private fun parsePrimary(): Double {
                if (pos < s.length && s[pos] == '(') {
                    pos++
                    val result = parseExpression()
                    if (pos < s.length && s[pos] == ')') pos++
                    return result
                }

                val functions = listOf("arcsin", "arccos", "arctan", "sinh", "cosh", "tanh", "coth", "sin", "cos", "tan", "cot", "log", "ln", "√", "abs")
                for (func in functions) {
                    if (s.substring(pos).startsWith(func)) {
                        pos += func.length
                        if (pos < s.length && s[pos] == '(') {
                            pos++
                            val arg = parseExpression()
                            if (pos < s.length && s[pos] == ')') pos++
                            return calculateFunction(func, arg)
                        }
                    }
                }

                var numStr = ""
                var isDegree = false

                while (pos < s.length && (s[pos].isDigit() || s[pos] == '.')) {
                    numStr += s[pos]
                    pos++
                }

                if (pos < s.length && s[pos] == '°') {
                    isDegree = true
                    pos++
                }

                if (numStr.isNotEmpty()) {
                    var value = numStr.toDouble()
                    if (isDegree) {
                        value = value * Math.PI / 180.0
                    }
                    return value
                }

                if (s.substring(pos).startsWith("pi")) {
                    pos += 2
                    return Math.PI
                }
                if (s.substring(pos).startsWith("e") && (pos + 1 >= s.length || !s[pos + 1].isLetter())) {
                    pos++
                    return Math.E
                }

                return 0.0
            }

            private fun calculateFunction(func: String, arg: Double): Double {
                return when (func) {
                    "sin" -> sin(arg)
                    "cos" -> cos(arg)
                    "tan" -> tan(arg)
                    "cot" -> if (tan(arg) != 0.0) 1.0 / tan(arg) else Double.NaN

                    // === ПРОВЕРКА ДИАПАЗОНА ДЛЯ АРКФУНКЦИЙ ===
                    "arcsin" -> if (arg in -1.0..1.0) asin(arg) else Double.NaN
                    "arccos" -> if (arg in -1.0..1.0) acos(arg) else Double.NaN
                    "arctan" -> atan(arg) // диапазон не ограничен

                    "sinh" -> sinh(arg)
                    "cosh" -> cosh(arg)
                    "tanh" -> tanh(arg)
                    "coth" -> if (tanh(arg) != 0.0) 1.0 / tanh(arg) else Double.NaN

                    // === ПРОВЕРКА ДИАПАЗОНА ДЛЯ ЛОГАРИФМОВ И КОРНЯ ===
                    "log" -> if (arg > 0) log10(arg) else Double.NaN
                    "ln" -> if (arg > 0) ln(arg) else Double.NaN
                    "√" -> if (arg >= 0) sqrt(arg) else Double.NaN
                    "abs" -> abs(arg)

                    else -> Double.NaN
                }
            }

            private fun factorial(n: Double): Double {
                if (n < 0 || n != n.toLong().toDouble()) return Double.NaN
                var result = 1.0
                for (i in 2..n.toInt()) {
                    result *= i
                    if (result.isInfinite()) return Double.POSITIVE_INFINITY
                }
                return result
            }
        }

        return try {
            val parser = Parser(autoClosed)
            val result = parser.parse()
            if (parser.pos != autoClosed.length) Double.NaN else result
        } catch (e: Exception) {
            Double.NaN
        }
    }

    fun formatResult(value: Double): String {
        if (value.isNaN()) return "Ошибка"
        if (value.isInfinite()) return "∞"
        if (Math.abs(value) > 1e15 || (Math.abs(value) < 1e-10 && value != 0.0)) {
            return String.format("%.6e", value)
        }
        return if (value == value.toLong().toDouble()) {
            value.toLong().toString()
        } else {
            String.format("%.8f", value).trimEnd('0').trimEnd('.')
        }
    }

    fun isOperator(char: String): Boolean {
        return char in listOf("+", "−", "×", "÷", "^")
    }

    private fun isValidExpression(expr: String): Boolean {
        if (expr.isEmpty()) return true
        if (expr.first() in listOf('+', '×', '÷', '.', '^', '!', ')', '°')) return false

        var bracketBalance = 0
        val validFuncs = listOf("arcsin", "arccos", "arctan", "sinh", "cosh", "tanh", "coth", "sin", "cos", "tan", "cot", "log", "ln", "√", "abs", "fact", "pi")
        var tempExpr = expr
        for (f in validFuncs.sortedByDescending { it.length }) {
            tempExpr = tempExpr.replace(f, "1")
        }
        if (tempExpr.any { it.isLetter() && it != 'e' }) return false

        // Функции, которые МОГУТ принимать градусы
        val trigFunctions = listOf("sin", "cos", "tan", "cot", "sinh", "cosh", "tanh", "coth")

        for (i in 0 until expr.length) {
            val current = expr[i]
            val prev = if (i > 0) expr[i - 1] else ' '
            val next = if (i + 1 < expr.length) expr[i + 1] else ' '

            if (current == '(') bracketBalance++
            if (current == ')') bracketBalance--
            if (bracketBalance < 0) return false

            // === НОВАЯ ПРОВЕРКА: ° не может стоять перед ) если функция не тригонометрическая ===
            if (current == ')' && prev == '°') {
                var j = i - 2
                while (j >= 0 && expr[j] != '(') j--
                if (j >= 0) {
                    val beforeBracket = expr.substring(0, j)
                    if (!trigFunctions.any { beforeBracket.endsWith(it) }) return false
                }
            }

            if (current == '!') {
                if (prev in listOf('!', '+', '−', '×', '÷', '^', '(', '.', '°', '-', '*', '/')) return false
                if (next in listOf('!', '.', '°', 'π', 'e') || next.isDigit()) return false
            }

            if (current == '°') {
                if (prev in listOf('°', '+', '−', '×', '÷', '^', '(', '.', '!', '-', '*', '/')) return false
                if (next in listOf('°', '^', '!', '.', 'π', 'e') || next.isDigit()) return false
            }

            if (current == '^') {
                if (prev in listOf('^', '(', '+', '−', '×', '÷', '.', '!', '-', '*', '/')) return false
                if (next in listOf('^', ')', '°', '!', 'π', 'e', '.', '+', '−', '×', '÷', '-', '*', '/')) return false
            }

            if (current in listOf('+', '−', '×', '÷', '*', '/')) {
                if (i == 0 && current != '-' && current != '−') return false
                if (prev in listOf('+', '−', '×', '÷', '*', '/') && !(prev == '-' && current == '(')) return false
                if (next in listOf('+', '−', '×', '÷', '*', '/', ')', '^', '!', '°')) return false
            }

            if (current == '(' && next in listOf('+', '−', '×', '÷', '*', '/', '^', '!', '°', ')')) return false
            if (current == ')' && prev in listOf('+', '−', '×', '÷', '*', '/', '(')) return false

            if (current == '.' && next == '.') return false
        }

        val parts = expr.split(Regex("[+\\-−×÷^!()* /]"))
        for (part in parts) {
            if (part.isEmpty()) continue
            if (part.startsWith("0") && part.length > 1 && part[1] != '.') return false
            if (part.count { it == '.' } > 1) return false
        }

        return true
    }
}