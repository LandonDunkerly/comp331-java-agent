package edu.comp331.sandbox.calculator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class CalculatorTest {
    private static final double DELTA = 1e-9;

    private static void check(double expected, String expression) {
        assertEquals(expected, Calculator.evaluate(expression), DELTA, expression);
    }

    @Test void addition()                  { check(5, "2+3"); }
    @Test void subtraction()               { check(-1, "2-3"); }
    @Test void multiplication()            { check(12, "3*4"); }
    @Test void divisionGivesFraction()     { check(3.5, "7/2"); }
    @Test void decimalOperands()           { check(3.75, "1.5+2.25"); }
    @Test void whitespaceIgnored()         { check(5, "  2 +   3 "); }
    @Test void singleNumber()              { check(42, "42"); }
    @Test void multiplicationBeforeAddition() { check(14, "2+3*4"); }
    @Test void divisionBeforeSubtraction() { check(8, "10-4/2"); }
    @Test void subtractionIsLeftAssociative() { check(3, "10-4-3"); }
    @Test void divisionIsLeftAssociative() { check(2, "16/4/2"); }
    @Test void parenthesesOverridePrecedence() { check(20, "(2+3)*4"); }
    @Test void nestedParentheses()         { check(21, "((1+2)*(3+4))"); }
    @Test void leadingUnaryMinus()         { check(-3, "-5+2"); }
    @Test void unaryMinusAfterOperator()   { check(-6, "2*-3"); }

    @Test void negatedParenthesesGroup()   { check(-5, "-(2 + 3)"); }
    @Test void subtractNegativeOperand()   { check(2, "1 - -1"); }
    @Test void parenthesizedNegative()     { check(-5, "(-5)"); }
    @Test void negativeZeroEqualsZero()    { check(0, "-0"); }
    @Test void floatingPointWithinTolerance() { check(0.3, "0.1 + 0.2"); }
    @Test void doubleMinusIsDoubleNegation() { check(1, "--1"); }
    @Test void zeroDividendGivesZero()     { check(0, "0/5"); }
    @Test void unaryMinusBindsBeforeMultiplication() { check(-10, "-(2+3)*2"); }


    @Test void divisionByZeroRejected() {
        assertThrows(ArithmeticException.class, () -> Calculator.evaluate("1/0"));
    }

    @Test void divisionByZeroResultRejected() {
        assertThrows(ArithmeticException.class, () -> Calculator.evaluate("5/(2-2)"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "2+", "*3", "2 3", "abc", "2+x", "(2+3", "2+3)", "()", "2**3", "1.2.3", "+", "-", "*", "2 + + 3", ".", "1.2e3", "2 - +3"})
    void invalidInputRejected(String expression) {
        assertThrows(IllegalArgumentException.class, () -> Calculator.evaluate(expression));
    }
}