package org.strah.client;

import org.strah.model.users.Role;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;


public class MainFrame extends JFrame {

    private final String         role;
    private final Socket         socket;
    private final PrintWriter    out;
    private final BufferedReader in;

    private final PolicyModel      policyModel = new PolicyModel();
    private final ClaimModel       claimModel  = new ClaimModel();
    private final ApplicationModel appModel    = new ApplicationModel();

    // Панели для STAFF/ADMIN
    private final PolicyCreatePanel policyCreatePanel;
    private final UsersPanel        usersPanel;

    public MainFrame(Socket sock,
                     PrintWriter out,
                     BufferedReader in,
                     String role) {
        super("Страхование — " + role);
        this.socket = sock;
        this.out    = out;
        this.in     = in;
        this.role   = role;

        boolean isStaff  = "ADMIN".equalsIgnoreCase(role)
                || "STAFF".equalsIgnoreCase(role)
                || "АДМИНИСТРАТОР".equalsIgnoreCase(role)
                || "СОТРУДНИК".equalsIgnoreCase(role);
        boolean isClient = "CLIENT".equalsIgnoreCase(role)
                || "КЛИЕНТ".equalsIgnoreCase(role);

        // Инициализируем панели
        // Для создания полиса нужен список логинов клиентов
        List<String> clients = loadClients();
        this.policyCreatePanel = new PolicyCreatePanel(this, clients);
        this.usersPanel        = new UsersPanel(out, in);

        JTabbedPane tabs = new JTabbedPane();

        tabs.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                int idx = tabs.getSelectedIndex();
                if ("Новый полис".equals(tabs.getTitleAt(idx))) {
                }
            }
        });


        // --- Полисы ---
        JTable tblPol = new JTable(policyModel);
        JButton bPol  = new JButton("Обновить");
        bPol.addActionListener(e -> request("POLICIES", policyModel));
        JPanel pnlPol = new JPanel(new BorderLayout());
        pnlPol.add(new JScrollPane(tblPol), BorderLayout.CENTER);
        pnlPol.add(bPol, BorderLayout.SOUTH);
        tabs.addTab("Полисы", pnlPol);

        // --- Заявки-выплаты ---
        JTable tblCl = new JTable(claimModel);
        JButton bCl = new JButton("Обновить");
        JButton bNewC = new JButton("Создать заявку");
        bCl.addActionListener(e -> request("CLAIMS", claimModel));
        bNewC.addActionListener(e ->
                new NewClaimDialog(this, policyModel.getCoverageMap()).setVisible(true)
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
        JButton bAppPay     = new JButton("Оплатить");

        bAppRefresh.addActionListener(e -> request("APPLIST", appModel));
        bAppNew    .addActionListener(e -> new NewAppDialog(this).setVisible(true));

        if (isStaff) {
            bAppApprove.addActionListener(e -> {
                int row = tblApp.getSelectedRow();
                if (row < 0) return;
                long id = Long.parseLong(appModel.getValueAt(row, 0).toString());
                sendCommand("APPROVE " + id, false);
                request("APPLIST", appModel);
            });
            bAppDecline.addActionListener(e -> {
                int row = tblApp.getSelectedRow();
                if (row < 0) return;
                long id = Long.parseLong(appModel.getValueAt(row, 0).toString());
                sendCommand("DECLINE " + id, false);
                request("APPLIST", appModel);
            });
        }
        if (isClient) {
            bAppPay.addActionListener(e -> {
                int row = tblApp.getSelectedRow();
                if (row < 0) return;
                String id = appModel.getValueAt(row, 0).toString();
                sendCommand("PAY " + id, false);
                request("APPLIST", appModel);
            });
        }

        JPanel pnlAppButtons = new JPanel();
        pnlAppButtons.add(bAppRefresh);
        if (isStaff) {
            pnlAppButtons.add(bAppApprove);
            pnlAppButtons.add(bAppDecline);
        }
        if (isClient) {
            pnlAppButtons.add(bAppPay);
        }
        pnlAppButtons.add(bAppNew);

        JPanel pnlApp = new JPanel(new BorderLayout());
        pnlApp.add(spApp, BorderLayout.CENTER);
        pnlApp.add(pnlAppButtons, BorderLayout.SOUTH);
        tabs.addTab("Заявки-страхование", pnlApp);

        // --- Вкладки для STAFF/ADMIN ---
        if (isStaff) {
            tabs.addTab("Новый полис", policyCreatePanel);
            tabs.addTab("Пользователи", usersPanel);
        }

        getContentPane().add(tabs, BorderLayout.CENTER);
        setSize(900, 550);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        // сразу загружаем список заявок
        request("APPLIST", appModel);
    }

    /**
     * Обновить список заявок-страхование (вызывается из NewAppDialog)
     */
    public void refreshApplications() {
        request("APPLIST", appModel);
    }

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
                if ("EMPTY".equals(line)) {
                    handler.accept("EMPTY");  // ← передадим дальше как строку
                    break;
                }
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


    /**
     * Загружает список страховых типов с сервера и возвращает их как объекты PolicyCreatePanel.TypeInfo
     * Команда:  INTYPE_LIST
     */
    /** Загружает виды страхования  (код‑>TypeInfo) */
    public java.util.List<org.strah.client.PolicyCreatePanel.TypeInfo> loadPolicyTypes() {
        final String SEP = "\u001F";
        java.util.List<org.strah.client.PolicyCreatePanel.TypeInfo> list = new java.util.ArrayList<>();
        sendSync("INTYPE_LIST", line -> {
            String[] p = line.split(SEP);
            if (p.length >= 7) list.add(new org.strah.client.PolicyCreatePanel.TypeInfo(p));
        });
        return list;
    }


    /** Загружает список клиентов (для "Новый полис") */
    /**
     * Загружает из сервера всех пользователей с ролью CLIENT (только их логины).
     * Команда: USERS
     */
    public java.util.List<String> loadClients() {
        java.util.List<String> clients = new java.util.ArrayList<>();
        System.out.println("▶ Отправка команды USERS");
        sendSync("USERS", line -> {
            System.out.println("◀ Ответ от сервера: " + line); // ← это выведет каждую строку
            if ("END".equals(line) || line.startsWith("ERR")) return;
            String[] p = line.split(" ", 3);
            if (p.length >= 2 && (
                    "CLIENT".equalsIgnoreCase(p[1]) ||
                            "КЛИЕНТ".equalsIgnoreCase(p[1])
            )) {
                clients.add(p[0]);
            }
        });
        System.out.println("✅ Итоговый список клиентов: " + clients); // ← покажет, что попало в список
        return clients;
    }


    /** Отправка команды с опциональным обновлением полисов */
    public void sendCommand(String cmd, boolean refreshPolicies) {
        StringBuilder resp = new StringBuilder();
        System.out.println("▶ Команда отправлена: " + cmd);

        sendSync(cmd, line -> {
            System.out.println("◀ Ответ: " + line);
            resp.append(line);
        });

        if (resp.toString().startsWith("OK")) {
            JOptionPane.showMessageDialog(this, "Успешно");
            if (refreshPolicies) {
                request("POLICIES", policyModel);
            }
        } else if (resp.toString().startsWith("EMPTY")) {
            JOptionPane.showMessageDialog(this, "Нет данных от сервера.");
        } else {
            JOptionPane.showMessageDialog(this, "Сервер: " + resp,
                    "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }


    public interface LineReceiver {
        void addFromLine(String s);
        void clear();
    }

    public PrintWriter    getWriter()  { return out; }
    public BufferedReader getReader()  { return in; }
}
