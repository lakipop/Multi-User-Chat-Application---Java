package com.chatapp.gui;

import com.chatapp.rmi.AdminRemoteInterface;
import com.chatapp.rmi.UserRemoteInterface;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Map;

/**
 * Login GUI for the Chat Application - Updated for separated admin/user architecture
 */
public class LoginFrame extends JFrame {

    private static final int width = 500;
    private static final int height = 350;
    private static final String Rmihost = "localhost";
    private static final int rmiport = 1099;
    private static final String userservicename = "UserService";
    private static final String adminservicename = "AdminService";

    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JButton adminLoginButton;
    private JButton registerButton;
    private JLabel statusLabel;

    private UserRemoteInterface userService;
    private AdminRemoteInterface adminService;

    public LoginFrame() {
        initializeRMI();
        setupUI();
        setVisible(true);
    }

    private void initializeRMI() {
        try {
            Registry registry = LocateRegistry.getRegistry(Rmihost, rmiport);
            userService = (UserRemoteInterface) registry.lookup(userservicename);
            adminService = (AdminRemoteInterface) registry.lookup(adminservicename);
        } catch (RemoteException | NotBoundException e) {
            JOptionPane.showMessageDialog(this,
                    "Failed to connect to the chat server: " + e.getMessage(),
                    "Connection Error",
                    JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    private void setupUI() {
        setTitle("Chat Application - Login");
        setSize(width, height);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel("Chat Application Login", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        mainPanel.add(titleLabel, BorderLayout.NORTH);

        JPanel formPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        formPanel.setBorder(new EmptyBorder(20, 0, 20, 0));

        JLabel usernameLabel = new JLabel("Username:");
        usernameField = new JTextField();
        JLabel passwordLabel = new JLabel("Password:");
        passwordField = new JPasswordField();

        formPanel.add(usernameLabel);
        formPanel.add(usernameField);
        formPanel.add(passwordLabel);
        formPanel.add(passwordField);

        mainPanel.add(formPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new GridLayout(4, 1, 0, 10));
        loginButton = new JButton("User Login");
        adminLoginButton = new JButton("Admin Login");
        registerButton = new JButton("Register");
        statusLabel = new JLabel("", JLabel.CENTER);

        buttonPanel.add(loginButton);
        buttonPanel.add(adminLoginButton);
        buttonPanel.add(registerButton);
        buttonPanel.add(statusLabel);

        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleUserLogin();
            }
        });

        adminLoginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleAdminLogin();
            }
        });

        registerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openRegistrationForm();
            }
        });

        add(mainPanel);
    }

    private void handleUserLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Username and password are required");
            return;
        }

        try {
            Map<String, Object> userData = userService.login(username, password);

            // Open user dashboard
            long userId = (long) userData.get("id");
            String nickName = (String) userData.get("nickName");

            SwingUtilities.invokeLater(() -> {
                new UserDashboard(userId, userService, nickName);
                dispose(); // Close the login window
            });

        } catch (RemoteException ex) {
            statusLabel.setText("Login failed: " + ex.getMessage());
        }
    }

    private void handleAdminLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Username and password are required");
            return;
        }

        try {
            Map<String, Object> adminData = adminService.adminLogin(username, password);

            // Open admin dashboard
            long adminId = (long) adminData.get("id");

            SwingUtilities.invokeLater(() -> {
                new AdminDashboard(adminId, adminService);
                dispose(); // Close the login window
            });

        } catch (RemoteException ex) {
            statusLabel.setText("Admin login failed: " + ex.getMessage());
        }
    }

    private void openRegistrationForm() {
        SwingUtilities.invokeLater(() -> {
            new RegistrationFrame(userService, this);
   });
}
}