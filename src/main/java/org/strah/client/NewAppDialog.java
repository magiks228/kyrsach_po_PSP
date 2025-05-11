package org.strah.client;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

class NewAppDialog extends JDialog {

    /* модель: code → meta‑данные */
    private final Map<String, Meta> types = new LinkedHashMap<>();

    /* поля формы */
    private final JComboBox<String> cbType;
    private final JSpinner spMonths = new JSpinner(new SpinnerNumberModel(6,1,24,1));
    private final JTextField tfCoverage = new JTextField("1000000",12);
    private final JLabel lblBase   = new JLabel();
    private final JLabel lblLimits = new JLabel();
    private final JLabel lblPrem   = new JLabel("‑‑‑");

    private record Meta(double rate,int term,double limMin,double limMax){}

    NewAppDialog(MainFrame parent){
        super(parent,"Новая заявка",true);
        setLayout(new GridBagLayout());
        GridBagConstraints c=new GridBagConstraints();
        c.insets=new Insets(4,4,4,4); c.anchor=GridBagConstraints.WEST;

        /* 1. запрашиваем TYPES */
        loadTypes(parent);

        cbType = new JComboBox<>(types.keySet().toArray(new String[0]));
        cbType.addActionListener(e -> updateMeta());

        addRow(c,0,"Тип:",          cbType);
        addRow(c,1,"Срок (мес.):",  spMonths);
        addRow(c,2,"Сумма (BYN):",  tfCoverage);
        addRow(c,3,"Ставка base:",  lblBase);
        addRow(c,4,"Лимиты:",       lblLimits);
        addRow(c,5,"Премия:",       lblPrem);

        JButton btn = new JButton("Отправить");
        btn.addActionListener(e -> submit(parent));
        c.gridx=0; c.gridy=6; c.gridwidth=2; add(btn,c);

        /* live‑пересчёт */
        tfCoverage.getDocument().addDocumentListener(new SimpleChange(this::recalc));
        spMonths.addChangeListener(e -> recalc());

        updateMeta(); recalc();
        pack(); setLocationRelativeTo(parent);
    }

    /* ===== helpers ===== */

    private void loadTypes(MainFrame parent){
        PrintWriter out = parent.out;
        BufferedReader in = parent.in;
        out.println("TYPES");
        try{
            String l; while(!(l=in.readLine()).equals("END")){
                if(l.startsWith("ERR")||l.equals("EMPTY")) break;
                String[] p=l.split(" ");
                types.put(p[0],
                        new Meta(Double.parseDouble(p[1]),
                                Integer.parseInt(p[2]),
                                Double.parseDouble(p[3]),
                                Double.parseDouble(p[4])));
            }
        }catch(Exception ex){ JOptionPane.showMessageDialog(this,"Связь потеряна"); }
    }

    private void updateMeta(){
        Meta m = types.get(cbType.getSelectedItem());
        lblBase.setText(String.format("%.4f", m.rate()));
        lblLimits.setText(String.format("%.0f – %.0f", m.limMin(), m.limMax()));
        spMonths.setValue(m.term());
        recalc();
    }

    private void recalc(){
        try{
            Meta m = types.get(cbType.getSelectedItem());
            double cov = Double.parseDouble(tfCoverage.getText());
            double prem = cov * m.rate() * (double)spMonths.getValue()/12.0;
            lblPrem.setText(String.format("%.2f", prem));
        }catch(Exception ignored){ lblPrem.setText("‑‑‑"); }
    }

    private void submit(MainFrame parent){
        String code   = (String) cbType.getSelectedItem();
        int months    = (Integer) spMonths.getValue();
        String amount = tfCoverage.getText().replace(',','.');
        String answers= "coverage="+amount;        // примитивно

        parent.sendCommand(String.format("NEWAPP %s %d %s",code,months,answers),false);
        parent.refreshApplications();
        dispose();
    }

    private void addRow(GridBagConstraints c,int y,String lbl,JComponent comp){
        c.gridx=0; c.gridy=y; add(new JLabel(lbl),c);
        c.gridx=1;             add(comp,c);
    }

    /* удобный DocumentListener */
    private static class SimpleChange implements DocumentListener{
        private final Runnable r;
        SimpleChange(Runnable r){ this.r=r; }
        public void insertUpdate(DocumentEvent e){ r.run(); }
        public void removeUpdate(DocumentEvent e){ r.run(); }
        public void changedUpdate(DocumentEvent e){ r.run(); }
    }
}
