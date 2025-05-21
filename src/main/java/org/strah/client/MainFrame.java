package org.strah.client;

import org.strah.model.users.Role;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;
import javax.swing.RowFilter;

public class MainFrame extends JFrame {

    private static final String SEP = ""; // ASCII Unit Separator

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
        alignTableColumns(tblPol);
        TableRowSorter<PolicyModel> polSorter = new TableRowSorter<>(policyModel);
        tblPol.setRowSorter(polSorter);
        JComboBox<String> cbPolUserFilter = new JComboBox<>();
        cbPolUserFilter.addItem("Все");
        for (String client : clients) cbPolUserFilter.addItem(client);
        cbPolUserFilter.addActionListener(e -> {
            String selected = (String) cbPolUserFilter.getSelectedItem();
            if ("Все".equals(selected)) {
                polSorter.setRowFilter(null);
            } else {
                polSorter.setRowFilter(RowFilter.regexFilter("^" + selected + "$", 6)); // колонка login
            }
        });
        JButton bPol  = new JButton("Обновить");
        bPol.setActionCommand("refresh");
        styleButton(bPol);
        bPol.addActionListener(e -> {
            request("POLICIES", policyModel);
            if (isStaff) {
                TableRowSorter<PolicyModel> policySorterFiltered = (TableRowSorter<PolicyModel>) tblPol.getRowSorter();
                policySorterFiltered.setRowFilter(new RowFilter<PolicyModel, Integer>() {
                    public boolean include(Entry<? extends PolicyModel, ? extends Integer> entry) {
                        try {
                            double coverage = Double.parseDouble(entry.getStringValue(2));
                            return coverage > 0;
                        } catch (Exception e) {
                            return true;
                        }
                    }
                });
            }
        });

        JPanel pnlPol = new JPanel(new BorderLayout());
        JPanel southPol = new JPanel();
        southPol.add(new JLabel("Фильтр по клиенту:"));
        southPol.add(cbPolUserFilter);
        southPol.add(bPol);
        pnlPol.add(new JScrollPane(tblPol), BorderLayout.CENTER);
        pnlPol.add(southPol, BorderLayout.SOUTH);
        tabs.addTab("Полисы", pnlPol);

        // --- Заявки-выплаты ---
        JTable tblCl = new JTable(claimModel);
        alignTableColumns(tblCl);
        TableRowSorter<ClaimModel> sorter = new TableRowSorter<>(claimModel);
        tblCl.setRowSorter(sorter);

        JButton bCl = new JButton("Обновить");
        bCl.setActionCommand("refresh");
        styleButton(bCl);
        JButton bNewC = new JButton("Создать заявку");
        bNewC.setActionCommand("plus");
        styleButton(bNewC);
        JButton bApproveClaim = new JButton("Одобрить выплату");
        bApproveClaim.setActionCommand("check");
        styleButton(bApproveClaim);
        JButton bRejectClaim  = new JButton("Отклонить выплату");
        bRejectClaim.setActionCommand("decline");
        styleButton(bRejectClaim);

        JComboBox<String> cbFilter = new JComboBox<>(new String[] {
                "Все", "NEW", "APPROVED", "REJECTED"
        });
        cbFilter.addActionListener(e -> {
            String selected = (String) cbFilter.getSelectedItem();
            if ("Все".equals(selected)) {
                sorter.setRowFilter(null);
            } else {
                sorter.setRowFilter(RowFilter.regexFilter("^" + selected + "$", 3));
            }
        });

        bCl.addActionListener(e -> {
            request("CLAIMS", claimModel);
            if (isStaff) {
                TableRowSorter<ClaimModel> claimSorterFiltered = (TableRowSorter<ClaimModel>) tblCl.getRowSorter();
                claimSorterFiltered.setRowFilter(new RowFilter<ClaimModel, Integer>() {
                    public boolean include(Entry<? extends ClaimModel, ? extends Integer> entry) {
                        String status = entry.getStringValue(3);
                        return !(status.equalsIgnoreCase("APPROVED") || status.equalsIgnoreCase("REJECTED"));
                    }
                });
            }
        });

        bNewC.addActionListener(e ->
                new NewClaimDialog(this, policyModel.getCoverageMap()).setVisible(true)
        );
        bApproveClaim.addActionListener(e -> processClaimAction("APPROVE_CLAIM", tblCl));
        bRejectClaim .addActionListener(e -> processClaimAction("REJECT_CLAIM", tblCl));

