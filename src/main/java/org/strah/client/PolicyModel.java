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
        if (parts.length != COLUMNS.length) return;

        String startDate = parts[3];
        String endDate   = parts[4];

        String formattedStart = startDate;
        String formattedEnd   = endDate;

        try {
            if (!startDate.isBlank() && !startDate.equals("-")) {
                formattedStart = java.time.LocalDate
                        .parse(startDate, java.time.format.DateTimeFormatter.ISO_DATE)
                        .format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy"));
            }
            if (!endDate.isBlank() && !endDate.equals("-")) {
                formattedEnd = java.time.LocalDate
                        .parse(endDate, java.time.format.DateTimeFormatter.ISO_DATE)
                        .format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy"));
            }
        } catch (Exception e) {
            // Логгирование можно добавить при желании
        }

        rows.add(new String[] {
                parts[0], // номер полиса
                parts[1], // тип
                parts[2], // покрытие
                formattedStart,
                formattedEnd,
                parts[5], // премия
                parts[6]  // клиент
        });

        fireTableRowsInserted(rows.size() - 1, rows.size() - 1);
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
