package org.strah.client;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

public class ClaimModel extends AbstractTableModel implements MainFrame.LineReceiver {
    private final List<String[]> rows = new ArrayList<>();
    private static final String SEP = "\u001F";

    private final String[] columnNames = {
            "ID", "Полис", "Сумма", "Статус"
    };

    @Override
    public void addFromLine(String line) {
        // Разбираем либо по Unit Separator, либо по пробелам (fallback)
        String[] parts;
        if (line.contains(SEP)) {
            parts = line.split(SEP, 4);
        } else {
            parts = line.split(" ", 4);
        }

        // Если полей меньше четырёх, дополняем пустыми
        if (parts.length < 4) {
            String[] tmp = new String[4];
            System.arraycopy(parts, 0, tmp, 0, parts.length);
            for (int i = parts.length; i < 4; i++) {
                tmp[i] = "";
            }
            parts = tmp;
        }

        rows.add(parts);
        // Сообщаем таблице, что добавился новый ряд
        int newIndex = rows.size() - 1;
        fireTableRowsInserted(newIndex, newIndex);
    }

    @Override
    public void clear() {
        int oldSize = rows.size();
        rows.clear();
        if (oldSize > 0) {
            // Обновляем всю модель
            fireTableRowsDeleted(0, oldSize - 1);
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
    public String getColumnName(int column) {
        return columnNames[column];
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        return rows.get(rowIndex)[columnIndex];
    }
}
