package org.strah.client;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/** Диалог «Новая заявка» */
class NewClaimDialog extends JDialog {

    private final MainFrame parent;               // <--- ссылка на MainFrame

    NewClaimDialog(MainFrame parent, List<String> policyNums) {
        super(parent, "Новая заявка", true);
        this.parent = parent;

        JComboBox<String> cbPolicy = new JComboBox<>(policyNums.toArray(new String[0]));
        JTextField tfAmount = new JTextField();
        JTextField tfDescr  = new JTextField();

        JPanel grid = new JPanel(new GridLayout(3,2,5,5));
        grid.add(new JLabel("Полис:"));      grid.add(cbPolicy);
        grid.add(new JLabel("Сумма:"));      grid.add(tfAmount);
        grid.add(new JLabel("Описание:"));   grid.add(tfDescr);

        JButton btnSend = new JButton("Отправить");
        btnSend.addActionListener(e -> {
            try {
                double amt = Double.parseDouble(tfAmount.getText());
                String pol = (String) cbPolicy.getSelectedItem();
                String ds  = tfDescr.getText();
                parent.createClaim(pol, amt, ds);
                dispose();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Неверная сумма",
                        "Ошибка", JOptionPane.ERROR_MESSAGE);
            }
        });

        add(grid, BorderLayout.CENTER);
        add(btnSend, BorderLayout.SOUTH);
        setSize(300, 180);
        setLocationRelativeTo(parent);
    }
}
