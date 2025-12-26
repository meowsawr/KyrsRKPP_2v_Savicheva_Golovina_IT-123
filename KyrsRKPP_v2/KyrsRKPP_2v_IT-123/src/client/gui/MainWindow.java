package client.gui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;

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
                out.println("DOWNLOAD " + filename);

                // Автоматически сохраняем в текущую папку
                File file = new File(filename);

                try (FileOutputStream fos = new FileOutputStream(file);
                     InputStream socketIn = socket.getInputStream()) {

                    byte[] buffer = new byte[8192];
                    int bytesRead;

                    while ((bytesRead = socketIn.read(buffer)) != -1) {
                        fos.write(buffer, 0, bytesRead);
                    }

                    SwingUtilities.invokeLater(() -> {
                        statusLabel.setText("Файл скачан: " + filename);
                        JOptionPane.showMessageDialog(MainWindow.this,
                                "Файл сохранен как: " + file.getAbsolutePath(),
                                "Успех", JOptionPane.INFORMATION_MESSAGE);
                    });
                }

            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("Ошибка скачивания");
                    JOptionPane.showMessageDialog(MainWindow.this,
                            "Ошибка: " + e.getMessage(),
                            "Ошибка", JOptionPane.ERROR_MESSAGE);
                });
            }
        }).start();
    }
}