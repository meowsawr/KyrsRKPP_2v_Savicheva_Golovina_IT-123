package client.gui;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class MainWindow extends JFrame {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String username;

    private JTable filesTable;
    private DefaultTableModel tableModel;
    private JButton downloadButton;
    private JLabel statusLabel;

    public MainWindow(Socket socket, BufferedReader in, PrintWriter out,
                      String username, String status) {
        this.socket = socket;
        this.in = in;
        this.out = out;
        this.username = username;

        setTitle("Файловый сервер - " + username);
        setSize(500, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        initializeUI();
        loadFileList();
    }

    private void initializeUI() {
        setLayout(new BorderLayout());

        // Таблица файлов (ОДНА колонка!)
        String[] columns = {"Доступные файлы"};
        tableModel = new DefaultTableModel(columns, 0);
        filesTable = new JTable(tableModel);

        // Панель кнопок
        JPanel buttonPanel = new JPanel();
        downloadButton = new JButton("Скачать выбранный файл");
        downloadButton.addActionListener(e -> downloadFile());
        buttonPanel.add(downloadButton);

        // Статус
        statusLabel = new JLabel("Готово", JLabel.CENTER);

        add(new JScrollPane(filesTable), BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
        add(statusLabel, BorderLayout.NORTH);

        // Двойной клик тоже скачивает
        filesTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    downloadFile();
                }
            }
        });
    }

    private void loadFileList() {
        try {
            out.println("LIST");
            String response = in.readLine();

            if ("LIST_START".equals(response)) {
                tableModel.setRowCount(0);

                String filename;
                while (!(filename = in.readLine()).equals("LIST_END")) {
                    tableModel.addRow(new Object[]{filename});
                }

                statusLabel.setText("Файлов: " + tableModel.getRowCount());
            }
        } catch (IOException e) {
            statusLabel.setText("Ошибка: " + e.getMessage());
        }
    }

    private void downloadFile() {
        int selectedRow = filesTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                    "Выберите файл из списка",
                    "Внимание", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String filename = (String) tableModel.getValueAt(selectedRow, 0);
        statusLabel.setText("Скачивание: " + filename);

        new Thread(() -> {
            try {
                // 1. Отправляем команду скачивания
                out.println("DOWNLOAD " + filename);
                out.flush();

                // 2. Читаем ответ от сервера
                String response = in.readLine();

                if (response == null) {
                    throw new IOException("Сервер не ответил");
                }

                // 3. Анализируем ответ
                if (response.startsWith("DOWNLOAD_START:")) {
                    // Новый формат: DOWNLOAD_START:filename:filesize
                    String[] parts = response.split(":");
                    String serverFilename = parts[1];
                    long fileSize = Long.parseLong(parts[2]);

                    System.out.println("Начинаю скачивание: " + serverFilename + " (" + fileSize + " байт)");

                    // 4. Создаем файл для сохранения
                    File file = new File(serverFilename);

                    // 5. Получаем InputStream для чтения БИНАРНЫХ данных
                    InputStream socketIn = socket.getInputStream();

                    // 6. Читаем файл
                    try (FileOutputStream fos = new FileOutputStream(file)) {

                        byte[] buffer = new byte[8192];
                        long totalRead = 0;
                        int bytesRead;

                        while (totalRead < fileSize) {
                            long remaining = fileSize - totalRead;
                            int toRead = (int) Math.min(buffer.length, remaining);

                            bytesRead = socketIn.read(buffer, 0, toRead);

                            if (bytesRead == -1) {
                                throw new IOException("Соединение прервано. Получено " + totalRead + " из " + fileSize + " байт");
                            }

                            fos.write(buffer, 0, bytesRead);
                            totalRead += bytesRead;

                            // Обновляем прогресс
                            final int progress = (int) ((totalRead * 100) / fileSize);
                            SwingUtilities.invokeLater(() -> {
                                statusLabel.setText("Скачивание: " + progress + "%");
                            });

                            // Небольшая пауза для UI
                            try {
                                Thread.sleep(1);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                        }

                        fos.flush();
                        System.out.println("Файл скачан успешно: " + totalRead + " байт");

                        // 7. УСПЕШНОЕ ЗАВЕРШЕНИЕ
                        SwingUtilities.invokeLater(() -> {
                            statusLabel.setText("Файл скачан: " + serverFilename);
                            JOptionPane.showMessageDialog(MainWindow.this,
                                    "✅ Файл успешно скачан!\n" +
                                            "Имя: " + serverFilename + "\n" +
                                            "Размер: " + (fileSize / 1024) + " KB\n" +
                                            "Сохранен в: " + file.getAbsolutePath(),
                                    "Успех", JOptionPane.INFORMATION_MESSAGE);
                        });

                    }

                } else if ("FILE_NOT_FOUND".equals(response)) {
                    SwingUtilities.invokeLater(() -> {
                        statusLabel.setText("Файл не найден");
                        JOptionPane.showMessageDialog(MainWindow.this,
                                "❌ Файл не найден на сервере",
                                "Ошибка", JOptionPane.ERROR_MESSAGE);
                    });
                } else if (response.startsWith("SIZE:")) {
                    // Старый формат для обратной совместимости
                    long fileSize = Long.parseLong(response.substring(5));
                    downloadFileOldFormat(filename, fileSize);
                } else {
                    throw new IOException("Неожиданный ответ сервера: " + response);
                }

            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("Ошибка скачивания");

                    String errorMsg = e.getMessage();
                    // Убираем технические детали для пользователя
                    if (errorMsg != null && errorMsg.contains("яШяЫC")) {
                        errorMsg = "Ошибка протокола: сервер отправил бинарные данные вместо текстовой команды";
                    }

                    JOptionPane.showMessageDialog(MainWindow.this,
                            "❌ Ошибка при скачивании:\n" + errorMsg,
                            "Ошибка", JOptionPane.ERROR_MESSAGE);
                });

                System.err.println("Ошибка при скачивании:");
                e.printStackTrace();
            }
        }).start();
    }

    // Метод для старого формата (если сервер все еще использует SIZE:)
    private void downloadFileOldFormat(String filename, long fileSize) {
        try {
            System.out.println("Использую старый формат для файла: " + filename);

            File file = new File(filename);
            InputStream socketIn = socket.getInputStream();

            try (FileOutputStream fos = new FileOutputStream(file)) {

                byte[] buffer = new byte[8192];
                long totalRead = 0;
                int bytesRead;

                while (totalRead < fileSize) {
                    long remaining = fileSize - totalRead;
                    int toRead = (int) Math.min(buffer.length, remaining);

                    bytesRead = socketIn.read(buffer, 0, toRead);

                    if (bytesRead == -1) {
                        throw new IOException("Соединение прервано");
                    }

                    fos.write(buffer, 0, bytesRead);
                    totalRead += bytesRead;
                }

                System.out.println("Файл скачан (старый формат): " + totalRead + " байт");

                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("Файл скачан: " + filename);
                    JOptionPane.showMessageDialog(MainWindow.this,
                            "Файл скачан (старый формат)",
                            "Успех", JOptionPane.INFORMATION_MESSAGE);
                });
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

