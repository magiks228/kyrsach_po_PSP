package org.strah.client;

import javax.swing.*;
import java.awt.*;

/* если уже сделали getAppModel() в MainFrame – импорт не нужен */
class NewAppDialog extends JDialog {

    NewAppDialog(MainFrame parent){
        super(parent,"Новая заявка",true);
        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4,4,4,4);
        c.anchor = GridBagConstraints.WEST;

        /* ------ поля формы ------ */

        JComboBox<String> cbType = new JComboBox<>(new String[]{
                "PROP    - Property",
                "BI      - Business Interruption",
                "GL      - Liability",
                "E&O     - Errors & Omissions",
                "PROD    - Product Liability",
                "CREDIT  - Financial",
                "CYBER   - Cyber",
                "CARGO   - Cargo",
                "D&O     - D&O"
        });

        /* срок страхования пусть задаётся спиннером */
        JSpinner spMonths = new JSpinner(new SpinnerNumberModel(6, 1, 24, 1));

        JTextField tfDescr = new JTextField(15);

        addRow(c,0,"Тип:",        cbType);
        addRow(c,1,"Срок (мес.):",spMonths);
        addRow(c,2,"Описание:",   tfDescr);

        /* ------ кнопка --------- */
        JButton btn = new JButton("Отправить");
        btn.addActionListener(e -> {

            String code   = ((String) cbType.getSelectedItem()).split(" ")[0];
            int    months = (Integer) spMonths.getValue();
            String answers = "descr=" + tfDescr.getText().replace(' ', '_');

            String cmd = String.format("NEWAPP %s %d %s", code, months, answers);
            parent.sendCommand(cmd, false);

            /* сразу перерисовали таблицу заявок */
            parent.refreshApplications();

            dispose();
        });

        c.gridx=0; c.gridy=3; c.gridwidth=2;
        add(btn,c);

        pack();
        setLocationRelativeTo(parent);
    }

    private void addRow(GridBagConstraints c,int y,String lbl,JComponent comp){
        c.gridx=0; c.gridy=y; c.gridwidth=1; add(new JLabel(lbl),c);
        c.gridx=1;             add(comp,c);
    }
}
