package org.strah.client;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

/** Таблица полисов. */
public class PolicyModel extends AbstractTableModel
        implements MainFrame.LineReceiver {

    private final List<String> rows = new ArrayList<>();
    private final String[] COLS = {"Номер","Тип","Премия"};

    /* ===== LineReceiver ===== */

    /** добавить строку, пришедшую с сервера */
    @Override public void addFromLine(String s) {
        rows.add(s.trim());
        fireTableDataChanged();
    }

    /** очистить таблицу перед повторной загрузкой */
    @Override public void clear() {
        rows.clear();
        fireTableDataChanged();
    }

    /** вспомогательно: список номеров полисов (для NewClaimDialog) */
    public List<String> getPolicyNumbers() {
        return rows.stream().map(r -> r.split(" ")[0]).toList();
    }

    /* ===== TableModel ===== */

    @Override public int getRowCount()           { return rows.size(); }
    @Override public int getColumnCount()        { return COLS.length; }
    @Override public String getColumnName(int c) { return COLS[c]; }

    @Override public Object getValueAt(int r,int c){
        String[] p = rows.get(r).split(" ");
        if(p.length < 3) return "";
        return switch (c) {
            case 0 -> p[0];   // номер
            case 1 -> p[1];   // тип
            case 2 -> p[2];   // премия
            default -> "";
        };
    }
}
