package org.strah.client;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.PrintWriter;

public class HistoryPanel extends JPanel {

    private final JTable tblHistPolicies = new JTable(new PolicyModel());
    private final JTable tblHistClaims = new JTable(new ClaimModel());
    private final JTable tblHistApps = new JTable(new ApplicationModel());

    public HistoryPanel(MainFrame parent) {
        setLayout(new BorderLayout());

        JTabbedPane historyTabs = new JTabbedPane();

        JScrollPane scrollPolicies = new JScrollPane(tblHistPolicies);
        JScrollPane scrollClaims = new JScrollPane(tblHistClaims);
        JScrollPane scrollApps = new JScrollPane(tblHistApps);

        historyTabs.addTab("Полисы", scrollPolicies);
        historyTabs.addTab("Заявки-выплаты", scrollClaims);
        historyTabs.addTab("Заявки-страхование", scrollApps);

        add(historyTabs, BorderLayout.CENTER);

        // Запросим все данные и фильтруем на клиенте
        loadAndFilterHistory(parent);
    }

    private void loadAndFilterHistory(MainFrame parent) {
        PrintWriter out = parent.getWriter();
        BufferedReader in = parent.getReader();

        // Полисы: показываем только те, у кого покрытие = 0
        PolicyModel policyModel = (PolicyModel) tblHistPolicies.getModel();
        policyModel.clear();
        parent.sendSync("POLICIES", line -> {
            if (line.contains("\u001F")) {
                String[] parts = line.split("\u001F", -1);
                try {
                    double coverage = Double.parseDouble(parts[2]);
                    if (coverage <= 0) {
                        policyModel.addFromLine(line);
                    }
                } catch (Exception ignored) {}
            }
        });

        // Выплаты: только APPROVED или REJECTED
        ClaimModel claimModel = (ClaimModel) tblHistClaims.getModel();
        claimModel.clear();
        parent.sendSync("CLAIMS", line -> {
            if (line.contains("\u001F")) {
                String[] parts = line.split("\u001F", -1);
                if (parts.length >= 4) {
                    String status = parts[3];
                    if ("APPROVED".equalsIgnoreCase(status) || "REJECTED".equalsIgnoreCase(status)) {
                        claimModel.addFromLine(line);
                    }
                }
            }
        });

        // Заявки-страхование: только FINISHED
        ApplicationModel appModel = (ApplicationModel) tblHistApps.getModel();
        appModel.clear();
        parent.sendSync("APPLIST", line -> {
            if (line.contains("\u001F")) {
                String[] parts = line.split("\u001F", -1);
                if (parts.length >= 6 && "FINISHED".equalsIgnoreCase(parts[5])) {
                    appModel.addFromLine(line);
                }
            }
        });
    }
}