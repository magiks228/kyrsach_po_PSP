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

    private JComboBox<String> cbType;
    private final JSpinner spMonths = new JSpinner(new SpinnerNumberModel(6, 1, 36, 1));
    private final JTextField tfCoverage = new JTextField("1000000", 12);

    private final JLabel lblBase   = new JLabel();
    private final JLabel lblLimits = new JLabel();
    private final JLabel lblPrem   = new JLabel("–––");

    private final JButton btnSend = new JButton("Отправить");

    private boolean failedToLoad = false;
    public boolean isFailedToLoad() {
        return failedToLoad;
    }

    public NewAppDialog(MainFrame parent) {
        super(parent, "Новая заявка", true);

        // 0. Загрузка справочников
        types       = loadTypes(parent);
        coeffByType = loadCoeffs(parent);

        if (types.isEmpty()) {
            JOptionPane.showMessageDialog(parent,
                    "Не удалось загрузить типы страхования.\nПовторите попытку позже.",
                    "Ошибка загрузки", JOptionPane.ERROR_MESSAGE);
            failedToLoad = true;
            return;
        }

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
            if (line.equals("EMPTY") || line.equals("END")) return;

            String[] v = line.split(SEP, 8);
            if (v.length != 8) {
                return;
            }

            try {
                // Проверяем, что это строка типа, а не случайная заявка или ошибка
                if (!v[0].matches("[A-Z&]+")) {
                    return;
                }

                InsuranceType t = new InsuranceType(
                        v[0],
                        v[1].replace('_', ' '),
                        Double.parseDouble(v[2]),
                        Double.parseDouble(v[3]),
                        Double.parseDouble(v[4]),
                        Double.parseDouble(v[5]),
                        Integer.parseInt(v[6]),
                        Double.parseDouble(v[7])
                );
                list.add(t);
            } catch (Exception ex) {
            }
        });

        if (list.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Не удалось загрузить типы страхования. Повторите попытку позже.",
                    "Ошибка загрузки", JOptionPane.ERROR_MESSAGE);
        }

        return list;
    }


    private Map<String, List<RiskCoeff>> loadCoeffs(MainFrame p) {
        Map<String, List<RiskCoeff>> map = new HashMap<>();

        p.sendSync("INCOEFF_LIST", line -> {
            if ("EMPTY".equals(line) || "END".equals(line)) {
                return;
            }

            String[] v = line.split(SEP, 5);
            if (v.length < 5) {
                return;
            }

            // Защита от случайных строк, не содержащих числовое значение
            try {
                double val = Double.parseDouble(v[4]);

                RiskCoeff rc = new RiskCoeff(
                        v[0], v[1], v[2],
                        v[3].replace('_', ' '),
                        val
                );
                map.computeIfAbsent(rc.getTypeCode(), k -> new ArrayList<>()).add(rc);

            } catch (NumberFormatException ex) {
                System.err.println("Пропущена строка (не число): " + line + " — " + ex.getMessage());
            } catch (Exception ex) {
                System.err.println("Ошибка парсинга коэффициента: " + line + " — " + ex.getMessage());
            }
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
        btnSend.setEnabled(false);
        p.sendSync(cmd, resp::append);
        String response = resp.toString().trim();
        if (response.isEmpty() || response.equalsIgnoreCase("EMPTY")) {
            JOptionPane.showMessageDialog(this,
                    "Сервер не вернул подтверждение.\nПопробуйте повторно нажать «Обновить» и убедитесь, что всё введено корректно.",
                    "Ошибка", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!response.startsWith("OK")) {
            JOptionPane.showMessageDialog(this, "Сервер: " + response, "Ошибка", JOptionPane.ERROR_MESSAGE);
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
