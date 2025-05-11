package org.strah.client;

import org.strah.model.users.Role;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class MainFrame extends JFrame {

    private final String         role;
    private final Socket         socket;
    private final PrintWriter    out;
    private final BufferedReader in;

    private final PolicyModel      policyModel = new PolicyModel();
    private final ClaimModel       claimModel  = new ClaimModel();
    private final ApplicationModel appModel    = new ApplicationModel();

    public MainFrame(Socket sock, PrintWriter out, BufferedReader in, String role) {
        super("Страхование — " + role);
        this.socket = sock;
        this.out    = out;
        this.in     = in;
        this.role   = role;

        JTabbedPane tabs = new JTabbedPane();

        // --- Полисы ---
        JTable tblPol = new JTable(policyModel);
        JButton bPol  = new JButton("Обновить");
        bPol.addActionListener(e -> request("POLICIES", policyModel));
        JPanel pnlPol = new JPanel(new BorderLayout());
        pnlPol.add(new JScrollPane(tblPol), BorderLayout.CENTER);
        pnlPol.add(bPol, BorderLayout.SOUTH);
        tabs.addTab("Полисы", pnlPol);

        // --- Заявки на выплаты ---
        JTable tblCl = new JTable(claimModel);
        JButton bCl = new JButton("Обновить");
        JButton bNewC = new JButton("Создать заявку");
        bCl.addActionListener(e -> request("CLAIMS", claimModel));
        bNewC.addActionListener(e ->
                new NewClaimDialog(this, policyModel.getPolicyNumbers()).setVisible(true)
        );
        JPanel pnlCl = new JPanel(new BorderLayout());
        JPanel southCl = new JPanel();
        southCl.add(bCl);
        southCl.add(bNewC);
        pnlCl.add(new JScrollPane(tblCl), BorderLayout.CENTER);
        pnlCl.add(southCl, BorderLayout.SOUTH);
        tabs.addTab("Заявки-выплаты", pnlCl);

        // --- Заявки-страхование ---
        JTable tblApp = new JTable(appModel);
        JScrollPane spApp = new JScrollPane(tblApp);

        JButton bAppRefresh = new JButton("Обновить");
        JButton bAppApprove = new JButton("Одобрить");
        JButton bAppDecline = new JButton("Отклонить");
        JButton bAppNew     = new JButton("Новая заявка");

        bAppRefresh.addActionListener(e -> request("APPLIST", appModel));
        bAppNew    .addActionListener(e -> new NewAppDialog(this).setVisible(true));

        // — одобрить
        bAppApprove.addActionListener(e -> {
            int row = tblApp.getSelectedRow();
            if (row < 0) return;
            String idStr = appModel.getValueAt(row, 0).toString();
            long id = Long.parseLong(idStr);
            sendCommand("APPROVE " + id, false);
            request("APPLIST", appModel);
        });
        // — отклонить
        bAppDecline.addActionListener(e -> {
            int row = tblApp.getSelectedRow();
            if (row < 0) return;
            String idStr = appModel.getValueAt(row, 0).toString();
            long id = Long.parseLong(idStr);
            sendCommand("DECLINE " + id, false);
            request("APPLIST", appModel);
        });

        boolean isStaff = role.equalsIgnoreCase("Сотрудник")
                || role.equalsIgnoreCase("Администратор");

        JPanel pnlAppButtons = new JPanel();
        pnlAppButtons.add(bAppRefresh);
        if (isStaff) {
            pnlAppButtons.add(bAppApprove);
            pnlAppButtons.add(bAppDecline);
        }
        pnlAppButtons.add(bAppNew);

        JPanel pnlApp = new JPanel(new BorderLayout());
        pnlApp.add(spApp, BorderLayout.CENTER);
        pnlApp.add(pnlAppButtons, BorderLayout.SOUTH);

        tabs.addTab("Заявки-страхование", pnlApp);

        // --- Новый полис (для STAFF/ADMIN) ---
        if (isStaff) {
            List<String> clients = loadClients();
            tabs.addTab("Новый полис", new PolicyCreatePanel(this, clients));
        }

        // --- Пользователи (только ADMIN) ---
        if (role.equalsIgnoreCase("Администратор")) {
            UsersPanel users = new UsersPanel(out, in);
            tabs.addTab("Пользователи", users);
        }

        add(tabs);
        setSize(900, 550);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        // при старте сразу подтягиваем список заявок
        request("APPLIST", appModel);
    }

    /** Перезагрузить список заявок-страхование */
    public void refreshApplications() {
        request("APPLIST", appModel);
    }

    /* ======================= util-методы =============================== */

    /** Универсальный метод запрос→модель */
    private void request(String cmd, LineReceiver model) {
        model.clear();
        try {
            out.println(cmd);
            String line;
            while ((line = in.readLine()) != null) {
                if ("END".equals(line) || "EMPTY".equals(line)) break;
                if (line.startsWith("ERR")) {
                    JOptionPane.showMessageDialog(this, "Сервер: " + line);
                    continue;
                }
                model.addFromLine(line);
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Связь потеряна");
        }
    }

    /** Отправка команд и чтение до OK/ERR/END */
    public void sendSync(String cmd, Consumer<String> handler) {
        out.println(cmd);
        try {
            String line;
            while ((line = in.readLine()) != null) {
                if ("END".equals(line)) break;
                handler.accept(line);
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Связь потеряна");
        }
    }

    /** Выпуск выплаты (из NewClaimDialog) */
    public void createClaim(String polNum, double amt, String descr) {
        String cmd = "NEWCLAIM " + polNum + " " + amt + " " + descr.replace(' ', '_');
        StringBuilder resp = new StringBuilder();
        sendSync(cmd, resp::append);
        if (resp.toString().startsWith("OK")) {
            JOptionPane.showMessageDialog(this, "Заявка создана");
        } else {
            JOptionPane.showMessageDialog(this, "Сервер: " + resp,
                    "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
        request("CLAIMS", claimModel);
    }

    /** Загружает список клиентов (для "Новый полис") */
    private List<String> loadClients() {
        List<String> list = new ArrayList<>();
        out.println("USERS");
        try {
            String line;
            while ((line = in.readLine()) != null) {
                if ("END".equals(line)) break;
                if (line.startsWith("ERR") || line.startsWith("EMPTY")) continue;
                String[] parts = line.split(" ", 3);
                if (parts.length < 2) continue;
                String login = parts[0], roleStr = parts[1];
                if (Role.CLIENT.name().equalsIgnoreCase(roleStr)
                        || "Клиент".equalsIgnoreCase(roleStr)) {
                    list.add(login);
                }
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                    "Связь потеряна при загрузке списка клиентов",
                    "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
        return list;
    }

    /** Отправка команды с визуальной индикацией */
    public void sendCommand(String cmd, boolean refreshPolicies) {
        StringBuilder resp = new StringBuilder();
        sendSync(cmd, resp::append);
        if (resp.toString().startsWith("OK")) {
            JOptionPane.showMessageDialog(this, "Успешно");
        } else {
            JOptionPane.showMessageDialog(this, "Сервер: " + resp,
                    "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
        if (refreshPolicies) {
            request("POLICIES", policyModel);
        }
    }

    interface LineReceiver {
        void addFromLine(String s);
        void clear();
    }

    public PrintWriter    getWriter() { return out; }
    public BufferedReader getReader() { return in; }
}
