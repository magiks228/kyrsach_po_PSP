package org.strah.client;

import org.strah.model.types.InsuranceType;
import org.strah.model.types.RiskCoeff;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

class NewAppDialog extends JDialog {

    private final List<InsuranceType> types;
    private final Map<String, List<RiskCoeff>> coeffByType;
    private final Map<String, JComboBox<String>> comboMap = new HashMap<>();
    private final JPanel optionPanel = new JPanel(new GridBagLayout());

    private final JComboBox<String> cbType;
    private final JSpinner spMonths = new JSpinner(new SpinnerNumberModel(6, 1, 36, 1));
    private final JTextField tfCoverage = new JTextField("1000000", 12);

    private final JLabel lblBase   = new JLabel();
    private final JLabel lblLimits = new JLabel();
    private final JLabel lblPrem   = new JLabel("–––");

    NewAppDialog(MainFrame parent) {
        super(parent, "Новая заявка", true);

        // 0. загрузка справочников с сервера
        types       = loadTypes(parent);
        coeffByType = loadCoeffs(parent);

        // 1. комбобокс выбора типа
        cbType = new JComboBox<>(
                types.stream().map(InsuranceType::getCode).toArray(String[]::new)
        );
        cbType.addActionListener(e -> rebuildOptions((String)cbType.getSelectedItem()));

        // 2. верстка диалога
        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4,4,4,4); c.anchor = GridBagConstraints.WEST;

        addRow(c, 0, "Тип полиса:",    cbType);
        addRow(c, 1, "Срок (мес.):",   spMonths);
        addRow(c, 2, "Сумма покрытия:",tfCoverage);
        addRow(c, 3, "Базовая ставка:", lblBase);
        addRow(c, 4, "Лимиты:",         lblLimits);

        c.gridx=0; c.gridy=5; c.gridwidth=2;
        add(optionPanel, c);

        addRow(c, 6, "Премия BYN:", lblPrem);

        JButton btnSend = new JButton("Отправить");
        btnSend.addActionListener(e -> submit(parent));
        c.gridx=0; c.gridy=7; c.gridwidth=2;
        add(btnSend, c);

        // live-пересчёт при изменении полей
        tfCoverage.getDocument().addDocumentListener(new SimpleChange(this::recalc));
        spMonths.addChangeListener(e -> recalc());

