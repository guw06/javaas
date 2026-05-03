package com.assistant.commands;

/**
 * Команда "посчитай" — простой калькулятор для математических выражений.
 * Поддерживает: +, -, *, / и скобки через рекурсивный парсер.
 */
public class CalculatorCommand implements Command {
    @Override
    public String execute(String input) {
        // Извлекаем математическое выражение из текста
        String expression = extractExpression(input);
        
        if (expression.isEmpty()) {
            return "🔢 Укажите выражение. Пример: 'посчитай 25 * 4 + 10'";
        }
        
        try {
            double result = evaluate(expression);
            
            // Если результат целое число, показываем без десятичных
            if (result == Math.floor(result) && !Double.isInfinite(result)) {
                return String.format("🔢 %s = %d", expression, (long) result);
            } else {
                return String.format("🔢 %s = %.2f", expression, result);
            }
        } catch (ArithmeticException e) {
            return "❌ Ошибка: деление на ноль";
        } catch (Exception e) {
            return "❌ Не удалось вычислить выражение: " + expression + 
                   "\nПример: 'посчитай 2 + 2' или 'сколько будет 100 / 5'";
        }
    }
    
    /**
     * Извлекает математическое выражение из пользовательского ввода
     */
    private String extractExpression(String input) {
        String lower = input.toLowerCase();
        
        // Убираем известные префиксы
        String[] prefixes = {
            "посчитай", "вычисли", "калькулятор", "сколько будет",
            "calculate", "calc", "считай", "рассчитай", "посчитать", "математика"
        };
        
        for (String prefix : prefixes) {
            int idx = lower.indexOf(prefix);
            if (idx != -1) {
                lower = lower.substring(idx + prefix.length()).trim();
                break;
            }
        }
        
        // Заменяем русские слова на операторы
        lower = lower
            .replace("плюс", "+")
            .replace("минус", "-")
            .replace("умножить на", "*")
            .replace("умножить", "*")
            .replace("делить на", "/")
            .replace("делить", "/")
            .replace("разделить на", "/")
            .replace("разделить", "/")
            .replace("на", "*")
            .replace("х", "*")
            .replace("x", "*");
        
        // Оставляем только цифры и операторы
        return lower.replaceAll("[^0-9+\\-*/().\\s]", "").replaceAll("\\s+", " ").trim();
    }
    
    /**
     * Рекурсивный парсер математических выражений.
     * Поддерживает: +, -, *, /, скобки, пробелы.
     */
    private double evaluate(String expression) {
        expression = expression.replaceAll("\\s+", "");
        return new ExpressionParser(expression).parse();
    }
    
    /**
     * Простой рекурсивный парсер выражений
     */
    private static class ExpressionParser {
        private final String expr;
        private int pos = 0;
        
        ExpressionParser(String expr) {
            this.expr = expr;
        }
        
        double parse() {
            double result = parseAddSub();
            if (pos < expr.length()) {
                throw new RuntimeException("Неожиданный символ: " + expr.charAt(pos));
            }
            return result;
        }
        
        private double parseAddSub() {
            double result = parseMulDiv();
            while (pos < expr.length() && (expr.charAt(pos) == '+' || expr.charAt(pos) == '-')) {
                char op = expr.charAt(pos++);
                double right = parseMulDiv();
                result = op == '+' ? result + right : result - right;
            }
            return result;
        }
        
        private double parseMulDiv() {
            double result = parseAtom();
            while (pos < expr.length() && (expr.charAt(pos) == '*' || expr.charAt(pos) == '/')) {
                char op = expr.charAt(pos++);
                double right = parseAtom();
                if (op == '/') {
                    if (right == 0) throw new ArithmeticException("Деление на ноль");
                    result /= right;
                } else {
                    result *= right;
                }
            }
            return result;
        }
        
        private double parseAtom() {
            // Пропускаем пробелы
            while (pos < expr.length() && expr.charAt(pos) == ' ') pos++;
            
            // Унарный минус
            if (pos < expr.length() && expr.charAt(pos) == '-') {
                pos++;
                return -parseAtom();
            }
            
            // Скобки
            if (pos < expr.length() && expr.charAt(pos) == '(') {
                pos++; // пропускаем '('
                double result = parseAddSub();
                if (pos < expr.length() && expr.charAt(pos) == ')') {
                    pos++; // пропускаем ')'
                }
                return result;
            }
            
            // Число
            int start = pos;
            while (pos < expr.length() && (Character.isDigit(expr.charAt(pos)) || expr.charAt(pos) == '.')) {
                pos++;
            }
            if (start == pos) {
                throw new RuntimeException("Ожидалось число");
            }
            return Double.parseDouble(expr.substring(start, pos));
        }
    }
}
