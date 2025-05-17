package org.strah.client;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

/**
 * Модель для вкладки «Заявки-страхование».
 * Каждая строка приходит от сервера в виде:
 *    <id> SEP <typeCode> SEP <termMonths> SEP <coverageAmount>
 *      SEP <premium> SEP <status> SEP <startDate> SEP <endDate>
 */
public class ApplicationModel extends AbstractTableModel implements MainFrame.LineReceiver {
    private static final String SEP = "\u001F";

    private final List<String[]> rows = new ArrayList<>();
    private final String[] columns = {
            "ID", "Тип", "Срок (мес.)", "Покрытие", "Премия", "Статус", "Начало", "Окончание"
    };

    @Override
    public void clear() {
        rows.clear();
        fireTableDataChanged();
    }

    @Override
    public void addFromLine(String line) {
        String[] parts = line.split(SEP, -1);
        if (parts.length < 8) return;

        String startDate = parts[6];
        String endDate   = parts[7];

        String formattedStart = startDate;
        String formattedEnd   = endDate;

        try {
            if (!"-".equals(startDate) && !startDate.isBlank()) {
                formattedStart = java.time.LocalDate
                        .parse(startDate, java.time.format.DateTimeFormatter.ISO_DATE)
                        .format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy"));
            }
            if (!"-".equals(endDate) && !endDate.isBlank()) {
                formattedEnd = java.time.LocalDate
                        .parse(endDate, java.time.format.DateTimeFormatter.ISO_DATE)
                        .format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy"));
            }
        } catch (Exception e) {
            // Если ошибка — оставим как есть
        }

        rows.add(new String[] {
                parts[0], parts[1], parts[2], parts[3],
                parts[4], parts[5], formattedStart, formattedEnd
        });

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
