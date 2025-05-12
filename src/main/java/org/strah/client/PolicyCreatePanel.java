package org.strah.client;

import org.strah.model.types.InsuranceType;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;

public class PolicyCreatePanel extends JPanel {

    private final MainFrame parent;
    private final JTextField tfNumber    = new JTextField(12);
    private final JComboBox<String> cbType;
    private final JTextField tfStart     = new JTextField(10);
    private final JTextField tfEnd       = new JTextField(10);
    private final JTextField tfPremium   = new JTextField("0.0", 10);
    private final JTextField tfCoverage  = new JTextField("0.0", 10);
    private final JComboBox<String> cbClient;

    public PolicyCreatePanel(MainFrame parent, List<String> clients) {
        super(new GridBagLayout());
        this.parent = parent;

        // Загружаем коды типов из сервера
        DefaultComboBoxModel<String> typeModel = new DefaultComboBoxModel<>();
        parent.sendSync("INTYPE_LIST", line -> {
            String[] parts = line.split(" ");
            // формат: code nameRu baseMin baseMax limitMin limitMax defaultTerm franchisePct
            typeModel.addElement(parts[0]);
        });
        cbType = new JComboBox<>(typeModel);

        cbClient = new JComboBox<>(clients.toArray(new String[0]));

        GridBagConstraints c = new GridBagConstraints();
        c.insets  = new Insets(4,4,4,4);
        c.anchor  = GridBagConstraints.WEST;
        c.gridx   = 0;
        c.gridy   = 0;

        add(new JLabel("Номер полиса:"), c);
        c.gridx = 1; add(tfNumber, c);

        c.gridy++; c.gridx = 0;
        add(new JLabel("Тип полиса:"), c);
        c.gridx = 1; add(cbType, c);

        c.gridy++; c.gridx = 0;
        add(new JLabel("Дата начала (гггг-MM-дд):"), c);
        c.gridx = 1; add(tfStart, c);

        c.gridy++; c.gridx = 0;
        add(new JLabel("Дата окончания:"), c);
        c.gridx = 1; add(tfEnd, c);

        c.gridy++; c.gridx = 0;
        add(new JLabel("Премия BYN:"), c);
        c.gridx = 1; add(tfPremium, c);

        c.gridy++; c.gridx = 0;
        add(new JLabel("Сумма покрытия BYN:"), c);
        c.gridx = 1; add(tfCoverage, c);

        c.gridy++; c.gridx = 0;
        add(new JLabel("Клиент (login):"), c);
        c.gridx = 1; add(cbClient, c);

        c.gridy++; c.gridx = 0; c.gridwidth = 2;
        JButton bCreate = new JButton("Создать полис");
        add(bCreate, c);

        bCreate.addActionListener(e -> onCreate());
    }

    private void onCreate() {
        String num       = tfNumber.getText().trim();
        String typeCode  = (String) cbType.getSelectedItem();
        String startStr  = tfStart.getText().trim();
        String endStr    = tfEnd.getText().trim();
        double premium, coverage;
        try {
            premium  = Double.parseDouble(tfPremium.getText().replace(',', '.'));
            coverage = Double.parseDouble(tfCoverage.getText().replace(',', '.'));
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(
                    this,
                    "Премия и покрытие должны быть числами",
                    "Ошибка ввода",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        // Проверяем дату
        try {
            LocalDate.parse(startStr);
            LocalDate.parse(endStr);
        } catch (DateTimeParseException ex) {
            JOptionPane.showMessageDialog(
                    this,
                    "Неверный формат даты, ожидается YYYY-MM-DD",
                    "Ошибка ввода",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        String client = (String) cbClient.getSelectedItem();
        if (client == null) {
            JOptionPane.showMessageDialog(
                    this,
                    "Выберите клиента",
                    "Ошибка ввода",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        // Формируем команду: NEWPOLICY num typeCode start end premium coverage client
        String cmd = String.format(Locale.US,
                "NEWPOLICY %s %s %s %s %.2f %.2f %s",
                num, typeCode, startStr, endStr, premium, coverage, client
        );

        // Отправляем и сразу обновляем список
        parent.sendCommand(cmd, true);
    }
}
