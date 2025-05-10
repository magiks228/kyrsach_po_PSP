package org.strah.client;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

class PolicyCreatePanel extends JPanel {

    private static final DateTimeFormatter DF =
            DateTimeFormatter.ofPattern("dd-MM-yyyy");

    PolicyCreatePanel(MainFrame parent, List<String> clients) {
        super(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.anchor = GridBagConstraints.WEST;

        JTextField tfNum   = new JTextField(15);

        JComboBox<String> cbType = new JComboBox<>(new String[]{
                "PROP   - Property",
                "BI     - Business Interruption",
                "GL     - Liability",
                "E&O    - Errors & Omissions",
                "PROD   - Product Liability",
                "CREDIT - Financial",
                "CYBER  - Cyber",
                "CARGO  - Cargo",
                "D&O    - D&O"
        });

        JTextField tfStart = new JTextField(LocalDate.now().format(DF), 10);
        JTextField tfEnd   = new JTextField(
                LocalDate.now().plusMonths(6).format(DF), 10);

        JSpinner  spPrem   = new JSpinner(
                new SpinnerNumberModel(1000.0, 0, 1e9, 100));

        JComboBox<String> cbClient =
                new JComboBox<>(clients.toArray(new String[0]));

        addRow(c, 0, "Номер полиса:", tfNum);
        addRow(c, 1, "Тип:",           cbType);
        addRow(c, 2, "Начало (dd-MM-yyyy):", tfStart);
        addRow(c, 3, "Окончание:",     tfEnd);
        addRow(c, 4, "Премия:",        spPrem);
        addRow(c, 5, "Клиент:",        cbClient);

        JButton btnSave = new JButton("Сохранить");
        btnSave.addActionListener(e -> {

            String rawType  = (String) cbType.getSelectedItem(); // "CREDIT - Financial"
            String typeCode = rawType.split(" ")[0];             // "CREDIT"

            String cmd = String.format("NEWPOLICY %s %s %s %s %s %s",
                    tfNum.getText().trim(),
                    typeCode,
                    tfStart.getText().trim(),
                    tfEnd.getText().trim(),
                    spPrem.getValue(),
                    cbClient.getSelectedItem());

            parent.sendCommand(cmd, true);      // true → «обновить таблицу»
            clear(tfNum, tfStart, tfEnd, spPrem);
        });

        c.gridx = 0; c.gridy = 6; c.gridwidth = 2;
        add(btnSave, c);
    }

    /* util */
    private void addRow(GridBagConstraints c, int y, String lbl, JComponent comp){
        c.gridx = 0; c.gridy = y; c.gridwidth = 1; add(new JLabel(lbl), c);
        c.gridx = 1;             add(comp, c);
    }
    private void clear(JTextField n,JTextField s,JTextField e,JSpinner p){
        n.setText("");
        s.setText(LocalDate.now().format(DF));
        e.setText("");
        p.setValue(0);
    }
}