        rebuildOptions(cbType.getItemAt(0));
        pack();
        setLocationRelativeTo(parent);
    }

    private List<InsuranceType> loadTypes(MainFrame p) {
        List<InsuranceType> list = new ArrayList<>();
        p.sendSync("INTYPE_LIST", line -> {
            // code nameRu limitMin limitMax baseMin baseMax defTerm franchise%
            String[] v = line.split(" ", 8);
            list.add(new InsuranceType(
                    v[0], v[1],
                    Double.parseDouble(v[2]), Double.parseDouble(v[3]),
                    Double.parseDouble(v[4]), Double.parseDouble(v[5]),
                    Integer.parseInt(v[6]), Double.parseDouble(v[7])
            ));
        });
        return list;
    }

    private Map<String,List<RiskCoeff>> loadCoeffs(MainFrame p) {
        Map<String,List<RiskCoeff>> map = new HashMap<>();
        p.sendSync("INCOEFF_LIST", line -> {
            // typeCode group optCode optName kValue
            String[] v = line.split(" ",5);
            RiskCoeff rc = new RiskCoeff(
                    v[0], v[1], v[2], v[3], Double.parseDouble(v[4])
            );
            map.computeIfAbsent(rc.getTypeCode(), k -> new ArrayList<>()).add(rc);
        });
        return map;
    }

    private void rebuildOptions(String typeCode) {
        optionPanel.removeAll();
        comboMap.clear();

        var byGrp = coeffByType.getOrDefault(typeCode, List.of())
                .stream()
                .collect(Collectors.groupingBy(RiskCoeff::getGroup));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(2,2,2,2); c.anchor = GridBagConstraints.WEST;
        int row = 0;
        for (var e : byGrp.entrySet()) {
            c.gridx=0; c.gridy=row;
            optionPanel.add(new JLabel(e.getKey() + ":"), c);

            JComboBox<String> cb = new JComboBox<>(
                    e.getValue().stream().map(RiskCoeff::getOptionName).toArray(String[]::new)
            );
            comboMap.put(e.getKey(), cb);

            c.gridx=1;
            optionPanel.add(cb, c);
            row++;
        }

        updateMeta();
        optionPanel.revalidate();
        optionPanel.repaint();
    }

    private void updateMeta() {
        InsuranceType it = types.stream()
                .filter(t -> t.getCode().equals(cbType.getSelectedItem()))
                .findFirst().orElseThrow();

        lblBase.setText(String.format("%.4f",
                (it.getBaseRateMin() + it.getBaseRateMax()) / 2.0));
        lblLimits.setText(String.format("%.0f – %.0f",
                it.getLimitMin(), it.getLimitMax()));

        spMonths.setValue(it.getDefaultTerm());
        recalc();
    }

    private void recalc() {
        try {
            InsuranceType it = types.stream()
                    .filter(t -> t.getCode().equals(cbType.getSelectedItem()))
                    .findFirst().orElseThrow();

            double base = (it.getBaseRateMin() + it.getBaseRateMax())/2.0;
            double cov  = Double.parseDouble(tfCoverage.getText().replace(',','.'));

            int    m    = (Integer) spMonths.getValue();
            double kTerm= 1.0;  // позже подтянем с сервера
            double kOpt = 1.0;

            List<RiskCoeff> rcs = coeffByType.get(it.getCode());
            for (var e : comboMap.entrySet()) {
                String grp     = e.getKey();
                String optName = (String)e.getValue().getSelectedItem();
                kOpt *= rcs.stream()
                        .filter(rc -> rc.getGroup().equals(grp)
                                && rc.getOptionName().equals(optName))
                        .map(RiskCoeff::getValue)
                        .findFirst().orElse(1.0);
            }
            double prem = cov * base * kTerm * kOpt;
            lblPrem.setText(String.format("%.2f", prem));
        } catch (Exception ex) {
            lblPrem.setText("–––");
        }
    }

    private void submit(MainFrame p) {
        String type   = (String)cbType.getSelectedItem();
        int    months = (Integer)spMonths.getValue();
        double cov    = Double.parseDouble(tfCoverage.getText().replace(',','.'));

        // 1) NEWAPP → OK <appId>
        StringBuilder resp = new StringBuilder();
        p.sendSync(String.format("NEWAPP %s %d %.2f", type, months, cov), resp::append);
        if (!resp.toString().startsWith("OK")) {
            JOptionPane.showMessageDialog(this, "Сервер: "+resp, "Ошибка", JOptionPane.ERROR_MESSAGE);
            return;
        }
        long appId = Long.parseLong(resp.toString().split(" ")[1]);

        // 2) NEWAPP_ANSWER для каждого коэффициента
        for (var e : comboMap.entrySet()) {
            String grp = e.getKey();
            String opt = e.getValue().getSelectedItem().toString().replace(' ','_');
            p.sendCommand(
                    String.format("NEWAPP_ANSWER %d %s %s", appId, grp, opt),
                    false
            );
        }

        p.refreshApplications();
        dispose();
    }

    private void addRow(GridBagConstraints c, int y, String lbl, JComponent comp) {
        c.gridx=0; c.gridy=y; add(new JLabel(lbl), c);
        c.gridx=1;          add(comp, c);
    }

    private static class SimpleChange implements DocumentListener {
        private final Runnable r;
        SimpleChange(Runnable r) { this.r = r; }
        public void insertUpdate(DocumentEvent e) { r.run(); }
        public void removeUpdate(DocumentEvent e){ r.run(); }
        public void changedUpdate(DocumentEvent e){ r.run(); }
    }
}
