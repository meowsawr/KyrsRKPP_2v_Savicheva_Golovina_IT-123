package server;

import java.io.*;
import java.net.Socket;

public class ClientHandler extends Thread {
    private Socket socket;
    private UserManager userManager;
    private FileManager fileManager;
    private String currentUser;

    public ClientHandler(Socket socket) {
        this.socket = socket;
        this.userManager = new UserManager();
        this.fileManager = new FileManager();
    }

    @Override
    public void run() {
        System.out.println("Новый клиент подключился!");

        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {
            // АВТОРИЗАЦИЯ
            out.println("Введите логин:");
            String username = in.readLine();
            out.println("Введите пароль:");
            String password = in.readLine();

            if (userManager.authenticate(username, password)) {
                currentUser = username;
                boolean isVip = userManager.isVip(username);
                out.println("OK:" + (isVip ? "VIP" : "REGULAR"));
                System.out.println("Клиент " + username + " авторизован");

            } else {
                out.println("ERROR");
                socket.close();
                return;
            }

            // ОСНОВНОЙ ЦИКЛ
            while (true) {
                String command = in.readLine();
                if (command == null) break;

                System.out.println("Команда от " + currentUser + ": " + command);

                if (command.equals("LIST")) {
                    out.println("LIST_START");
                    for (String filename : fileManager.getAvailableFiles()) {
                        out.println(filename);
                    }
                    out.println("LIST_END");

                } else if (command.startsWith("DOWNLOAD ")) {
                    String filename = command.substring(9);
                    File file = fileManager.getFile(filename);

                    if (file == null) {
                        out.println("FILE_NOT_FOUND");
                    } else {
                        // ============================================
                        // КЛЮЧЕВОЕ ИЗМЕНЕНИЕ: Отправляем ВСЕ данные ОДНИМ сообщением
                        // Формат: "DOWNLOAD_START:filename:filesize"
                        // ============================================
                        long fileSize = file.length();
                        out.println("DOWNLOAD_START:" + filename + ":" + fileSize);
                        out.flush(); // Важно: немедленная отправка

                        // Ждем немного, чтобы клиент успел обработать
                        Thread.sleep(100);

                        // ============================================
                        // Отправляем ТОЛЬКО бинарные данные файла
                        // через ОТДЕЛЬНЫЙ OutputStream
                        // ============================================
                        OutputStream socketOut = socket.getOutputStream();
                        try (FileInputStream fis = new FileInputStream(file)) {

                            byte[] buffer = new byte[8192];
                            int bytesRead;
                            long totalSent = 0;

                            while ((bytesRead = fis.read(buffer)) != -1) {
                                socketOut.write(buffer, 0, bytesRead);
                                totalSent += bytesRead;

                                // разная скорость скачивания
                                if (userManager.isVip(currentUser)) {
                                    Thread.sleep(10);
                                } else {
                                    Thread.sleep(50);
                                }
                            }

                            // Финальный flush
                            socketOut.flush();

                            System.out.println("Файл отправлен: " + filename + " (" + totalSent + " байт)");

                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }

                        // ============================================
                        // НЕ отправляем подтверждение через out!
                        // Бинарные данные уже отправлены
                        // ============================================

                        // После отправки файла просто продолжаем цикл
                        // Клиент сам поймет, что файл закончился (прочитал filesize байт)
                    }
                } else if (command.equals("EXIT")) {
                    out.println("GOODBYE");
                    break;
                } else {
                    out.println("UNKNOWN_COMMAND");
                }
            }
        } catch (Exception e) {
            System.out.println("Ошибка с клиентом " + currentUser + ": " + e.getClass().getSimpleName());
            if (!e.getMessage().contains("Connection reset") &&
                    !e.getMessage().contains("Socket closed") &&
                    !e.getMessage().contains("Broken pipe")) {
                e.printStackTrace();
            }
        } finally {
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                    System.out.println("Соединение с " + currentUser + " закрыто");
                }
            } catch (IOException e) {
                // Игнорируем
            }
        }
    }
}