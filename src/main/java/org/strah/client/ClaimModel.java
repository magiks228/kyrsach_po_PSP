package org.strah.client;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

/**
 * Модель для вкладки «Заявки-выплаты».
 * Каждая строка приходит от сервера в виде:
 *    <id> SEP <номер полиса> SEP <сумма> SEP <статус>
 */
public class ClaimModel extends AbstractTableModel implements MainFrame.LineReceiver {
    private static final String SEP = "\u001F";

    private final List<String[]> rows = new ArrayList<>();
    private final String[] columns = {
            "ID", "Полис", "Сумма", "Статус"
    };

    @Override
    public void clear() {
        rows.clear();
        fireTableDataChanged();
    }

    @Override
    public void addFromLine(String line) {
        String[] parts = line.split(SEP, -1);
        rows.add(parts);
        int last = rows.size() - 1;
        fireTableRowsInserted(last, last);
    }

    @Override
    public int getRowCount() {
        return rows.size();
    }

    @Override
    public int getColumnCount() {
        return columns.length;
    }

    @Override
    public String getColumnName(int column) {
        return columns[column];
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        return rows.get(rowIndex)[columnIndex];
    }
}
