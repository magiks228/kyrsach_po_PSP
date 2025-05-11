package org.strah.client;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

public class ApplicationModel extends AbstractTableModel implements MainFrame.LineReceiver {
    private static final String SEP = "\u001F";

    private final List<String[]> rows = new ArrayList<>();
    private final String[] columnNames = {
            "ID", "Тип", "Мес.", "Покрытие", "Премия", "Статус", "Начало", "Конец"
    };

    @Override
    public void addFromLine(String line) {
        String[] parts;
        if (line.contains(SEP)) {
            // split только на 8 частей
            parts = line.split(SEP, 8);
        } else {
            parts = line.split(" ", 8);
        }

        // гарантируем длину 8
        if (parts.length < 8) {
            String[] tmp = new String[8];
            System.arraycopy(parts, 0, tmp, 0, parts.length);
            for (int i = parts.length; i < 8; i++) {
                tmp[i] = "-";
            }
            parts = tmp;
        }

        rows.add(parts);
        int last = rows.size() - 1;
        fireTableRowsInserted(last, last);
    }

    @Override
    public void clear() {
        int sz = rows.size();
        if (sz > 0) {
            rows.clear();
            fireTableRowsDeleted(0, sz - 1);
        }
    }

    @Override
    public int getRowCount() {
        return rows.size();
    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    @Override
    public String getColumnName(int col) {
        return columnNames[col];
    }

    @Override
    public Object getValueAt(int row, int col) {
        return rows.get(row)[col];
    }
}
