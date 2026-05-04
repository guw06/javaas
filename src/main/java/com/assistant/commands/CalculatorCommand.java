package com.assistant.commands;

import java.util.Locale;

public class CalculatorCommand implements Command {
    @Override
    public String execute(String input) {
        String expression = extractExpression(input);

        if (expression.isEmpty()) {
            return "Укажите выражение. Пример: посчитай 25 * 4 + 10.";
        }

        try {
            double result = evaluate(expression);
            if (result == Math.floor(result) && !Double.isInfinite(result)) {
                return String.format("%s = %d", expression, (long) result);
            }
            return String.format(Locale.ROOT, "%s = %.4f", expression, result).replaceAll("0+$", "").replaceAll("\\.$", "");
        } catch (ArithmeticException e) {
            return "Ошибка: деление на ноль.";
        } catch (Exception e) {
            return "Не удалось вычислить выражение: " + expression + ". Пример: посчитай 2 + 2.";
        }
    }

    private String extractExpression(String input) {
        if (input == null) {
            return "";
        }

        String text = input.toLowerCase(Locale.ROOT)
            .replace(',', '.')
            .replace('ё', 'е');

        String[] prefixes = {
            "посчитай", "вычисли", "рассчитай", "сколько будет", "сколько получится",
            "калькулятор", "calculate", "calc", "считай", "математика"
        };

        for (String prefix : prefixes) {
            int index = text.indexOf(prefix);
            if (index >= 0) {
                text = text.substring(index + prefix.length()).trim();
                break;
            }
        }

        text = text
            .replace("плюс", "+")
            .replace("минус", "-")
            .replace("умножить на", "*")
            .replace("умножь на", "*")
            .replace("умножить", "*")
            .replace("умножь", "*")
            .replace("разделить на", "/")
            .replace("поделить на", "/")
            .replace("делить на", "/")
            .replace("разделить", "/")
            .replace("поделить", "/")
            .replace("делить", "/")
            .replace("в степени", "^")
            .replace("степень", "^")
            .replace("процентов от", "%of")
            .replace("процент от", "%of")
            .replace("процента от", "%of")
            .replace("процентов", "%")
            .replace("процент", "%")
            .replace("x", "*")
            .replace("х", "*");

        text = convertPercentOf(text);
        text = text.replaceAll("(?<=\\d)\\s*%\\s*", "/100");

        return text.replaceAll("[^0-9+\\-*/^().\\s]", "")
            .replaceAll("\\s+", " ")
            .trim();
    }

    private String convertPercentOf(String text) {
        return text.replaceAll("(\\d+(?:\\.\\d+)?)\\s*%of\\s*(\\d+(?:\\.\\d+)?)", "($1/100*$2)");
    }

    private double evaluate(String expression) {
        return new ExpressionParser(expression.replaceAll("\\s+", "")).parse();
    }

    private static class ExpressionParser {
        private final String expression;
        private int position = 0;

        ExpressionParser(String expression) {
            this.expression = expression;
        }

        double parse() {
            double result = parseAddSub();
            if (position < expression.length()) {
                throw new RuntimeException("Unexpected character: " + expression.charAt(position));
            }
            return result;
        }

        private double parseAddSub() {
            double result = parseMulDiv();
            while (position < expression.length()) {
                char operator = expression.charAt(position);
                if (operator != '+' && operator != '-') {
                    break;
                }
                position++;
                double right = parseMulDiv();
                result = operator == '+' ? result + right : result - right;
            }
            return result;
        }

        private double parseMulDiv() {
            double result = parsePower();
            while (position < expression.length()) {
                char operator = expression.charAt(position);
                if (operator != '*' && operator != '/') {
                    break;
                }
                position++;
                double right = parsePower();
                if (operator == '/') {
                    if (right == 0) {
                        throw new ArithmeticException("Division by zero");
                    }
                    result /= right;
                } else {
                    result *= right;
                }
            }
            return result;
        }

        private double parsePower() {
            double result = parseAtom();
            if (position < expression.length() && expression.charAt(position) == '^') {
                position++;
                result = Math.pow(result, parsePower());
            }
            return result;
        }

        private double parseAtom() {
            if (position < expression.length() && expression.charAt(position) == '-') {
                position++;
                return -parseAtom();
            }

            if (position < expression.length() && expression.charAt(position) == '+') {
                position++;
                return parseAtom();
            }

            if (position < expression.length() && expression.charAt(position) == '(') {
                position++;
                double result = parseAddSub();
                if (position < expression.length() && expression.charAt(position) == ')') {
                    position++;
                    return result;
                }
                throw new RuntimeException("Missing closing parenthesis");
            }

            int start = position;
            while (position < expression.length() &&
                (Character.isDigit(expression.charAt(position)) || expression.charAt(position) == '.')) {
                position++;
            }

            if (start == position) {
                throw new RuntimeException("Number expected");
            }

            return Double.parseDouble(expression.substring(start, position));
        }
    }
}
