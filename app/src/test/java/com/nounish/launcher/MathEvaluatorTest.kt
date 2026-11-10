package com.nounish.launcher

import com.nounish.launcher.util.MathEvaluator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class MathEvaluatorTest {

    private val delta = 1e-9

    @Test
    fun testBasicArithmetic() {
        // Addition
        assertEquals(133.0, MathEvaluator.evaluate("125 + 8")!!, delta)
        // Subtraction
        assertEquals(117.0, MathEvaluator.evaluate("125 - 8")!!, delta)
        assertEquals(6.0, MathEvaluator.evaluate("10 − 4")!!, delta) // Unicode minus
        // Multiplication (brief example: 125 * 8)
        assertEquals(1000.0, MathEvaluator.evaluate("125 * 8")!!, delta)
        assertEquals(1000.0, MathEvaluator.evaluate("125 × 8")!!, delta) // Unicode multiply
        // Division (brief example: 45000 / 12)
        assertEquals(3750.0, MathEvaluator.evaluate("45000 / 12")!!, delta)
        assertEquals(3750.0, MathEvaluator.evaluate("45000 ÷ 12")!!, delta) // Unicode divide
    }

    @Test
    fun testParenthesesAndPrecedence() {
        // (15 + 5) * 4 from brief
        assertEquals(80.0, MathEvaluator.evaluate("(15 + 5) * 4")!!, delta)
        assertEquals(80.0, MathEvaluator.evaluate("4 * (15 + 5)")!!, delta)
        assertEquals(35.0, MathEvaluator.evaluate("15 + 5 * 4")!!, delta)
        assertEquals(22.0, MathEvaluator.evaluate("(2 + 3) * 4 + 2")!!, delta)
        assertEquals(30.0, MathEvaluator.evaluate("((2 + 3) * 2) * 3")!!, delta)
    }

    @Test
    fun testPercentageAndModulo() {
        // 25% * 80 from brief
        assertEquals(20.0, MathEvaluator.evaluate("25% * 80")!!, delta)
        assertEquals(20.0, MathEvaluator.evaluate("80 * 25%")!!, delta)
        assertEquals(0.25, MathEvaluator.evaluate("25%")!!, delta)
        assertEquals(100.25, MathEvaluator.evaluate("100 + 25%")!!, delta)
        assertEquals(0.35, MathEvaluator.evaluate("25% + 10%")!!, delta)
        assertEquals(0.2, MathEvaluator.evaluate("(15 + 5)%")!!, delta)

        // Binary modulo
        assertEquals(1.0, MathEvaluator.evaluate("10 % 3")!!, delta)
        assertEquals(2.0, MathEvaluator.evaluate("20 % 6")!!, delta)
        assertEquals(1.0, MathEvaluator.evaluate("10 % (2 + 1)")!!, delta)
        assertEquals(0.0, MathEvaluator.evaluate("12 % 4")!!, delta)
    }

    @Test
    fun testPower() {
        assertEquals(8.0, MathEvaluator.evaluate("2 ^ 3")!!, delta)
        assertEquals(256.0, MathEvaluator.evaluate("2 ^ 8")!!, delta)
        assertEquals(1.0, MathEvaluator.evaluate("5 ^ 0")!!, delta)
        // Right-associativity: 2 ^ (3 ^ 2) = 2 ^ 9 = 512
        assertEquals(512.0, MathEvaluator.evaluate("2 ^ 3 ^ 2")!!, delta)
        assertEquals(0.5, MathEvaluator.evaluate("2 ^ -1")!!, delta)
    }

    @Test
    fun testDecimalsAndWhitespaces() {
        assertEquals(4.0, MathEvaluator.evaluate("1.5 + 2.5")!!, delta)
        assertEquals(2.0, MathEvaluator.evaluate(".5 * 4")!!, delta)
        assertEquals(10.0, MathEvaluator.evaluate("5. * 2")!!, delta)
        assertEquals(1000.0, MathEvaluator.evaluate("   125   *   8   ")!!, delta)
        assertEquals(1000.0, MathEvaluator.evaluate("125*8")!!, delta)
        assertEquals(3750.0, MathEvaluator.evaluate("45000/12")!!, delta)
    }

    @Test
    fun testUnarySigns() {
        assertEquals(3.0, MathEvaluator.evaluate("-5 + 8")!!, delta)
        assertEquals(15.0, MathEvaluator.evaluate("+5 * +3")!!, delta)
        assertEquals(-15.0, MathEvaluator.evaluate("5 * -3")!!, delta)
        assertEquals(-8.0, MathEvaluator.evaluate("-(5 + 3)")!!, delta)
        assertEquals(-9.0, MathEvaluator.evaluate("-3 ^ 2")!!, delta)
        assertEquals(9.0, MathEvaluator.evaluate("(-3) ^ 2")!!, delta)
    }

    @Test
    fun testEqualsPrefixSuffixTolerance() {
        assertEquals(1000.0, MathEvaluator.evaluate("= 125 * 8")!!, delta)
        assertEquals(1000.0, MathEvaluator.evaluate("125 * 8 =")!!, delta)
        assertEquals(3750.0, MathEvaluator.evaluate("= 45000 / 12 =")!!, delta)
        assertNull(MathEvaluator.evaluate("="))
        assertNull(MathEvaluator.evaluate("= ="))
    }

    @Test
    fun testDivisionByZeroSafety() {
        assertNull(MathEvaluator.evaluate("5 / 0"))
        assertNull(MathEvaluator.evaluate("45000 / 0.0"))
        assertNull(MathEvaluator.evaluate("10 / (5 - 5)"))
        assertNull(MathEvaluator.evaluate("0 / 0"))
        assertNull(MathEvaluator.evaluate("5 % 0"))
        assertNull(MathEvaluator.evaluate("10 % (3 - 3)"))
        assertNull(MathEvaluator.evaluate("1 / -0.0"))

        // Zero divided by non-zero is valid
        assertEquals(0.0, MathEvaluator.evaluate("0 / 5")!!, delta)
    }

    @Test
    fun testRegularAppSearchesReturnNull() {
        assertNull(MathEvaluator.evaluate("Chrome"))
        assertNull(MathEvaluator.evaluate("chrome"))
        assertNull(MathEvaluator.evaluate("Settings"))
        assertNull(MathEvaluator.evaluate("calc"))
        assertNull(MathEvaluator.evaluate("calculator"))
        assertNull(MathEvaluator.evaluate("WhatsApp"))
        assertNull(MathEvaluator.evaluate("123 abc"))
        assertNull(MathEvaluator.evaluate("4k video"))
        assertNull(MathEvaluator.evaluate("1Password"))
        assertNull(MathEvaluator.evaluate("f1"))
        assertNull(MathEvaluator.evaluate("youtube"))

        // Pure numbers without operators count as app searches
        assertNull(MathEvaluator.evaluate("2048"))
        assertNull(MathEvaluator.evaluate("125"))
        assertNull(MathEvaluator.evaluate("42"))
        assertNull(MathEvaluator.evaluate("0"))

        // Unless requireOperator is explicitly false
        assertEquals(42.0, MathEvaluator.evaluate("42", requireOperator = false)!!, delta)
        assertEquals(2048.0, MathEvaluator.evaluate("2048", requireOperator = false)!!, delta)
    }

    @Test
    fun testInvalidSyntaxReturnsNull() {
        assertNull(MathEvaluator.evaluate(""))
        assertNull(MathEvaluator.evaluate("   "))
        assertNull(MathEvaluator.evaluate("+"))
        assertNull(MathEvaluator.evaluate("-"))
        assertNull(MathEvaluator.evaluate("*"))
        assertNull(MathEvaluator.evaluate("/"))
        assertNull(MathEvaluator.evaluate("^"))
        assertNull(MathEvaluator.evaluate("125 +"))
        assertNull(MathEvaluator.evaluate("* 125"))
        assertNull(MathEvaluator.evaluate("5 + * 3"))
        assertNull(MathEvaluator.evaluate("5 * / 3"))
        assertNull(MathEvaluator.evaluate("(15 + 5"))
        assertNull(MathEvaluator.evaluate("15 + 5)"))
        assertNull(MathEvaluator.evaluate("((15 + 5)"))
        assertNull(MathEvaluator.evaluate("()"))
        assertNull(MathEvaluator.evaluate("1.2.3"))
        assertNull(MathEvaluator.evaluate("."))
        assertNull(MathEvaluator.evaluate("1..2"))
    }

    @Test
    fun testFormatResult() {
        assertEquals("1000", MathEvaluator.formatResult(1000.0))
        assertEquals("3750", MathEvaluator.formatResult(3750.0))
        assertEquals("80", MathEvaluator.formatResult(80.0))
        assertEquals("20", MathEvaluator.formatResult(20.0))
        assertEquals("0.25", MathEvaluator.formatResult(0.25))
        assertEquals("2.5", MathEvaluator.formatResult(2.5))
        assertEquals("0", MathEvaluator.formatResult(0.0))
        assertEquals("0", MathEvaluator.formatResult(-0.0))
        assertEquals("-15", MathEvaluator.formatResult(-15.0))
        assertEquals("0.3", MathEvaluator.formatResult(0.1 + 0.2))
        assertEquals("0.33333333", MathEvaluator.formatResult(1.0 / 3.0))
        assertEquals("", MathEvaluator.formatResult(Double.NaN))
        assertEquals("", MathEvaluator.formatResult(Double.POSITIVE_INFINITY))
    }
}