        JPanel pnlCl = new JPanel(new BorderLayout());
        JPanel southCl = new JPanel();
        southCl.add(bCl);
        southCl.add(bNewC);
        if (isStaff) {
            southCl.add(bApproveClaim);
            southCl.add(bRejectClaim);
        }
        southCl.add(new JLabel("Фильтр:"));
        southCl.add(cbFilter);
        pnlCl.add(new JScrollPane(tblCl), BorderLayout.CENTER);
        pnlCl.add(southCl, BorderLayout.SOUTH);
        tabs.addTab("Заявки-выплаты", pnlCl);

        // --- Заявки-страхование ---
        JTable tblApp = new JTable(appModel);
        alignTableColumns(tblApp);
        TableRowSorter<ApplicationModel> appSorter = new TableRowSorter<>(appModel);
        tblApp.setRowSorter(appSorter);
        JComboBox<String> cbAppFilter = new JComboBox<>(new String[] {
                "Все", "NEW", "WAIT_PAYMENT", "PAID", "DECLINED", "FINISHED"
        });
        cbAppFilter.addActionListener(e -> {
            String selected = (String) cbAppFilter.getSelectedItem();
            if ("Все".equals(selected)) {
                appSorter.setRowFilter(null);
            } else {
                appSorter.setRowFilter(RowFilter.regexFilter("^" + selected + "$", 5));
            }
        });

        JScrollPane spApp = new JScrollPane(tblApp);

        JButton bAppRefresh = new JButton("Обновить");
        bAppRefresh.setActionCommand("refresh");
        styleButton(bAppRefresh);
        JButton bAppApprove = new JButton("Одобрить");
        bAppApprove.setActionCommand("check");
        styleButton(bAppApprove);
        JButton bAppDecline = new JButton("Отклонить");
        bAppDecline.setActionCommand("decline");
        styleButton(bAppDecline);
        JButton bAppNew     = new JButton("Новая заявка");
        bAppNew.setActionCommand("plus");
        styleButton(bAppNew);
        JButton bAppPay     = new JButton("Оплатить");
        bAppPay.setActionCommand("pay");
        styleButton(bAppPay);
        JButton bAppConfirm = new JButton("Выпустить полис");
        bAppConfirm.setActionCommand("shield");
        styleButton(bAppConfirm);


        bAppRefresh.addActionListener(e -> {
            request("APPLIST", appModel);
            if (isStaff) {
                TableRowSorter<ApplicationModel> appSorterFiltered = (TableRowSorter<ApplicationModel>) tblApp.getRowSorter();
                appSorterFiltered.setRowFilter(new RowFilter<ApplicationModel, Integer>() {
                    public boolean include(Entry<? extends ApplicationModel, ? extends Integer> entry) {
                        String status = entry.getStringValue(5);
                        return !(status.equalsIgnoreCase("FINISHED") || status.equalsIgnoreCase("DECLINED"));
                    }
                });
            }
        });

        bAppNew.addActionListener(e -> {
            NewAppDialog dlg = new NewAppDialog(this);
            if (!dlg.isFailedToLoad()) {
                dlg.setVisible(true);
            }
        });

