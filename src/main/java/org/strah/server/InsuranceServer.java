package org.strah.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

// Класс многопоточного сервера
public class InsuranceServer {

    private static final int PORT = 8080;  // порт сервера

    public static void main(String[] args) {
        System.out.println("Сервер страхования запущен на порту " + PORT);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {

            // Сервер всегда ожидает подключения клиентов
            while (true) {
                Socket clientSocket = serverSocket.accept(); // Ждём клиента
                System.out.println("Подключился клиент: " + clientSocket);

                // Каждый клиент обрабатывается в отдельном потоке
                new ClientHandler(clientSocket).start();
            }

        } catch (IOException e) {
            System.err.println("Ошибка запуска сервера: " + e.getMessage());
        }
    }
}
