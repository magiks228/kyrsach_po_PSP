package org.strah.client;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Map;

/** Диалог «Новая заявка» */
public class NewClaimDialog extends JDialog {
    private final JComboBox<String> cbPolicy;
    private final JSpinner         spAmount;
    private final JTextArea        taDescr;
    private final MainFrame        parent;

    // вместо списка строк — передаём мапу {policyNumber -> оставшийся лимит}
    public NewClaimDialog(MainFrame parent, Map<String,Double> coverageByPolicy) {
        super(parent, "Новая заявка", true);
        this.parent = parent;

        cbPolicy = new JComboBox<>(coverageByPolicy.keySet().toArray(new String[0]));
        // когда меняется полис — обновляем максимум в спиннере
        cbPolicy.addActionListener(e -> updateSpinnerModel(coverageByPolicy));

        spAmount = new JSpinner();
        updateSpinnerModel(coverageByPolicy);

        taDescr = new JTextArea(3, 20);

        JButton btnSend = new JButton("Отправить");
        btnSend.addActionListener(e -> submit());

        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4,4,4,4); c.anchor = GridBagConstraints.WEST;

        addRow(c, 0, "Полис:",      cbPolicy);
        addRow(c, 1, "Сумма (≤ max):", spAmount);
        addRow(c, 2, "Описание:", new JScrollPane(taDescr));
        c.gridx=0; c.gridy=3; c.gridwidth=2;
        add(btnSend, c);

        pack();
        setLocationRelativeTo(parent);
    }

    private void updateSpinnerModel(Map<String,Double> coverageByPolicy) {
        String sel = (String)cbPolicy.getSelectedItem();
        double max = coverageByPolicy.getOrDefault(sel, 0.0);
        // шаг 1 единица, от 0 до max (округляем вниз)
        SpinnerNumberModel m = new SpinnerNumberModel(0,
                0,
                (long)Math.floor(max),
                1);
        spAmount.setModel(m);
    }

    private void submit() {
        String pol = (String)cbPolicy.getSelectedItem();
        long   amt = ((Number)spAmount.getValue()).longValue();
        String desc = taDescr.getText().replace(' ', '_');

        parent.createClaim(pol, amt, desc);
        dispose();
    }

    private void addRow(GridBagConstraints c,int y,String lbl,JComponent comp){
        c.gridx=0; c.gridy=y; add(new JLabel(lbl),c);
        c.gridx=1;           add(comp,c);
    }
}
