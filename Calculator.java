import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.text.DecimalFormat;

public class Calculator extends JFrame implements ActionListener {

    private final JTextField display;
    private final JTextField expression;

    private String operator = "";
    private double first = 0;
    private boolean startNewNumber = true; // when true, next digit replaces display (after operator or =)
    private final DecimalFormat fmt = new DecimalFormat("0.##########"); // trim trailing zeros

    public Calculator() {
        // Top expression line
        expression = new JTextField();
        expression.setEditable(false);
        expression.setHorizontalAlignment(JTextField.RIGHT);
        expression.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        expression.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        expression.setBackground(new Color(240, 240, 240));

        // Main display
        display = new JTextField("0");
        display.setEditable(false);
        display.setHorizontalAlignment(JTextField.RIGHT);
        display.setFont(new Font("Segoe UI", Font.BOLD, 28));
        display.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        display.setBackground(Color.WHITE);

        // Top utility buttons (Clear + Back)
        JPanel topButtons = new JPanel(new GridLayout(1, 2, 8, 0));
        topButtons.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
        topButtons.setBackground(new Color(230, 230, 230));

        // Clear button
        JButton clear = new JButton("C");
        styleButton(clear);
        clear.setBackground(new Color(255, 140, 140));
        clear.setFont(new Font("Segoe UI", Font.BOLD, 20));
        clear.addActionListener(e -> clearAll());

        // Backspace button
        JButton back = new JButton("Back");
        styleButton(back);
        back.setFont(new Font("Segoe UI", Font.BOLD, 18));
        back.setBackground(new Color(220, 220, 220));
        back.addActionListener(e -> backspace());

        topButtons.add(clear);
        topButtons.add(back);

        // Main buttons (added +/- and %)
        JPanel panel = new JPanel(new GridLayout(5, 4, 8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.setBackground(new Color(230, 230, 230));

        String[] buttons = {
            "%", "±", "←", "/",
            "7", "8", "9", "*",
            "4", "5", "6", "-",
            "1", "2", "3", "+",
            "0", "0", ".", "=" // '0' twice to make 2-column wide zero visually (we handle in layout appearance)
        };

        // We'll create custom buttons for the special three: percent, plusminus, leftarrow (redundant with Back but present)
        for (String text : buttons) {
            JButton btn = new JButton(text);
            styleButton(btn);
            if ("%±←/ * - + =".contains(text)) {
                btn.setBackground(new Color(210, 210, 210));
            }
            // special mapping: treat '←' as backspace
            if (text.equals("←")) {
                btn.addActionListener(e -> backspace());
            } else {
                btn.addActionListener(this);
            }
            panel.add(btn);
        }

        // Build north (expression + display + top buttons)
        JPanel north = new JPanel(new GridLayout(3, 1));
        north.add(expression);
        north.add(display);
        north.add(topButtons);

        add(north, BorderLayout.NORTH);
        add(panel, BorderLayout.CENTER);

        // Key bindings for keyboard input
        setupKeyBindings();

        setTitle("Windows Style Calculator");
        setSize(360, 560);
        setResizable(false);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
    }

    private void setupKeyBindings() {
        // Use key bindings on the root pane for better focus handling
        JRootPane root = getRootPane();
        InputMap im = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = root.getActionMap();

        String keys = "0123456789.+-*/=%\n\b\177"; // added newline for Enter, \b backspace, \177 delete
        // digits & dot
        for (char c = '0'; c <= '9'; c++) {
            final String key = String.valueOf(c);
            im.put(KeyStroke.getKeyStroke(c), "digit" + c);
            am.put("digit" + c, new AbstractAction() {
                public void actionPerformed(ActionEvent e) { numberInput(key); }
            });
        }
        im.put(KeyStroke.getKeyStroke('.'), "dot");
        am.put("dot", new AbstractAction() { public void actionPerformed(ActionEvent e) { numberInput("."); } });

        // operators
        String[] ops = {"+", "-", "*", "/"};
        for (String op : ops) {
            im.put(KeyStroke.getKeyStroke(op.charAt(0)), "op" + op);
            am.put("op" + op, new AbstractAction() { public void actionPerformed(ActionEvent e) { operatorInput(op); }});
        }

        // Enter or '='
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "equals");
        im.put(KeyStroke.getKeyStroke('='), "equals");
        am.put("equals", new AbstractAction() { public void actionPerformed(ActionEvent e) { equalAction(); }});

        // Backspace
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), "back");
        am.put("back", new AbstractAction() { public void actionPerformed(ActionEvent e) { backspace(); }});

