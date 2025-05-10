package org.strah.client;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

public class ApplicationModel extends AbstractTableModel
        implements MainFrame.LineReceiver {

    private final String[] COLS = { "ID", "Тип", "Мес.", "Статус", "Премия" };
    private final java.util.List<Object[]> data = new java.util.ArrayList<>();

    /* таблица JTable спрашивает размеры */
    public int getRowCount()            { return data.size(); }
    public int getColumnCount()         { return COLS.length; }
    public String getColumnName(int c)  { return COLS[c]; }

    public Object getValueAt(int r,int c){ return data.get(r)[c]; }

    /* LineReceiver ---- */
    public void addFromLine(String s){
        // id type months status premium
        String[] p = s.split(" ");
        Object[] row = new Object[]{
                Long.parseLong(p[0]),
                p[1],
                Integer.parseInt(p[2]),
                p[3],
                Double.parseDouble(p[4])
        };
        data.add(row);
        fireTableRowsInserted(data.size()-1,data.size()-1);
    }
    public void clear(){ data.clear(); fireTableDataChanged(); }
}
