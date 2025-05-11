package org.strah.client;

import org.strah.model.types.InsuranceType;
import org.strah.model.types.RiskCoeff;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class NewAppDialog extends JDialog {

    private static final String SEP = "\u001F";

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

    private final JButton btnSend = new JButton("Отправить");

    public NewAppDialog(MainFrame parent) {
        super(parent, "Новая заявка", true);

        // 0. Загрузка справочников
        types       = loadTypes(parent);
        coeffByType = loadCoeffs(parent);

        // 1. ComboBox для выбора типа
        cbType = new JComboBox<>(
                types.stream().map(InsuranceType::getCode).toArray(String[]::new)
        );
        cbType.addActionListener(e -> rebuildOptions((String) cbType.getSelectedItem()));

        // 2. Верстка
        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4,4,4,4);
        c.anchor = GridBagConstraints.WEST;

        addRow(c, 0, "Тип полиса:",    cbType);
        addRow(c, 1, "Срок (мес.):",   spMonths);
        addRow(c, 2, "Сумма покрытия:", tfCoverage);
        addRow(c, 3, "Базовая ставка:", lblBase);
        addRow(c, 4, "Лимиты BYN:",     lblLimits);

        c.gridx = 0; c.gridy = 5; c.gridwidth = 2;
        add(optionPanel, c);

        addRow(c, 6, "Премия BYN:", lblPrem);

        // 3. Кнопка «Отправить»
        btnSend.setEnabled(false);
        btnSend.addActionListener(e -> submit(parent));
        c.gridx = 0; c.gridy = 7; c.gridwidth = 2;
        add(btnSend, c);

        // Live-пересчёт и валидация
        tfCoverage.getDocument().addDocumentListener(new SimpleChange(this::validateAndRecalc));
        spMonths.addChangeListener(e -> validateAndRecalc());

        // Инициализация полей
        rebuildOptions((String) cbType.getItemAt(0));

        pack();
        setLocationRelativeTo(parent);
    }

    private List<InsuranceType> loadTypes(MainFrame p) {
        List<InsuranceType> list = new ArrayList<>();
        p.sendSync("INTYPE_LIST", line -> {
            // делим по SEP, ожидаем 8 полей
            String[] v = line.split(SEP, 8);
            list.add(new InsuranceType(
                    v[0],              // code
                    v[1].replace('_',' '), // nameRu
                    Double.parseDouble(v[2]),
                    Double.parseDouble(v[3]),
                    Double.parseDouble(v[4]),
                    Double.parseDouble(v[5]),
                    Integer.parseInt(v[6]),
                    Double.parseDouble(v[7])
            ));
        });
        return list;
    }

    private Map<String,List<RiskCoeff>> loadCoeffs(MainFrame p) {
        Map<String,List<RiskCoeff>> map = new HashMap<>();
        p.sendSync("INCOEFF_LIST", line -> {
            // делим по SEP, ожидаем 5 полей
            String[] v = line.split(SEP, 5);
            RiskCoeff rc = new RiskCoeff(
                    v[0],               // typeCode
                    v[1],               // group
                    v[2],               // optionCode
                    v[3].replace('_',' '), // optionName
                    Double.parseDouble(v[4])
            );
            map.computeIfAbsent(rc.getTypeCode(), k -> new ArrayList<>()).add(rc);
        });
        return map;
    }

    private void rebuildOptions(String typeCode) {
        optionPanel.removeAll();
        comboMap.clear();

        Map<String,List<RiskCoeff>> byGrp = coeffByType.getOrDefault(typeCode, List.of())
                .stream().collect(Collectors.groupingBy(RiskCoeff::getCoeffGroup));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(2,2,2,2);
        c.anchor = GridBagConstraints.WEST;

        int row = 0;
        for (Map.Entry<String,List<RiskCoeff>> e : byGrp.entrySet()) {
            c.gridx = 0; c.gridy = row;
            optionPanel.add(new JLabel(e.getKey() + ":"), c);

            JComboBox<String> cb = new JComboBox<>(
                    e.getValue().stream().map(RiskCoeff::getOptionName).toArray(String[]::new)
            );
            comboMap.put(e.getKey(), cb);

            c.gridx = 1;
            optionPanel.add(cb, c);
            row++;
        }

        updateMeta();
        optionPanel.revalidate();
        optionPanel.repaint();
        pack();
    }

    private void updateMeta() {
        String selectedCode = (String) cbType.getSelectedItem();
        // Ищем тип по коду
        InsuranceType it = types.stream()
                .filter(t -> t.getCode().equals(selectedCode))
                .findFirst()
                .orElse(null);

        if (it == null) {
            // Если не нашли — отключаем кнопку и сбрасываем подписи
            btnSend.setEnabled(false);
            lblBase.setText("—");
            lblLimits.setText("—");
            lblPrem.setText("—");
            return;
        }

        // Нашли — показываем данные
        double avgBase = (it.getBaseRateMin() + it.getBaseRateMax()) / 2.0;
        lblBase.setText(String.format("%.4f", avgBase));
        lblLimits.setText(String.format("%.0f – %.0f",
                it.getLimitMin(), it.getLimitMax()));

        // Подставляем дефолтный срок и сразу проверяем/пересчитываем
        spMonths.setValue(it.getDefaultTerm());
        validateAndRecalc();
    }


    private void validateAndRecalc() {
        InsuranceType it;
        try {
            it = types.stream()
                    .filter(t -> t.getCode().equals(cbType.getSelectedItem()))
                    .findFirst().orElseThrow();
        } catch (Exception ex) {
            btnSend.setEnabled(false);
            lblPrem.setText("–––");
            return;
        }

        double cov;
        try {
            cov = Double.parseDouble(tfCoverage.getText().replace(',', '.'));
        } catch (NumberFormatException ex) {
            btnSend.setEnabled(false);
            lblPrem.setText("Неверный формат");
            return;
        }

        double min = it.getLimitMin(), max = it.getLimitMax();
        if (cov < min || cov > max) {
            btnSend.setEnabled(false);
            lblPrem.setText(String.format("Допустимо: %.0f–%.0f", min, max));
            return;
        }

        btnSend.setEnabled(true);
        recalcPremium(it, cov);
    }

    private void recalcPremium(InsuranceType it, double cov) {
        double base = (it.getBaseRateMin() + it.getBaseRateMax()) / 2.0;
        double kTerm = 1.0; // пока заглушка
        double kOpt  = comboMap.entrySet().stream()
                .mapToDouble(e -> {
                    String grp = e.getKey();
                    String sel = (String)e.getValue().getSelectedItem();
                    return coeffByType.get(it.getCode()).stream()
                            .filter(rc -> rc.getCoeffGroup().equals(grp) && rc.getOptionName().equals(sel))
                            .mapToDouble(RiskCoeff::getValue)
                            .findFirst().orElse(1.0);
                }).reduce(1.0, (a,b)->a*b);

        double prem = cov * base * kTerm * kOpt;
        lblPrem.setText(String.format("%.2f", prem));
    }

    private void submit(MainFrame p) {
        String type   = (String) cbType.getSelectedItem();
        int    months = (Integer) spMonths.getValue();
        double cov    = Double.parseDouble(tfCoverage.getText().replace(',', '.'));

        StringBuilder resp = new StringBuilder();
        // Форматируем команду в Locale.US, чтобы получить точку, а не запятую:
        String cmd = String.format(Locale.US,
                "NEWAPP %s %d %.2f",
                type, months, cov
        );
        p.sendSync(cmd, resp::append);
        if (!resp.toString().startsWith("OK")) {
            JOptionPane.showMessageDialog(this, "Сервер: " + resp, "Ошибка", JOptionPane.ERROR_MESSAGE);
            return;
        }
        long appId = Long.parseLong(resp.toString().split(" ")[1]);
        comboMap.forEach((grp, cb) -> {
            String opt = cb.getSelectedItem().toString().replace(' ', '_');
            p.sendCommand(String.format("NEWAPP_ANSWER %d %s %s", appId, grp, opt), false);
        });

        dispose();
        p.refreshApplications();
    }

    private void addRow(GridBagConstraints c, int y, String text, JComponent comp) {
        // Метка в колонке 0 — не растягиваем
        c.gridx     = 0;
        c.gridy     = y;
        c.gridwidth = 1;
        c.weightx   = 0;
        c.fill      = GridBagConstraints.NONE;
        add(new JLabel(text), c);

        // Компонент в колонке 1 — растягиваем по ширине
        c.gridx     = 1;
        c.weightx   = 1.0;
        c.fill      = GridBagConstraints.HORIZONTAL;
        add(comp, c);

        // «Сброс» поведения на случай, если где-то дальше вы не переопределите
        c.fill    = GridBagConstraints.NONE;
        c.weightx = 0;
    }


    private static class SimpleChange implements DocumentListener {
        private final Runnable r;
        SimpleChange(Runnable r) { this.r = r; }
        public void insertUpdate(DocumentEvent e) { r.run(); }
        public void removeUpdate(DocumentEvent e) { r.run(); }
        public void changedUpdate(DocumentEvent e) { r.run(); }
    }
}
