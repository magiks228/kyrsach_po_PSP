package org.strah.client;

import org.strah.model.users.Role;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.BufferedReader;
import java.io.PrintWriter;

public class UsersPanel extends JPanel {
    private final PrintWriter out;
    private final BufferedReader in;
    private final DefaultTableModel model;

    public UsersPanel(PrintWriter out, BufferedReader in) {
        this.out = out;
        this.in  = in;

        setLayout(new BorderLayout());

        // 1) Таблица пользователей
        model = new DefaultTableModel(new String[]{"Login","Role","Full Name"}, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        JTable table = new JTable(model);
        add(new JScrollPane(table), BorderLayout.CENTER);

        // 2) Кнопки внизу
        JPanel btns = new JPanel();
        JButton bRefresh = new JButton("Обновить");
        JButton bAdd     = new JButton("Добавить");

        bRefresh.addActionListener(e -> loadUsers());
        bAdd.addActionListener(e     -> showAddDialog());

        btns.add(bRefresh);
        btns.add(bAdd);
        add(btns, BorderLayout.SOUTH);

        // сразу загрузим список
        loadUsers();
    }

    private void loadUsers() {
        model.setRowCount(0);
        try {
            out.println("USERS");
            String line;
            while ((line = in.readLine()) != null) {
                if (line.equals("END")) break;
                if (line.startsWith("ERR")) {
                    JOptionPane.showMessageDialog(this, "Сервер: " + line, "Ошибка", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                // строка формата: login role full_name_with_underscores
                String[] parts = line.split(" ", 3);
                // восстанавливаем пробелы в имени
                String fullName = parts[2].replace('_',' ');
                model.addRow(new Object[]{ parts[0], parts[1], fullName });
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Связь потеряна", "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showAddDialog() {
        JTextField tfLogin = new JTextField(10);
        JPasswordField pf   = new JPasswordField(10);
        JComboBox<String> cbRole = new JComboBox<>(
                // подставляем именно имена enum-а, а не русские строки
                new String[]{ Role.ADMIN.name(), Role.STAFF.name(), Role.CLIENT.name() }
        );
        JTextField tfName  = new JTextField(15);

        JPanel p = new JPanel(new GridLayout(4,2,4,4));
        p.add(new JLabel("Login:"));      p.add(tfLogin);
        p.add(new JLabel("Password:"));   p.add(pf);
        p.add(new JLabel("Role:"));       p.add(cbRole);
        p.add(new JLabel("Full name:"));  p.add(tfName);

        int rc = JOptionPane.showConfirmDialog(
                this, p, "Новый пользователь",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE
        );
        if (rc != JOptionPane.OK_OPTION) return;

        String login = tfLogin.getText().trim();
        String pass  = new String(pf.getPassword()).trim();
        String role  = (String)cbRole.getSelectedItem();
        String name  = tfName.getText().trim().replace(' ','_');

        if (login.isEmpty() || pass.isEmpty() || name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Все поля обязательны", "Ошибка", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // шлём команду на сервер
        out.println(String.join(" ",
                "NEWUSER", login, pass, role, name
        ));

        try {
            // ждём ответ до END или первого OK/ERR
            String line;
            while ((line = in.readLine()) != null) {
                if (line.startsWith("OK")) {
                    JOptionPane.showMessageDialog(this, "Пользователь добавлен");
                    loadUsers();  // обновляем таблицу
                    return;
                } else if (line.startsWith("ERR")) {
                    JOptionPane.showMessageDialog(this, "Сервер: " + line, "Ошибка", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Связь потеряна", "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }
}
