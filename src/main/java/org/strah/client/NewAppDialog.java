package org.strah.client;

import org.strah.model.types.InsuranceType;
import org.strah.model.types.RiskCoeff;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Диалог «Новая заявка на страхование».
 *
 * 1.  Загружает insurance_types + risk_coefficients с сервера.
 * 2.  Показывает набор динамических ComboBox‑ов по группам коэффициентов.
 * 3.  Валидирует лимиты, считает премию в режиме real‑time.
 * 4.  Посылает:  NEWAPP  +  NEWAPP_ANSWER  (по каждому выбранному опциону).
 */
class NewAppDialog extends JDialog {

    /* ‑‑‑ кешированные справочники ‑‑‑ */
    private final List<InsuranceType>                   types;
    private final Map<String, List<RiskCoeff>>          coeffByType;

    /* динамические Combo “группа → выбранный option_code” */
    private final Map<String, JComboBox<String>> comboMap = new HashMap<>();
    private final JPanel optionPanel = new JPanel(new GridBagLayout());

    /* поля формы */
    private final JComboBox<String> cbType;
    private final JSpinner spMonths = new JSpinner(new SpinnerNumberModel(6, 1, 24, 1));
    private final JTextField tfCoverage = new JTextField("1000000", 12);

    private final JLabel lblBase   = new JLabel();     // базовая ставка
    private final JLabel lblLimits = new JLabel();     // мин / макс лимит
    private final JLabel lblPrem   = new JLabel("‑‑‑"); // рассчитанная премия BYN

    /* === КОНСТРУКТОР ===================================================== */
    NewAppDialog(MainFrame parent) {
        super(parent, "Новая заявка", true);

        /* 0. Справочники */
        types       = loadTypes(parent);
        coeffByType = loadCoeffs(parent);

        /* 1. Вид полиса */
        cbType = new JComboBox<>(
                types.stream().map(InsuranceType::getCode).toArray(String[]::new));
        cbType.addActionListener(e ->
                rebuildOptions((String) cbType.getSelectedItem()));

        /* 2. Макет */
        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4,4,4,4); c.anchor = GridBagConstraints.WEST;

        addRow(c,0,"Тип полиса:", cbType);
        addRow(c,1,"Срок (мес.):", spMonths);
        addRow(c,2,"Сумма покрытия:", tfCoverage);
        addRow(c,3,"Базовая ставка:", lblBase);
        addRow(c,4,"Лимиты:", lblLimits);

        /* панель с коэффициентами */
        c.gridx=0; c.gridy=5; c.gridwidth=2; add(optionPanel,c);

        addRow(c,6,"Премия:", lblPrem);

        /* 3. Кнопка отправки */
        JButton btnSend = new JButton("Отправить");
        btnSend.addActionListener(e -> submit(parent));
        c.gridx=0; c.gridy=7; c.gridwidth=2; add(btnSend,c);

        /* live‑пересчёт */
        tfCoverage.getDocument().addDocumentListener(new SimpleChange(this::recalc));
        spMonths.addChangeListener(e -> recalc());

