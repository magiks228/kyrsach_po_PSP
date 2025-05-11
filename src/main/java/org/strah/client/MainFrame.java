package org.strah.client;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class MainFrame extends JFrame {

    private final String         role;
    private final Socket         socket;
    private final PrintWriter    out;
    private final BufferedReader in;

    private final PolicyModel      policyModel = new PolicyModel();
    private final ClaimModel       claimModel  = new ClaimModel();
    private final ApplicationModel appModel    = new ApplicationModel();
    public ApplicationModel getAppModel() {
        return appModel;
    }

    /* -------------------------------------------------------------------- */
    public MainFrame(Socket sock, PrintWriter out, BufferedReader in, String role) {
        super("Страхование — " + role);
        this.socket = sock; this.out = out; this.in = in; this.role = role;

        JTabbedPane tabs = new JTabbedPane();

        /* --- Полисы ------------------------------------------------------ */
        JTable tblPol = new JTable(policyModel);
        JButton bPol  = new JButton("Обновить");
        bPol.addActionListener(e -> request("POLICIES", policyModel));

        JPanel pnlPol = new JPanel(new BorderLayout());
        pnlPol.add(new JScrollPane(tblPol), BorderLayout.CENTER);
        pnlPol.add(bPol, BorderLayout.SOUTH);
        tabs.addTab("Полисы", pnlPol);

        /* --- Заявки на выплаты (Claim) ----------------------------------- */
        JTable   tblCl = new JTable(claimModel);
        JButton  bCl   = new JButton("Обновить");
        JButton  bNewC = new JButton("Создать заявку");
        bCl.addActionListener(e -> request("CLAIMS", claimModel));
        bNewC.addActionListener(e ->
                new NewClaimDialog(this, policyModel.getPolicyNumbers()).setVisible(true));

        JPanel southCl = new JPanel();
        southCl.add(bCl); southCl.add(bNewC);

        JPanel pnlCl = new JPanel(new BorderLayout());
        pnlCl.add(new JScrollPane(tblCl), BorderLayout.CENTER);
        pnlCl.add(southCl, BorderLayout.SOUTH);
        tabs.addTab("Заявки-выплаты", pnlCl);

        /* --- Заявки клиентов (Application) ------------------------------- */
        JTable  tblAp  = new JTable(appModel);
        JButton bApR   = new JButton("Обновить");
        JButton bApAdd = new JButton("Новая заявка");
        bApR.addActionListener(e -> request("APPLIST", appModel));
        bApAdd.addActionListener(e -> new NewAppDialog(this).setVisible(true));

        JPanel southAp = new JPanel();
        southAp.add(bApR); southAp.add(bApAdd);

        JPanel pnlAp = new JPanel(new BorderLayout());
        pnlAp.add(new JScrollPane(tblAp), BorderLayout.CENTER);
        pnlAp.add(southAp, BorderLayout.SOUTH);
        tabs.addTab("Заявки-страхование", pnlAp);

        /* --- Новый полис (для админов/сотрудников) ----------------------- */
        if (role.equals("Администратор") || role.equals("Сотрудник")) {
            List<String> clients = loadClients();
            tabs.addTab("Новый полис", new PolicyCreatePanel(this, clients));
        }

        /* --- Пользователи (только админ) --------------------------------- */
        if (role.equalsIgnoreCase("Администратор")) {
            UsersPanel users = new UsersPanel(out, in);
            tabs.addTab("Пользователи", users);
        }

        add(tabs);
        setSize(900, 550);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
    }


    /* ---------- публичный refresh для таблицы заявок ------------------ */
    public void refreshApplications() {
        request("APPLIST", appModel);
    }

    /* ======================= util-методы =============================== */

    /** Создание заявки-выплаты (вызывает NewClaimDialog) */
    public void createClaim(String policyNum, double amount, String descr) {
        String cmd = "NEWCLAIM " + policyNum + " " + amount + " " + descr.replace(' ', '_');
        StringBuilder status = new StringBuilder();
        sendSync(cmd, status::append);

        if (status.toString().startsWith("OK"))
            JOptionPane.showMessageDialog(this, "Заявка создана");
        else
            JOptionPane.showMessageDialog(this, "Сервер: " + status,
                    "Ошибка", JOptionPane.ERROR_MESSAGE);

        request("CLAIMS", claimModel);
    }

    /** универсальный запрос-список */
    private void request(String cmd, LineReceiver model) {
        model.clear();
        try {
            out.println(cmd);
            String l;
            while (!(l = in.readLine()).equals("END")
                    && !l.startsWith("ERR") && !l.equals("EMPTY"))
                model.addFromLine(l);
            if (l.startsWith("ERR"))
                JOptionPane.showMessageDialog(this, "Сервер: " + l);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Связь потеряна");
        }
    }

    /** sendSync: читаем до OK / ERR / END */
    public void sendSync(String cmd, java.util.function.Consumer<String> handler) {
        out.println(cmd);
        try {
            String l;
            while ((l = in.readLine()) != null) {
                if ("END".equals(l)) break;
                handler.accept(l);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Связь потеряна");
        }
    }

    /** список логинов клиентов (для выпадающего списка) */
    private List<String> loadClients() {
        List<String> list = new ArrayList<>();
        out.println("USERS");
        try {
            String l;
            while (!(l = in.readLine()).equals("END")) {
                if (l.startsWith("ERR") || l.startsWith("EMPTY")) break;
                String[] p = l.split(" ");
                if (p.length >= 2 && p[1].equals("Клиент"))
                    list.add(p[0]);
            }
        } catch (Exception ignored) {}
        return list;
    }

    /** универсальная команда + опциональный рефреш полисов */
    void sendCommand(String cmd, boolean refreshPolicies) {
        StringBuilder st = new StringBuilder();
        sendSync(cmd, st::append);

        if (st.toString().startsWith("OK"))
            JOptionPane.showMessageDialog(this, "Успешно");
        else
            JOptionPane.showMessageDialog(this, "Сервер: " + st,
                    "Ошибка", JOptionPane.ERROR_MESSAGE);

        if (refreshPolicies) request("POLICIES", policyModel);
    }

    /* интерфейс для моделей-таблиц */
    interface LineReceiver { void addFromLine(String s); void clear(); }

    /* === геттеры для диалогов === */
    public PrintWriter    getWriter(){ return out; }
    public BufferedReader getReader(){ return in; }

}
