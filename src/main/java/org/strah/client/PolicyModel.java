package org.strah.client;

import javax.swing.table.AbstractTableModel;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Модель для вкладки «Полисы». Ожидает от сервера строки вида:
 *   <policyNumber> SEP <typeCode> SEP <coverage> SEP <startDate> SEP <endDate> SEP <premium> SEP <clientLogin>
 */
public class PolicyModel extends AbstractTableModel implements MainFrame.LineReceiver {
    private static final String SEP = "\u001F";

    private final List<String[]> rows = new ArrayList<>();
    private static final String[] COLUMNS = {
            "Номер полиса", "Тип", "Покрытие", "Дата начала", "Дата окончания", "Премия", "Клиент"
    };

    @Override
    public void clear() {
        rows.clear();
        fireTableDataChanged();
    }

    @Override
    public void addFromLine(String line) {
        String[] parts = line.split(SEP, -1);
        // ожидаем ровно 7 полей: [0]=номер, [1]=тип, [2]=покрытие, [3]=start, [4]=end, [5]=premia, [6]=client
        if (parts.length == COLUMNS.length) {
            rows.add(parts);
            int last = rows.size() - 1;
            fireTableRowsInserted(last, last);
        }
    }

    @Override public int getRowCount()       { return rows.size(); }
    @Override public int getColumnCount()    { return COLUMNS.length; }
    @Override public String getColumnName(int col) { return COLUMNS[col]; }
    @Override public Object getValueAt(int row, int col) {
        return rows.get(row)[col];
    }

    /** Для NewClaimDialog: список всех номеров полисов */
    public List<String> getPolicyNumbers() {
        return rows.stream()
                .map(r -> r[0])
                .collect(Collectors.toList());
    }

    /** Для NewClaimDialog: карта «номер полиса → покрытие» */
    public Map<String, Double> getCoverageMap() {
        return rows.stream().collect(Collectors.toMap(
                r -> r[0],
                r -> {
                    try {
                        return Double.parseDouble(r[2]);
                    } catch (NumberFormatException ex) {
                        return 0.0;
                    }
                }
        ));
    }
}