        /* первичная инициализация */
        rebuildOptions(cbType.getItemAt(0));
        pack(); setLocationRelativeTo(parent);
    }

    /* === СПРАВОЧНИКИ С СЕРВЕРА ========================================== */
    private List<InsuranceType> loadTypes(MainFrame p){
        List<InsuranceType> list = new ArrayList<>();
        p.sendSync("INTYPE_LIST", line -> {
            // code nameRu limitMin limitMax baseMin baseMax defTerm franchise%
            String[] v = line.split(" ",8);
            list.add(new InsuranceType(
                    v[0],v[1],
                    Double.parseDouble(v[2]),Double.parseDouble(v[3]),
                    Double.parseDouble(v[4]),Double.parseDouble(v[5]),
                    Integer.parseInt(v[6]), Double.parseDouble(v[7])));
        });
        return list;
    }

    private Map<String,List<RiskCoeff>> loadCoeffs(MainFrame p){
        Map<String,List<RiskCoeff>> map = new HashMap<>();
        p.sendSync("INCOEFF_LIST", line -> {
            // typeCode group optCode optName kValue
            String[] v = line.split(" ",5);
            RiskCoeff rc = new RiskCoeff(v[0],v[1],v[2],v[3],Double.parseDouble(v[4]));
            map.computeIfAbsent(rc.getTypeCode(),k->new ArrayList<>()).add(rc);
        });
        return map;
    }

    /* === ДИНАМИЧЕСКИЕ ПАРАМЕТРЫ ======================================== */
    private void rebuildOptions(String typeCode){
        optionPanel.removeAll(); comboMap.clear();

        Map<String, List<RiskCoeff>> byGrp =
                coeffByType.getOrDefault(typeCode, List.of())
                        .stream()
                        .collect(Collectors.groupingBy(RiskCoeff::getGroup));

        GridBagConstraints c = new GridBagConstraints();
        c.insets=new Insets(2,2,2,2); c.anchor=GridBagConstraints.WEST;
        int row=0;
        for(var e: byGrp.entrySet()){
            c.gridx=0; c.gridy=row;
            optionPanel.add(new JLabel(e.getKey()+":"),c);

            JComboBox<String> cb = new JComboBox<>(
                    e.getValue().stream()
                            .map(RiskCoeff::getOptionName)
                            .toArray(String[]::new));
            comboMap.put(e.getKey(), cb);
            c.gridx=1;
            optionPanel.add(cb,c);
            row++;
        }
        /* сразу обновляем метаданные + премию */
        updateMeta();
        optionPanel.revalidate(); optionPanel.repaint();
    }

    private void updateMeta(){
        InsuranceType it = types.stream()
                .filter(t -> t.getCode().equals(cbType.getSelectedItem()))
                .findFirst().orElseThrow();

        lblBase.setText(String.format("%.4f",
                (it.getBaseRateMin()+it.getBaseRateMax())/2.0));
        lblLimits.setText(String.format("%.0f – %.0f",
                it.getLimitMin(), it.getLimitMax()));

        /* дефолтный срок */
        spMonths.setValue(it.getDefaultTerm());
        recalc();
    }

    private void recalc(){
        try{
            InsuranceType it = types.stream()
                    .filter(t -> t.getCode().equals(cbType.getSelectedItem()))
                    .findFirst().orElseThrow();

            double base = (it.getBaseRateMin()+it.getBaseRateMax())/2.0;
            double cov  = Double.parseDouble(tfCoverage.getText().replace(',','.'));

            /* K_TERM */
            int m   = (Integer) spMonths.getValue();
            double kTerm = 1.0;          // пока =1, позже можно вытянуть с сервера

            /* K_options */
            double kOpt  = 1.0;
            List<RiskCoeff> rcs = coeffByType.get(it.getCode());
            for(var e: comboMap.entrySet()){
                String grp = e.getKey();
                String optName = (String) e.getValue().getSelectedItem();
                kOpt*= rcs.stream()
                        .filter(rc-> rc.getGroup().equals(grp)
                                && rc.getOptionName().equals(optName))
                        .map(RiskCoeff::getValue).findFirst().orElse(1.0);
            }
            double prem = cov * base * kTerm * kOpt;
            lblPrem.setText(String.format("%.2f", prem));
        }catch(Exception ex){
            lblPrem.setText("‑‑‑");
        }
    }

    /* === ОТПРАВКА НА СЕРВЕР ============================================ */
    private void submit(MainFrame p){
        String type   = (String) cbType.getSelectedItem();
        int    months = (Integer) spMonths.getValue();
        double cov    = Double.parseDouble(tfCoverage.getText().replace(',','.'));

        /* 1. создаём заявку -> получаем ID */
        StringBuilder resp = new StringBuilder();
        p.sendSync(String.format("NEWAPP %s %d %.2f", type, months, cov), resp);

        if(!resp.toString().startsWith("OK")){
            JOptionPane.showMessageDialog(this,"Сервер: "+resp,"Ошибка",JOptionPane.ERROR_MESSAGE);
            return;
        }
        long appId = Long.parseLong(resp.toString().split(" ")[1]);

        /* 2. отправляем ответы коэффициентов */
        for(var e: comboMap.entrySet()){
            String grp = e.getKey();
            String opt = e.getValue().getSelectedItem().toString().replace(' ','_');
            p.sendCommand(String.format("NEWAPP_ANSWER %d %s %s", appId, grp, opt), false);
        }

        p.refreshApplications();
        dispose();
    }

    /* === УТИЛИТЫ ======================================================== */
    private void addRow(GridBagConstraints c,int y,String lbl,JComponent comp){
        c.gridx=0; c.gridy=y; add(new JLabel(lbl),c);
        c.gridx=1;           add(comp,c);
    }

    private static class SimpleChange implements DocumentListener{
        private final Runnable r;
        SimpleChange(Runnable r){ this.r=r; }
        public void insertUpdate(DocumentEvent e){ r.run(); }
        public void removeUpdate(DocumentEvent e){ r.run(); }
        public void changedUpdate(DocumentEvent e){ r.run(); }
    }
}
