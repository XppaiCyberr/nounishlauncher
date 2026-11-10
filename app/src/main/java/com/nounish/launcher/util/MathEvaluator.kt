package com.nounish.launcher.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs
import kotlin.math.pow

object MathEvaluator {

    private sealed class Token {
        data class Number(val value: Double) : Token()
        data object Plus : Token()
        data object Minus : Token()
        data object Multiply : Token()
        data object Divide : Token()
        data object Power : Token()
        data object Percent : Token()
        data object LParen : Token()
        data object RParen : Token()
    }

    /**
     * Safely evaluates an arithmetic expression string.
     * Supports: +, -, *, /, ^, %, parentheses, decimals.
     *
     * Returns null if:
     * - The input contains invalid characters (e.g. letters from regular app searches)
     * - The syntax is invalid (e.g. mismatched parens, hanging operators)
     * - Division or modulo by zero occurs
     * - The result is NaN or Infinite
     * - [requireOperator] is true and no arithmetic operator (+, -, *, /, ^, %) was used.
     */
    fun evaluate(expression: String, requireOperator: Boolean = true): Double? {
        var cleaned = expression.trim()
        if (cleaned.isEmpty()) return null

        // Allow leading or trailing '=' gracefully
        if (cleaned.startsWith("=")) {
            cleaned = cleaned.substring(1).trim()
        }
        if (cleaned.endsWith("=")) {
            cleaned = cleaned.substring(0, cleaned.length - 1).trim()
        }
        if (cleaned.isEmpty()) return null

        val tokens = tokenize(cleaned) ?: return null
        if (tokens.isEmpty()) return null

        if (requireOperator) {
            val hasOperator = tokens.any {
                it is Token.Plus || it is Token.Minus || it is Token.Multiply ||
                        it is Token.Divide || it is Token.Power || it is Token.Percent
            }
            if (!hasOperator) return null
        }

        val parser = Parser(tokens)
        val result = parser.parse() ?: return null

        if (result.isNaN() || result.isInfinite()) return null

        return result
    }

    /**
     * Formats a calculated Double cleanly:
     * - Avoids scientific notation for reasonably sized numbers
     * - Strips trailing zeros for decimals
     * - Formats integers without decimal point
     */
    fun formatResult(value: Double): String {
        if (value.isNaN() || value.isInfinite()) return ""
        val normalized = if (value == -0.0) 0.0 else value

        // If it's an exact integer within Long range
        if (normalized == normalized.toLong().toDouble() && abs(normalized) < 1e15) {
            return normalized.toLong().toString()
        }

        val df = DecimalFormat("0.########", DecimalFormatSymbols(Locale.US)).apply {
            maximumFractionDigits = 8
            isGroupingUsed = false
        }
        return df.format(normalized)
    }

    private fun tokenize(input: String): List<Token>? {
        val tokens = mutableListOf<Token>()
        var i = 0
        val n = input.length

        while (i < n) {
            val c = input[i]
            when {
                c.isWhitespace() -> {
                    i++
                }
                c == '+' -> {
                    tokens.add(Token.Plus)
                    i++
                }
                c == '-' || c == '−' -> {
                    tokens.add(Token.Minus)
                    i++
                }
                c == '*' || c == '×' -> {
                    tokens.add(Token.Multiply)
                    i++
                }
                c == '/' || c == '÷' -> {
                    tokens.add(Token.Divide)
                    i++
                }
                c == '^' -> {
                    tokens.add(Token.Power)
                    i++
                }
                c == '%' -> {
                    tokens.add(Token.Percent)
                    i++
                }
                c == '(' -> {
                    tokens.add(Token.LParen)
                    i++
                }
                c == ')' -> {
                    tokens.add(Token.RParen)
                    i++
                }
                c.isDigit() || c == '.' -> {
                    val start = i
                    var hasDot = false
                    while (i < n && (input[i].isDigit() || input[i] == '.')) {
                        if (input[i] == '.') {
                            if (hasDot) return null // Multiple decimal points
                            hasDot = true
                        }
                        i++
                    }
                    val numStr = input.substring(start, i)
                    if (numStr == ".") return null
                    val value = numStr.toDoubleOrNull() ?: return null
                    tokens.add(Token.Number(value))
                }
                else -> {
                    // Non-math character (regular app search like 'a'..'z', etc.)
                    return null
                }
            }
        }
        return tokens
    }

