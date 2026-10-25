package edu.comp331.sandbox.calculator;

public final class Calculator {
public static double evaluate(String expression) {
if (expression == null || expression.trim().isEmpty()) {
throw new IllegalArgumentException("Invalid expression");
}
    String[] tokens = expression.replace(" ", "").split("(?=[+\\-*/()])|(?<=[+\\-*/()])");
    double result = Double.parseDouble(tokens[0]);

    for (int i = 1; i < tokens.length; i++) {
        String token = tokens[i];
        if (token.isEmpty()) continue;
        if (token.equals("+") || token.equals("-") || token.equals("*") || token.equals("/")) {
            double value = Double.parseDouble(tokens[++i]);
            switch (token) {
                case "+": result += value; break;
                case "-": result -= value; break;
                case "*": result *= value; break;
                case "/": result /= value; break;
                default: throw new IllegalArgumentException("Invalid operator");
            }
        } else {
            result = Double.parseDouble(token);
        }
    }

    return result;
}
}