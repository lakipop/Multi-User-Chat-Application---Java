package com.chatapp.gui;

import com.chatapp.rmi.UserRemoteInterface;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.rmi.RemoteException;

/**
 * Registration GUI for the Chat Application
 */
public class RegistrationFrame extends JFrame {

    private static final int WIDTH = 500;
    private static final int HEIGHT = 500;

    private JTextField emailField;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JPasswordField confirmPasswordField;
    private JTextField nickNameField;
    private JButton chooseImageButton;
    private JLabel imageLabel;
    private JButton registerButton;
    private JButton cancelButton;
    private JLabel statusLabel;

    private UserRemoteInterface userService;
    private LoginFrame parentFrame;
    private byte[] profilePicture;

    public RegistrationFrame(UserRemoteInterface userService, LoginFrame parentFrame) {
        this.userService = userService;
        this.parentFrame = parentFrame;
        setupUI();
        setVisible(true);
    }

    private void setupUI() {
        setTitle("Chat Application - Registration");
        setSize(WIDTH, HEIGHT);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(parentFrame);
        setResizable(false);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel("User Registration", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        mainPanel.add(titleLabel, BorderLayout.NORTH);

        JPanel formPanel = new JPanel(new GridLayout(6, 2, 10, 10));
        formPanel.setBorder(new EmptyBorder(20, 0, 20, 0));

        // Email
        JLabel emailLabel = new JLabel("Email:");
        emailField = new JTextField();
        formPanel.add(emailLabel);
        formPanel.add(emailField);

        // Username
        JLabel usernameLabel = new JLabel("Username:");
        usernameField = new JTextField();
        formPanel.add(usernameLabel);
        formPanel.add(usernameField);

        // Password
        JLabel passwordLabel = new JLabel("Password:");
        passwordField = new JPasswordField();
        formPanel.add(passwordLabel);
        formPanel.add(passwordField);

        // Confirm Password
        JLabel confirmPasswordLabel = new JLabel("Confirm Password:");
        confirmPasswordField = new JPasswordField();
        formPanel.add(confirmPasswordLabel);
        formPanel.add(confirmPasswordField);

        // Nickname
        JLabel nickNameLabel = new JLabel("Nickname:");
        nickNameField = new JTextField();
        formPanel.add(nickNameLabel);
        formPanel.add(nickNameField);

        // Profile Picture
        JLabel profilePictureLabel = new JLabel("Profile Picture:");
        JPanel imagePanel = new JPanel(new BorderLayout(5, 0));
        chooseImageButton = new JButton("Choose Image");
        imageLabel = new JLabel("No image selected", JLabel.CENTER);
        imagePanel.add(chooseImageButton, BorderLayout.WEST);
        imagePanel.add(imageLabel, BorderLayout.CENTER);
        formPanel.add(profilePictureLabel);
        formPanel.add(imagePanel);

        mainPanel.add(formPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new GridLayout(3, 1, 0, 10));
        registerButton = new JButton("Register");
        cancelButton = new JButton("Cancel");
        statusLabel = new JLabel("", JLabel.CENTER);

        buttonPanel.add(registerButton);
        buttonPanel.add(cancelButton);
        buttonPanel.add(statusLabel);

        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        // Action listeners
        registerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleRegistration();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        chooseImageButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                selectProfilePicture();
            }
        });

        add(mainPanel);
    }

    private void selectProfilePicture() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select Profile Picture");
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
                "Image files", "jpg", "jpeg", "png", "gif");
        fileChooser.setFileFilter(filter);

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            imageLabel.setText(selectedFile.getName());

            try {
                profilePicture = Files.readAllBytes(selectedFile.toPath());
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this,
                        "Failed to read image file: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                profilePicture = null;
                imageLabel.setText("No image selected");
            }
        }
    }

    private void handleRegistration() {
        String email = emailField.getText().trim();
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        String confirmPassword = new String(confirmPasswordField.getPassword());
        String nickName = nickNameField.getText().trim();

        // Validate input
        if (email.isEmpty() || username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() || nickName.isEmpty()) {
            statusLabel.setText("All fields are required");
            return;
        }

        if (!password.equals(confirmPassword)) {
            statusLabel.setText("Passwords don't match");
            return;
        }

        // Simple email validation
        if (!email.contains("@") || !email.contains(".")) {
            statusLabel.setText("Invalid email format");
            return;
        }

        try {
            long userId = userService.registerUser(email, username, password, nickName, profilePicture);
            JOptionPane.showMessageDialog(this,
                    "Registration successful! Your user ID is: " + userId,
                    "Registration Complete",
                    JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } catch (RemoteException ex) {
            statusLabel.setText("Registration failed: " + ex.getMessage());
        }
    }

}