    private class Parser(private val tokens: List<Token>) {
        private var pos = 0

        fun parse(): Double? {
            val result = parseAdditive() ?: return null
            if (pos != tokens.size) return null // Unconsumed tokens -> invalid syntax
            return result
        }

        private fun peek(): Token? = tokens.getOrNull(pos)
        private fun peekAhead(offset: Int): Token? = tokens.getOrNull(pos + offset)
        private fun consume(): Token? = tokens.getOrNull(pos++)

        // Additive: + and -
        private fun parseAdditive(): Double? {
            var left = parseMultiplicative() ?: return null
            while (pos < tokens.size) {
                val next = peek()
                if (next is Token.Plus) {
                    consume()
                    val right = parseMultiplicative() ?: return null
                    left += right
                } else if (next is Token.Minus) {
                    consume()
                    val right = parseMultiplicative() ?: return null
                    left -= right
                } else {
                    break
                }
            }
            return left
        }

        // Multiplicative: *, /, and binary modulo %
        private fun parseMultiplicative(): Double? {
            var left = parseUnary() ?: return null
            while (pos < tokens.size) {
                val next = peek()
                when (next) {
                    is Token.Multiply -> {
                        consume()
                        val right = parseUnary() ?: return null
                        left *= right
                    }
                    is Token.Divide -> {
                        consume()
                        val right = parseUnary() ?: return null
                        if (abs(right) < 1e-15 || right == 0.0) return null // Division by zero
                        left /= right
                    }
                    is Token.Percent -> {
                        // Binary modulo
                        consume()
                        val right = parseUnary() ?: return null
                        if (abs(right) < 1e-15 || right == 0.0) return null // Modulo by zero
                        left %= right
                    }
                    else -> break
                }
            }
            return left
        }

        // Unary: + and -
        private fun parseUnary(): Double? {
            if (peek() is Token.Plus) {
                consume()
                return parseUnary()
            }
            if (peek() is Token.Minus) {
                consume()
                val operand = parseUnary() ?: return null
                return -operand
            }
            return parseExponential()
        }

        // Exponential: ^ (right-associative)
        private fun parseExponential(): Double? {
            val base = parsePostfix() ?: return null
            if (peek() is Token.Power) {
                consume()
                val exponent = parseUnary() ?: return null
                val result = base.pow(exponent)
                if (result.isNaN() || result.isInfinite()) return null
                return result
            }
            return base
        }

        // Postfix percentage: e.g. 25% or 25% * 80
        private fun parsePostfix(): Double? {
            var value = parsePrimary() ?: return null
            while (peek() is Token.Percent) {
                val next = peekAhead(1)
                // If followed by an operator, closing paren, another percent, or EOF,
                // this % is a postfix percentage (divide by 100).
                val isPostfix = next == null ||
                        next is Token.Plus ||
                        next is Token.Minus ||
                        next is Token.Multiply ||
                        next is Token.Divide ||
                        next is Token.Power ||
                        next is Token.Percent ||
                        next is Token.RParen

                if (isPostfix) {
                    consume()
                    value /= 100.0
                } else {
                    // Next token is a number or '(', so % is binary modulo
                    break
                }
            }
            return value
        }

        // Primary: Number or (expression)
        private fun parsePrimary(): Double? {
            val token = peek() ?: return null
            return when (token) {
                is Token.Number -> {
                    consume()
                    token.value
                }
                is Token.LParen -> {
                    consume()
                    val result = parseAdditive() ?: return null
                    if (peek() !is Token.RParen) return null
                    consume() // Consume ')'
                    result
                }
                else -> null
            }
        }
    }
}
