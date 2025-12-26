package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
// главный класс сервера
public class MainServer {
    public static void main(String[] args) {
        System.out.println("=== ФАЙЛОВЫЙ СЕРВЕР ===");
        System.out.println("Порт: 12345");
        System.out.println("Для остановки: Ctrl+C");
        try (ServerSocket serverSocket = new ServerSocket(12345)) {
            System.out.println("Сервер запущен...");
            // цикл принятия подключений
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Новое подключение");
                new ClientHandler(clientSocket).start();
            }
        } catch (IOException e) {
            System.out.println("Ошибка сервера: " + e.getMessage());
        }
    }
}