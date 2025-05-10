package org.strah.client;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

public class ClaimModel extends AbstractTableModel
        implements MainFrame.LineReceiver {

    private final List<String[]> rows = new ArrayList<>();
    private final String[] cols = {"ID","Полис","Сумма","Статус"};

    @Override public void addFromLine(String s){
        if(s.startsWith("EMPTY")) return;
        rows.add(s.split(" "));
        fireTableDataChanged();
    }
    @Override public void clear(){ rows.clear(); fireTableDataChanged(); }

    @Override public int getRowCount(){ return rows.size(); }
    @Override public int getColumnCount(){ return cols.length; }
    @Override public String getColumnName(int c){ return cols[c]; }
    @Override public Object getValueAt(int r,int c){ return rows.get(r)[c]; }
}
