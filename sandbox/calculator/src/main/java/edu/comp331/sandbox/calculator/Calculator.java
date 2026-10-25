package edu.comp331.sandbox.calculator;

public final class Calculator {
public static double evaluate(String expression) {
if (expression == null || expression.trim().isEmpty()) {
throw new IllegalArgumentException("Invalid expression");
}
Parser parser = new Parser(expression);
double result = parser.parseExpression();
parser.skipSpaces();
if (parser.hasMore()) {
throw new IllegalArgumentException("Invalid expression");
}
return result;
}
private static final class Parser {
    private final String text;
    private int index;

    private Parser(String text) {
        this.text = text;
    }

    private void skipSpaces() {
        while (index < text.length() && Character.isWhitespace(text.charAt(index))) {
            index++;
        }
    }

    private double parseExpression() {
        double value = parseTerm();
        while (true) {
            skipSpaces();
            if (index >= text.length() || text.charAt(index) == ')') {
                return value;
            }
            char op = text.charAt(index);
            if (op != '+' && op != '-') {
                throw new IllegalArgumentException("Invalid expression");
            }
            index++;
            value = op == '+' ? value + parseTerm() : value - parseTerm();
        }
    }

    private double parseTerm() {
        double value = parseFactor();
        while (true) {
            skipSpaces();
            if (index >= text.length() || text.charAt(index) == ')' || text.charAt(index) == '+' || text.charAt(index) == '-') {
                return value;
            }
            char op = text.charAt(index);
            if (op != '*' && op != '/') {
                throw new IllegalArgumentException("Invalid expression");
            }
            index++;
            double rhs = parseFactor();
            if (op == '/' && rhs == 0.0) {
                throw new ArithmeticException("Division by zero");
            }
            value = op == '*' ? value * rhs : value / rhs;
        }
    }

    private double parseFactor() {
        skipSpaces();
        if (index >= text.length()) {
            throw new IllegalArgumentException("Invalid expression");
        }
        char c = text.charAt(index);
        if (c == '+') {
            throw new IllegalArgumentException("Invalid expression");
        }
        if (c == '-') {
            index++;
            return -parseFactor();
        }
        if (c == '(') {
            index++;
            double value = parseExpression();
            skipSpaces();
            if (index >= text.length() || text.charAt(index) != ')') {
                throw new IllegalArgumentException("Invalid expression");
            }
            index++;
            return value;
        }
        if (!Character.isDigit(c) && c != '.') {
            throw new IllegalArgumentException("Invalid expression");
        }
        int start = index;
        while (index < text.length() && (Character.isDigit(text.charAt(index)) || text.charAt(index) == '.')) {
            index++;
        }
        String literal = text.substring(start, index);
        if (!literal.matches("(\\d+\\.?\\d*|\\.\\d+)")) {
            throw new IllegalArgumentException("Invalid expression");
        }
        return Double.parseDouble(literal);
    }

    private boolean hasMore() {
        skipSpaces();
        return index < text.length();
    }
}
}