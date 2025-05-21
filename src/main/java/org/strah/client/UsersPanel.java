package org.strah.client;

import org.strah.model.users.Role;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.BufferedReader;
import java.io.PrintWriter;

/**
 * Полная панель управления пользователями (CRUD).
 * <p>
 * Команды: USERS / NEWUSER / SETROLE / DELUSER
 */
public class UsersPanel extends JPanel {

    private final PrintWriter out;
    private final BufferedReader in;
    private final DefaultTableModel model;

    public UsersPanel(PrintWriter out, BufferedReader in) {
        this.out = out;
        this.in  = in;

        setLayout(new BorderLayout());

        /* ---------- Таблица ---------- */
        model = new DefaultTableModel(new String[]{"Login", "Role", "Full Name"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable tbl = new JTable(model);
        add(new JScrollPane(tbl), BorderLayout.CENTER);

        /* ---------- Кнопочная панель ---------- */
        JPanel btns = new JPanel();

        JButton bRefresh = new JButton("Обновить");
        JButton bAdd     = new JButton("Добавить");
        JButton bRole    = new JButton("Сменить роль");
        JButton bDel     = new JButton("Удалить");

        bRefresh.addActionListener(e -> loadUsers());
        bAdd    .addActionListener(e -> showAddDialog());
        bRole   .addActionListener(e -> changeRole(tbl.getSelectedRow()));
        bDel    .addActionListener(e -> deleteUser(tbl.getSelectedRow()));

        btns.add(bRefresh); btns.add(bAdd);
        btns.add(bRole);    btns.add(bDel);
        add(btns, BorderLayout.SOUTH);

        loadUsers();                      // при открытии
    }

    /* ------------------------------------------------------------
       Загрузка списка
       ------------------------------------------------------------ */
    private void loadUsers() {
        model.setRowCount(0);
        try {
            out.println("USERS");
            String line;
            while ((line = in.readLine()) != null) {
                if ("END".equals(line)) break;
                if (line.startsWith("ERR") || line.startsWith("EMPTY")) continue;
                String[] p = line.split(" ", 3);
                model.addRow(new Object[]{p[0], p[1], p.length > 2 ? p[2].replace('_', ' ') : ""});
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Связь потеряна");
        }
    }

    /* ------------------------------------------------------------
       Добавление
       ------------------------------------------------------------ */
    private void showAddDialog() {
        JTextField tfLogin = new JTextField(10);
        JPasswordField pf   = new JPasswordField(10);
        JComboBox<String> cbRole = new JComboBox<>(new String[]{
                Role.ADMIN.name(), Role.STAFF.name(), Role.CLIENT.name()});
        JTextField tfName = new JTextField(15);

        JPanel p = new JPanel(new GridLayout(4, 2, 4, 4));
        p.add(new JLabel("Login:"));      p.add(tfLogin);
        p.add(new JLabel("Password:"));   p.add(pf);
        p.add(new JLabel("Role:"));       p.add(cbRole);
        p.add(new JLabel("Full name:"));  p.add(tfName);

        if (JOptionPane.showConfirmDialog(this, p, "Новый пользователь",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;

        String cmd = String.join(" ",
                "NEWUSER",
                tfLogin.getText().trim(),
                new String(pf.getPassword()),
                (String) cbRole.getSelectedItem(),
                tfName.getText().trim().replace(' ', '_')
        );
        sendAndRefresh(cmd, "Пользователь добавлен");
    }

    /* ------------------------------------------------------------
       Смена роли
       ------------------------------------------------------------ */
    private void changeRole(int row) {
        if (row < 0) return;

        String login = (String) model.getValueAt(row, 0);
        String roleText = ((String) model.getValueAt(row, 1)).toUpperCase();

        Role current;
        switch (roleText) {
            case "КЛИЕНТ":
                current = Role.CLIENT;
                break;
            case "СОТРУДНИК":
                current = Role.STAFF;
                break;
            case "АДМИНИСТРАТОР":
                current = Role.ADMIN;
                break;
            default:
                JOptionPane.showMessageDialog(this, "Неизвестная роль: " + roleText);
                return;
        }

        Role next = current == Role.CLIENT ? Role.STAFF
                : current == Role.STAFF ? Role.ADMIN
                : Role.CLIENT;

        if (JOptionPane.showConfirmDialog(this,
                "Сменить роль " + login + " с " + current + " → " + next + " ?",
                "Подтверждение", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;

        sendAndRefresh("SETROLE " + login + " " + next.name(), "Роль изменена");
    }


    /* ------------------------------------------------------------
       Удаление
       ------------------------------------------------------------ */
    private void deleteUser(int row) {
        if (row < 0) return;
        String login = (String) model.getValueAt(row, 0);
        if (JOptionPane.showConfirmDialog(this,
                "Удалить пользователя " + login + " ?", "Подтверждение",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;

        sendAndRefresh("DELUSER " + login, "Удалён");
    }

    /* ------------------------------------------------------------
       Общий метод
       ------------------------------------------------------------ */
    private void sendAndRefresh(String cmd, String okMsg) {
        try {
            out.println(cmd);
            out.flush(); // гарантируем отправку

            boolean responded = false;

            while (true) {
                String line = in.readLine();
                if (line == null) break;


                if (line.startsWith("OK")) {
                    JOptionPane.showMessageDialog(this, okMsg);
                    loadUsers();
                    responded = true;
                } else if (line.startsWith("ERR")) {
                    String msg = line.contains("HasPolicies")
                            ? "Невозможно удалить пользователя: у него есть полисы."
                            : "Ошибка: " + line;
                    JOptionPane.showMessageDialog(this, msg, "Ошибка", JOptionPane.ERROR_MESSAGE);
                    responded = true;
                } else if ("END".equals(line)) {
                    if (!responded) {
                        JOptionPane.showMessageDialog(this, "Сервер завершил ответ без подтверждения.", "Нет ответа", JOptionPane.WARNING_MESSAGE);
                    }
                    break;
                }
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Связь потеряна");
        }
    }

}
