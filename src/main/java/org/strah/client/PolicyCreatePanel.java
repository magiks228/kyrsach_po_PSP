package org.strah.client;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PolicyCreatePanel extends JPanel {

    public static class TypeInfo {
        public final String code;
        public final double baseMin, baseMax, limitMin, limitMax;
        public final int defaultTerm;

        public TypeInfo(String[] parts) {
            this.code = parts[0];
            this.limitMin = Double.parseDouble(parts[2]);
            this.limitMax = Double.parseDouble(parts[3]);
            this.baseMin = Double.parseDouble(parts[4]);
            this.baseMax = Double.parseDouble(parts[5]);
            this.defaultTerm = Integer.parseInt(parts[6]);
        }
    }

    private final MainFrame parent;
    private final List<TypeInfo> types = new ArrayList<>();

    private final JTextField tfNumber = new JTextField(14);
    private final JComboBox<String> cbType;
    private final JTextField tfStart = new JTextField(10);
    private final JTextField tfEnd = new JTextField(10);
    private final JTextField tfPremium = new JTextField(10);
    private final JTextField tfCoverage = new JTextField(10);
    private final JTextField tfMonths = new JTextField(6);
    private final JComboBox<String> cbClient;

    private final DateTimeFormatter uiFmt = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    public PolicyCreatePanel(MainFrame parent, List<String> clientsLogins) {
        super(new GridBagLayout());
        this.parent = parent;

        cbClient = new JComboBox<>(clientsLogins.toArray(new String[0]));

        DefaultComboBoxModel<String> typeModel = new DefaultComboBoxModel<>();
        parent.loadPolicyTypes().forEach(t -> {
            types.add(t);
            typeModel.addElement(t.code);
        });
        cbType = new JComboBox<>(typeModel);

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.anchor = GridBagConstraints.WEST;

        int row = 0;
        addLine(++row, "Номер полиса:", tfNumber, c);
        addLine(++row, "Тип полиса:", cbType, c);
        addLine(++row, "Срок (мес):", tfMonths, c);
        addLine(++row, "Дата начала:", tfStart, c);
        addLine(++row, "Дата окончания:", tfEnd, c);
        addLine(++row, "Покрытие BYN:", tfCoverage, c);
        addLine(++row, "Премия BYN:", tfPremium, c);
        addLine(++row, "Клиент (login):", cbClient, c);

        JButton bCreate = new JButton("Создать полис");
        c.gridy = ++row;
        c.gridx = 0;
        c.gridwidth = 2;
        add(bCreate, c);

        cbType.addActionListener(e -> recalcFromType());
        tfMonths.getDocument().addDocumentListener(dl(e -> recalcDatesAndPremium()));
        tfCoverage.getDocument().addDocumentListener(dl(e -> recalcPremium()));
        bCreate.addActionListener(e -> SwingUtilities.invokeLater(this::onCreate));

        recalcFromType();
    }

    private void addLine(int row, String lbl, Component comp, GridBagConstraints c) {
        c.gridy = row;
        c.gridx = 0;
        c.gridwidth = 1;
        add(new JLabel(lbl), c);
        c.gridx = 1;
        add(comp, c);
    }

    private javax.swing.event.DocumentListener dl(java.util.function.Consumer<javax.swing.event.DocumentEvent> f) {
        return new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { f.accept(e); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { f.accept(e); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { f.accept(e); }
        };
    }

    private void recalcFromType() {
        int idx = cbType.getSelectedIndex();
        if (idx < 0) return;
        TypeInfo t = types.get(idx);

        tfMonths.setText(String.valueOf(t.defaultTerm));
        tfCoverage.setText(String.format(Locale.US, "%.2f", t.limitMin));
        recalcDatesAndPremium();
    }

    private void recalcDatesAndPremium() {
        try {
            int m = Integer.parseInt(tfMonths.getText().trim());
            LocalDate start = LocalDate.now();
            LocalDate end = start.plusMonths(m);
            tfStart.setText(start.format(uiFmt));
            tfEnd.setText(end.format(uiFmt));
        } catch (NumberFormatException ignored) {
        }

        recalcPremium();
    }

    private void recalcPremium() {
        try {
            double cov = Double.parseDouble(tfCoverage.getText().replace(',', '.'));
            int idx = cbType.getSelectedIndex();
            if (idx >= 0) {
                double base = types.get(idx).baseMin;
                double prem = cov * base;
                tfPremium.setText(String.format(Locale.US, "%.2f", prem));
            }
        } catch (NumberFormatException ignored) {
        }
    }

    private void onCreate() {
        if (tfNumber.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Укажите номер полиса");
            return;
        }
        LocalDate start, end;
        try {
            start = LocalDate.parse(tfStart.getText(), uiFmt);
            end = LocalDate.parse(tfEnd.getText(), uiFmt);
        } catch (DateTimeParseException ex) {
            JOptionPane.showMessageDialog(this, "Формат даты должен быть dd-MM-yyyy");
            return;
        }

        double cov, prem;
        try {
            cov = Double.parseDouble(tfCoverage.getText().replace(',', '.'));
            prem = Double.parseDouble(tfPremium.getText().replace(',', '.'));
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Покрытие и премия должны быть числами");
            return;
        }

        String cmd = String.format(Locale.US,
                "NEWPOLICY %s %s %s %s %.2f %.2f %s",
                tfNumber.getText().trim(),
                cbType.getSelectedItem(),
                start.format(uiFmt),
                end.format(uiFmt),
                prem, cov,
                cbClient.getSelectedItem()
        );

        parent.sendCommand(cmd, true);
    }
}
