package org.strah.client;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

/** Диалог «Новая заявка‑выплата» */
public class NewClaimDialog extends JDialog {

    /* поля инициализируем сразу → компилятор доволен */
    private final JComboBox<String> cbPolicy;
    private final JSpinner          spAmount = new JSpinner();
    private final JTextArea         taDescr  = new JTextArea(3, 20);
    private final MainFrame         parent;

    // --------------------------------------------------------------------
    public NewClaimDialog(MainFrame parent,
                          Map<String, Double> coverageByPolicy) {
        super(parent, "Новая заявка‑выплата", true);
        this.parent = parent;

        /* ---------- комбобокс полисов ---------- */
        cbPolicy = new JComboBox<>(coverageByPolicy.keySet().toArray(new String[0]));
        if (cbPolicy.getItemCount() > 0) {
            cbPolicy.setSelectedIndex(0);
        }

        /* ---------- если полисов нет ---------- */
        if (coverageByPolicy.isEmpty()) {
            JOptionPane.showMessageDialog(
                    parent,
                    "У вас нет активных полисов с доступным покрытием.",
                    "Нет полисов",
                    JOptionPane.INFORMATION_MESSAGE
            );
            dispose();
            return;
        }

        /* ---------- слушатель + инициализация спиннера ---------- */
        cbPolicy.addActionListener(e -> updateSpinnerModel(coverageByPolicy));
        updateSpinnerModel(coverageByPolicy);

        // оформление спиннера
        JSpinner.NumberEditor editor =
                new JSpinner.NumberEditor(spAmount, "#,##0.00");
        spAmount.setEditor(editor);
        Dimension d = spAmount.getPreferredSize();
        spAmount.setPreferredSize(new Dimension(120, d.height));

        /* ---------- кнопка ---------- */
        JButton btnSend = new JButton("Отправить");
        btnSend.addActionListener(e -> submit());

        /* ---------- вёрстка ---------- */
        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets  = new Insets(4, 4, 4, 4);
        c.anchor  = GridBagConstraints.WEST;

        addRow(c, 0, "Полис:",           cbPolicy);
        addRow(c, 1, "Сумма (≤ max):",   spAmount);
        addRow(c, 2, "Описание:",        new JScrollPane(taDescr));
        c.gridx = 0; c.gridy = 3; c.gridwidth = 2;
        add(btnSend, c);

        pack();
        setLocationRelativeTo(parent);
    }

    // --------------------------------------------------------------------
    /** Подстраиваем диапазон спиннера под выбранный полис */
    private void updateSpinnerModel(Map<String, Double> coverageByPolicy) {
        String sel = (String) cbPolicy.getSelectedItem();
        if (sel == null) return;

        double max = coverageByPolicy.getOrDefault(sel, 0.0);

        if (max < 0.01) {                      // лимит исчерпан
            spAmount.setModel(new SpinnerNumberModel(0.0, 0.0, 0.0, 0.0));
            spAmount.setEnabled(false);
            return;
        }

        double step = 1.0;
        double init = Math.min(step, max);
        spAmount.setModel(new SpinnerNumberModel(init, 0.01, max, step));
        spAmount.setEnabled(true);
    }

    // --------------------------------------------------------------------
    private void submit() {
        String pol  = (String) cbPolicy.getSelectedItem();
        double amt  = ((Number) spAmount.getValue()).doubleValue();
        String desc = taDescr.getText().trim().replace(' ', '_');

        parent.createClaim(pol, amt, desc);
        dispose();
    }

    // --------------------------------------------------------------------
    private void addRow(GridBagConstraints c, int y,
                        String lbl, JComponent comp) {
        c.gridx = 0; c.gridy = y; c.gridwidth = 1;
        add(new JLabel(lbl), c);
        c.gridx = 1;
        add(comp, c);
    }
}
