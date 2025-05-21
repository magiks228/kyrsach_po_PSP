package org.strah.client;

import com.formdev.flatlaf.FlatDarkLaf; // ✅ подключаем тему
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.Socket;

public class ClientStarter {
    public static void main(String[] args) {

        // ✅ Устанавливаем FlatLaf до запуска интерфейса
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf()); // Или FlatLightLaf
            UIManager.put("Table.showHorizontalLines", true);
            UIManager.put("Table.showVerticalLines", false); // опционально
            UIManager.put("Table.alternateRowColor", new Color(45, 45, 45));

            UIManager.put("TableHeader.background", new Color(60, 60, 60));
            UIManager.put("TableHeader.foreground", Color.WHITE);
            UIManager.put("TableHeader.font", new Font("Segoe UI", Font.BOLD, 13));
        } catch (Exception ex) {
            System.err.println("❌ Не удалось применить FlatLaf: " + ex);
        }

        SwingUtilities.invokeLater(() -> {
            try {
                Socket sock = new Socket("localhost", 8080);
                PrintWriter out = new PrintWriter(sock.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
                if (!"READY".equals(in.readLine()))
                    throw new IOException("Сервер не ответил READY");

                new AuthFrame(sock, out, in).setVisible(true);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "Сервер недоступен:\n" + e.getMessage());
            }
        });
    }
}