        if (isStaff) {
            bAppApprove.addActionListener(e -> {
                int row = tblApp.convertRowIndexToModel(tblApp.getSelectedRow());
                if (row < 0) return;
                long id = Long.parseLong(appModel.getValueAt(row, 0).toString());
                sendCommand("APPROVE " + id, false);
                request("APPLIST", appModel);
            });
            bAppDecline.addActionListener(e -> {
                int row = tblApp.convertRowIndexToModel(tblApp.getSelectedRow());
                if (row < 0) return;
                long id = Long.parseLong(appModel.getValueAt(row, 0).toString());
                sendCommand("DECLINE " + id, false);
                request("APPLIST", appModel);
            });
            bAppConfirm.addActionListener(e -> {
                int row = tblApp.convertRowIndexToModel(tblApp.getSelectedRow());
                if (row < 0) {
                    JOptionPane.showMessageDialog(this, "Выберите заявку.");
                    return;
                }

                String status = appModel.getValueAt(row, 5).toString();
                if (!status.equals("PAID")) {
                    JOptionPane.showMessageDialog(this, "Можно выпускать полис только для заявок со статусом PAID.");
                    return;
                }

                long id = Long.parseLong(appModel.getValueAt(row, 0).toString());

                DateTimeFormatter tableFormat = DateTimeFormatter.ofPattern("dd-MM-yyyy");
                DateTimeFormatter sendFormat  = DateTimeFormatter.ofPattern("dd-MM-yyyy"); // можно и тот же

                String rawStart = appModel.getValueAt(row, 6).toString();
                String rawEnd   = appModel.getValueAt(row, 7).toString();

                String start = LocalDate.parse(rawStart, tableFormat).format(sendFormat);
                String end   = LocalDate.parse(rawEnd, tableFormat).format(sendFormat);

                sendCommand("NEWPOLICYFROMAPP " + id + " " + start + " " + end, true);
                request("APPLIST", appModel);
            });
        }
        if (isClient) {
            bAppPay.addActionListener(e -> {
                int row = tblApp.convertRowIndexToModel(tblApp.getSelectedRow());
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
            pnlAppButtons.add(bAppConfirm);
        }
        if (isClient) {
            pnlAppButtons.add(bAppPay);
        }
        pnlAppButtons.add(bAppNew);
        pnlAppButtons.add(new JLabel("Фильтр:"));
        pnlAppButtons.add(cbAppFilter);

        JPanel pnlApp = new JPanel(new BorderLayout());
        pnlApp.add(spApp, BorderLayout.CENTER);
        pnlApp.add(pnlAppButtons, BorderLayout.SOUTH);
        tabs.addTab("Заявки-страхование", pnlApp);

        if (isStaff) {
            tabs.addTab("Новый полис", policyCreatePanel);
            tabs.addTab("Пользователи", usersPanel);
        }

        getContentPane().add(tabs, BorderLayout.CENTER);
        setSize(900, 550);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        request("APPLIST", appModel);


        if (isStaff) {
            tabs.addTab("История", new HistoryPanel(this));
        }

    }

    private void processClaimAction(String cmd, JTable tbl) {
        int row = tbl.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Выберите заявку.");
            return;
        }

        String claimId = tbl.getValueAt(row, 0).toString();
        StringBuilder resp = new StringBuilder();
        sendSync(cmd + " " + claimId, resp::append);

        if (resp.toString().startsWith("OK")) {
            JOptionPane.showMessageDialog(this, "Успешно");
            request("CLAIMS", claimModel);
        } else {
            JOptionPane.showMessageDialog(this, "Сервер: " + resp,
                    "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void refreshApplications() {
        request("APPLIST", appModel);
    }

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

    public void sendSync(String cmd, Consumer<String> handler) {
        out.println(cmd);
        try {
            String line;
            while ((line = in.readLine()) != null) {
                if ("END".equals(line)) break;
                if ("EMPTY".equals(line)) {
                    handler.accept("EMPTY");
                    break;
                }
                handler.accept(line);
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Связь потеряна");
        }
    }


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

    public java.util.List<org.strah.client.PolicyCreatePanel.TypeInfo> loadPolicyTypes() {
        final String SEP = "";
        java.util.List<org.strah.client.PolicyCreatePanel.TypeInfo> list = new java.util.ArrayList<>();
        sendSync("INTYPE_LIST", line -> {
            String[] p = line.split(SEP);
            if (p.length >= 7) list.add(new org.strah.client.PolicyCreatePanel.TypeInfo(p));
        });
        return list;
    }

    public java.util.List<String> loadClients() {
        java.util.List<String> clients = new java.util.ArrayList<>();
        sendSync("USERS", line -> {
            if ("END".equals(line) || line.startsWith("ERR")) return;
            String[] p = line.split(" ", 3);
            if (p.length >= 2 && (
                    "CLIENT".equalsIgnoreCase(p[1]) ||
                            "КЛИЕНТ".equalsIgnoreCase(p[1])
            )) {
                clients.add(p[0]);
            }
        });
        return clients;
    }

    public void sendCommand(String cmd, boolean refreshPolicies) {
        StringBuilder resp = new StringBuilder();

        sendSync(cmd, line -> {
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

    private void styleButton(JButton button) {
        button.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        button.setMargin(new Insets(6, 14, 6, 14));

        String name = button.getActionCommand(); // Иконка по имени
        if (name != null) {
            String path = "/icons/" + name + ".png";
            URL iconUrl = getClass().getResource(path);
            if (iconUrl != null) {
                button.setIcon(new ImageIcon(iconUrl));
            } else {
                System.err.println("⚠ Иконка не найдена: " + path);
            }
        }
    }



    private void alignTableColumns(JTable table) {
        DefaultTableCellRenderer centerAlign = new DefaultTableCellRenderer();
        centerAlign.setHorizontalAlignment(SwingConstants.CENTER);
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(centerAlign);
        }
    }


}