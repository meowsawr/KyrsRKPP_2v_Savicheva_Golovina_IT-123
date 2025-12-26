package client.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
// окно входа (View в MVC)
public class LoginWindow extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;

    public LoginWindow() {
        setTitle("Файловый сервер - Вход");
        setSize(350, 200);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        initializeUI();
    }

    private void initializeUI() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // форма входа
        JPanel formPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        formPanel.add(new JLabel("Логин:"));
        usernameField = new JTextField();
        formPanel.add(usernameField);

        formPanel.add(new JLabel("Пароль:"));
        passwordField = new JPasswordField();
        formPanel.add(passwordField);

        mainPanel.add(formPanel, BorderLayout.CENTER);

        // кнопка входа
        loginButton = new JButton("Войти");
        loginButton.addActionListener(new LoginAction());

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(loginButton);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(mainPanel);
        getRootPane().setDefaultButton(loginButton);
    }

    private class LoginAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword());

            if (username.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(LoginWindow.this,
                        "Заполните все поля!", "Ошибка", JOptionPane.ERROR_MESSAGE);
                return;
            }

            loginButton.setEnabled(false);

            new Thread(() -> {
                try {
                    Socket socket = new Socket("localhost", 12345);
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    // процесс авторизации
                    in.readLine(); // "Введите логин:"
                    out.println(username);
                    in.readLine(); // "Введите пароль:"
                    out.println(password);

                    String response = in.readLine();

                    SwingUtilities.invokeLater(() -> {
                        if (response.startsWith("OK:")) {
                            String status = response.substring(3); // VIP или REGULAR

                            // открываем главное окно
                            MainWindow mainWindow = new MainWindow(socket, in, out, username, status);
                            mainWindow.setVisible(true);
                            dispose(); // закрываем окно входа
                        } else {
                            JOptionPane.showMessageDialog(LoginWindow.this,
                                    "Неверный логин или пароль", "Ошибка", JOptionPane.ERROR_MESSAGE);
                            loginButton.setEnabled(true);
                        }
                    });
                } catch (IOException ex) {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(LoginWindow.this,
                                "Не удалось подключиться к серверу", "Ошибка", JOptionPane.ERROR_MESSAGE);
                        loginButton.setEnabled(true);
                    });
                }
            }).start();
        }
    }
}