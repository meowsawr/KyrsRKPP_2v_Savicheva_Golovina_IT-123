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
                // отправляем одну строку с результатом
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
                    // отправляем только имена файлов
                    out.println("LIST_START");
                    for (String filename : fileManager.getAvailableFiles()) {
                        out.println(filename); // Просто имя файла
                    }
                    out.println("LIST_END");

                } else if (command.startsWith("DOWNLOAD ")) {
                    String filename = command.substring(9);
                    File file = fileManager.getFile(filename);

                    if (file == null) {
                        out.println("FILE_NOT_FOUND");
                    } else {
                        // отправляем файл без информации о размере
                        try (FileInputStream fis = new FileInputStream(file);
                             OutputStream socketOut = socket.getOutputStream()) {

                            byte[] buffer = new byte[8192];
                            int bytesRead;

                            while ((bytesRead = fis.read(buffer)) != -1) {
                                socketOut.write(buffer, 0, bytesRead);
                                socketOut.flush();

                                // разная скорость скачивания
                                if (userManager.isVip(currentUser)) {
                                    Thread.sleep(10); // Быстро для VIP
                                } else {
                                    Thread.sleep(50); // Медленно для обычных
                                }
                            }
                            System.out.println("Файл отправлен: " + filename);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                } else if (command.equals("EXIT")) {
                    out.println("GOODBYE");
                    break;
                } else {
                    out.println("UNKNOWN_COMMAND");
                }
            }
        } catch (Exception e) {
            System.out.println("Ошибка с клиентом: " + e.getMessage());
        } finally {
            try {
                socket.close();
                System.out.println("Соединение закрыто");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}