        // Delete -> clear
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "clear");
        am.put("clear", new AbstractAction() { public void actionPerformed(ActionEvent e) { clearAll(); }});

        // percent
        im.put(KeyStroke.getKeyStroke('%'), "percent");
        am.put("percent", new AbstractAction() { public void actionPerformed(ActionEvent e) { percentAction(); }});

        // plusminus (use 'n' as toggle key)
        im.put(KeyStroke.getKeyStroke('n'), "plusminus");
        am.put("plusminus", new AbstractAction() { public void actionPerformed(ActionEvent e) { plusMinusAction(); }});
    }

    // Style for all buttons
    private void styleButton(JButton b) {
        b.setFont(new Font("Segoe UI", Font.PLAIN, 20));
        b.setBackground(Color.WHITE);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
    }

    // Backspace / Delete last character
    private void backspace() {
        String txt = display.getText();
        if (txt.equals("Error") || startNewNumber) {
            display.setText("0");
            startNewNumber = true;
            return;
        }
        if (!txt.isEmpty() && !(txt.equals("0"))) {
            txt = txt.substring(0, txt.length() - 1);
            if (txt.isEmpty() || txt.equals("-")) {
                display.setText("0");
                startNewNumber = true;
            } else {
                display.setText(txt);
            }
        } else {
            display.setText("0");
            startNewNumber = true;
        }
    }

    // Clear all state
    private void clearAll() {
        display.setText("0");
        expression.setText("");
        first = 0;
        operator = "";
        startNewNumber = true;
    }

    // Handle button actions
    public void actionPerformed(ActionEvent e) {
        String input = e.getActionCommand();

        if ("0123456789.".contains(input)) {
            numberInput(input);
            return;
        }

        if (input.equals("=")) {
            equalAction();
            return;
        }

        if (input.equals("%")) {
            percentAction();
            return;
        }

        if (input.equals("±")) {
            plusMinusAction();
            return;
        }

        // If it's an operator like + - * /
        if ("+-*/".contains(input)) {
            operatorInput(input);
        }
    }

    // Called for numeric/dot inputs (from both buttons and key bindings)
    private void numberInput(String input) {
        if (display.getText().equals("Error")) {
            display.setText("0");
            startNewNumber = true;
        }

        if (startNewNumber) {
            // start fresh
            if (input.equals(".")) {
                display.setText("0.");
                startNewNumber = false;
            } else {
                display.setText(input);
                startNewNumber = false;
            }
        } else {
            // append
            if (input.equals(".") && display.getText().contains(".")) return;
            // prevent leading zero multi-digit like 00
            if (display.getText().equals("0") && !input.equals(".")) {
                display.setText(input);
            } else {
                display.setText(display.getText() + input);
            }
        }
    }

    private void operatorInput(String op) {
        try {
            double current = Double.parseDouble(display.getText());
            if (!operator.isEmpty() && !startNewNumber) {
                // chain: compute previous first operator current -> become new first
                first = compute(first, current, operator);
                display.setText(fmt.format(first));
            } else {
                first = current;
            }
            operator = op;
            expression.setText(fmt.format(first) + " " + operator);
            startNewNumber = true;
        } catch (Exception ex) {
            display.setText("Error");
            operator = "";
            startNewNumber = true;
        }
    }

    private void equalAction() {
        if (operator.isEmpty()) return;
        if (display.getText().isEmpty()) return;
        try {
            double second = Double.parseDouble(display.getText());
            double result = compute(first, second, operator);
            expression.setText(fmt.format(first) + " " + operator + " " + fmt.format(second) + " =");
            display.setText(fmt.format(result));
            operator = "";
            first = result; // allow further chaining from result
            startNewNumber = true;
        } catch (ArithmeticException ae) {
            display.setText("Error");
            operator = "";
            startNewNumber = true;
        } catch (Exception ex) {
            display.setText("Error");
            operator = "";
            startNewNumber = true;
        }
    }

    private double compute(double a, double b, String op) {
        switch (op) {
            case "+": return a + b;
            case "-": return a - b;
            case "*": return a * b;
            case "/":
                if (b == 0) throw new ArithmeticException("divide by zero");
                return a / b;
            default: return b;
        }
    }

    private void percentAction() {
        try {
            double cur = Double.parseDouble(display.getText());
            cur = cur / 100.0;
            display.setText(fmt.format(cur));
            startNewNumber = true;
        } catch (Exception e) {
            display.setText("Error");
            startNewNumber = true;
        }
    }

    private void plusMinusAction() {
        try {
            double cur = Double.parseDouble(display.getText());
            cur = -cur;
            display.setText(fmt.format(cur));
        } catch (Exception e) {
            display.setText("Error");
        }
    }

    public static void main(String[] args) {
        // Swing look and feel on EDT
        SwingUtilities.invokeLater(() -> new Calculator());
    }
}
