package com.chatapp.gui;

import com.chatapp.rmi.UserClientCallback;
import com.chatapp.rmi.UserRemoteInterface;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.Map;
import java.util.Vector;

/**
 * User Dashboard GUI for the Chat Application
 */
public class UserDashboard extends JFrame implements UserClientCallback {

    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;

    private final long userId;
    private final UserRemoteInterface userService;
    private final String nickName;
    private UserClientCallback callbackStub;

    private JTabbedPane tabbedPane;
    private JTable chatsTable;
    private DefaultTableModel chatsTableModel;
    private JButton joinChatButton;
    private JButton subscribeButton;
    private JButton unsubscribeButton;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JTextField nickNameField;
    private JButton updateProfileButton;
    private JButton chooseImageButton;
    private JLabel imageLabel;
    private byte[] profilePictureBytes;

    private boolean isInChat = false;
    private ChatFrame chatFrame;

    public UserDashboard(long userId, UserRemoteInterface userService, String nickName) {
        this.userId = userId;
        this.userService = userService;
        this.nickName = nickName;

        setupCallbacks();
        setupUI();
        setVisible(true);
        loadChatsData();
    }

    private void setupCallbacks() {
        try {
            callbackStub = (UserClientCallback) UnicastRemoteObject.exportObject(this, 0);
            userService.registerClient(userId, callbackStub);

            // Add shutdown hook to unregister client
            addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    try {
                        userService.unregisterClient(userId);
                        if (isInChat) {
                            try {
                                userService.leaveChat(userId);
                            } catch (Exception ex) {
                                // Ignore if already left
                            }
                        }
                        UnicastRemoteObject.unexportObject(UserDashboard.this, true);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            });

        } catch (RemoteException e) {
            JOptionPane.showMessageDialog(this,
                    "Failed to register for notifications: " + e.getMessage(),
                    "Connection Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void setupUI() {
        setTitle("Chat Application - User Dashboard");
        setSize(WIDTH, HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);


        tabbedPane = new JTabbedPane();


        JPanel chatsPanel = createChatsPanel();
        tabbedPane.addTab("Chats", chatsPanel);


        JPanel profilePanel = createProfilePanel();
        tabbedPane.addTab("My Profile", profilePanel);

        getContentPane().add(tabbedPane);
    }

    private JPanel createChatsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));


        JLabel welcomeLabel = new JLabel("Welcome, " + nickName + "!");
        welcomeLabel.setFont(new Font("Arial", Font.BOLD, 16));
        panel.add(welcomeLabel, BorderLayout.NORTH);


        String[] columnNames = {"ID", "Name", "Created At", "Status"};
        chatsTableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };


        chatsTable = new JTable(chatsTableModel);
        chatsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Set column widths
        chatsTable.getColumnModel().getColumn(0).setPreferredWidth(30);
        chatsTable.getColumnModel().getColumn(1).setPreferredWidth(150);
        chatsTable.getColumnModel().getColumn(2).setPreferredWidth(150);
        chatsTable.getColumnModel().getColumn(3).setPreferredWidth(100);

        // Create scroll pane for the table
        JScrollPane scrollPane = new JScrollPane(chatsTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Create buttons panel
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        joinChatButton = new JButton("Join Active Chat");
        subscribeButton = new JButton("Subscribe to Chat");
        unsubscribeButton = new JButton("Unsubscribe from Chat");
        JButton refreshButton = new JButton("Refresh");

        buttonsPanel.add(joinChatButton);
        buttonsPanel.add(subscribeButton);
        buttonsPanel.add(unsubscribeButton);
        buttonsPanel.add(refreshButton);

        panel.add(buttonsPanel, BorderLayout.SOUTH);

        // Add action listeners
        joinChatButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                joinActiveChat();
            }
        });

        subscribeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                subscribeToChat();
            }
        });

        unsubscribeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                unsubscribeFromChat();
            }
        });

        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                loadChatsData();
            }
        });

        return panel;
    }

    private JPanel createProfilePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(new TitledBorder("Update Your Profile"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // Username field
        gbc.gridx = 0;
        gbc.gridy = 0;
        JLabel usernameLabel = new JLabel("Username:");
        formPanel.add(usernameLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        usernameField = new JTextField(20);
        formPanel.add(usernameField, gbc);

        // Password field
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.0;
        JLabel passwordLabel = new JLabel("Password:");
        formPanel.add(passwordLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        passwordField = new JPasswordField(20);
        formPanel.add(passwordField, gbc);

        // Nick Name field
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0.0;
        JLabel nickNameLabel = new JLabel("Nick Name:");
        formPanel.add(nickNameLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        nickNameField = new JTextField(20);
        formPanel.add(nickNameField, gbc);

        // Profile Picture
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weightx = 0.0;
        JLabel pictureLabel = new JLabel("Profile Picture:");
        formPanel.add(pictureLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        JPanel picturePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        chooseImageButton = new JButton("Choose Image");
        imageLabel = new JLabel("No image selected");
        picturePanel.add(chooseImageButton);
        picturePanel.add(imageLabel);
        formPanel.add(picturePanel, gbc);

        // Update button
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        updateProfileButton = new JButton("Update Profile");
        formPanel.add(updateProfileButton, gbc);

        panel.add(formPanel, BorderLayout.CENTER);

        // Add action listeners
        chooseImageButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                selectProfilePicture();
            }
        });

        updateProfileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateProfile();
            }
        });

        // Set initial values
        nickNameField.setText(nickName);

        // Load initial profile data
        loadUserProfile();

        return panel;
    }

    private void loadUserProfile() {
        try {
            Map<String, Object> profile = userService.getUserProfile(userId);
            usernameField.setText((String) profile.get("username"));
            nickNameField.setText((String) profile.get("nickName"));

        } catch (RemoteException e) {
            JOptionPane.showMessageDialog(this,
                    "Failed to load profile data: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadChatsData() {
        try {
            List<Map<String, Object>> chats = userService.getAllChats();

            // Clear table model
            chatsTableModel.setRowCount(0);

            // Add chats to table
            for (Map<String, Object> chat : chats) {
                Vector<Object> row = new Vector<>();
                row.add(chat.get("id"));
                row.add(chat.get("name"));
                row.add(chat.get("createdAt"));

                boolean isActive = (boolean) chat.get("isActive");
                String status = isActive ? "Active" : "Inactive";
                row.add(status);

                chatsTableModel.addRow(row);
            }
        } catch (RemoteException e) {
            JOptionPane.showMessageDialog(this,
                    "Failed to load chats: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void joinActiveChat() {
        if (isInChat) {
            JOptionPane.showMessageDialog(this,
                    "You are already in a chat",
                    "Information",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        try {
            Map<String, Object> chatData = userService.joinChat(userId);

            String chatName = (String) chatData.get("chatName");
            long chatId = (long) chatData.get("chatId");
            String startTime = (String) chatData.get("startTime");

            // Add the missing isAdmin parameter (false for regular users)
            chatFrame = new ChatFrame(chatId, chatName, userId, nickName, userService, this, false);
            chatFrame.appendSystemMessage("Chat started at: " + startTime);
            chatFrame.setVisible(true);
            isInChat = true;

        } catch (RemoteException e) {
            JOptionPane.showMessageDialog(this,
                    "Failed to join chat: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void subscribeToChat() {
        int selectedRow = chatsTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                    "Please select a chat to subscribe to",
                    "Selection Required",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        long chatId = (long) chatsTableModel.getValueAt(selectedRow, 0);
        String chatName = (String) chatsTableModel.getValueAt(selectedRow, 1);

        // Disable button during operation
        subscribeButton.setEnabled(false);

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                userService.subscribeToChat(userId, chatId);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get(); // Check for exceptions
                    JOptionPane.showMessageDialog(UserDashboard.this,
                            "Successfully subscribed to chat '" + chatName + "'",
                            "Success",
                            JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(UserDashboard.this,
                            "Failed to subscribe to chat: " + e.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                } finally {
                    subscribeButton.setEnabled(true);
                }
            }
        }.execute();
    }

    private void unsubscribeFromChat() {
        int selectedRow = chatsTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                    "Please select a chat to unsubscribe from",
                    "Selection Required",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        final long chatId = (long) chatsTableModel.getValueAt(selectedRow, 0);
        final String chatName = (String) chatsTableModel.getValueAt(selectedRow, 1);

        // Disable button during operation
        unsubscribeButton.setEnabled(false);

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                userService.unsubscribeFromChat(userId, chatId);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get(); // Check for exceptions
                    JOptionPane.showMessageDialog(UserDashboard.this,
                            "Successfully unsubscribed from chat '" + chatName + "'",
                            "Success",
                            JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(UserDashboard.this,
                            "Failed to unsubscribe from chat: " + e.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                } finally {
                    unsubscribeButton.setEnabled(true);
                }
            }
        }.execute();
    }

    private void selectProfilePicture() {
        JFileChooser fileChooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
                "Image Files", "jpg", "jpeg", "png", "gif");
        fileChooser.setFileFilter(filter);

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try {
                BufferedImage img = ImageIO.read(selectedFile);
                if (img != null) {
                    // Resize image if needed
                    BufferedImage resizedImg = resizeImage(img, 64, 64);

                    // Convert to byte array
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ImageIO.write(resizedImg, "jpg", baos);
                    profilePictureBytes = baos.toByteArray();

                    // Update preview label
                    imageLabel.setText(selectedFile.getName());

                    // Optional: Show small preview
                    ImageIcon icon = new ImageIcon(resizedImg);
                    imageLabel.setIcon(icon);
                }
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this,
                        "Error loading image: " + e.getMessage(),
                        "Image Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private BufferedImage resizeImage(BufferedImage originalImage, int targetWidth, int targetHeight) {
        BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics2D = resizedImage.createGraphics();
        graphics2D.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
        graphics2D.dispose();
        return resizedImage;
    }

    private void updateProfile() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        String nickName = nickNameField.getText().trim();

        // Validate inputs
        if (username.isEmpty() || password.isEmpty() || nickName.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Username, password, and nick name are required",
                    "Validation Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            userService.updateUserProfile(userId, username, password, nickName, profilePictureBytes);
            JOptionPane.showMessageDialog(this,
                    "Profile updated successfully",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE);

            // Clear fields
            passwordField.setText("");
            profilePictureBytes = null;
            imageLabel.setIcon(null);
            imageLabel.setText("No image selected");

        } catch (RemoteException e) {
            JOptionPane.showMessageDialog(this,
                    "Failed to update profile: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    public void chatClosed() {
        isInChat = false;
        chatFrame = null;
    }

    // UserClientCallback implementation
    @Override
    public void receiveMessage(Map<String, Object> messageData) throws RemoteException {
        if (isInChat && chatFrame != null) {
            String nickName = (String) messageData.get("nickName");
            String message = (String) messageData.get("message");
            String timestamp = (String) messageData.get("timestamp");

            chatFrame.appendUserMessage(nickName, message, timestamp);
        }
    }

    @Override
    public void userJoined(Map<String, Object> userData) throws RemoteException {
        if (isInChat && chatFrame != null) {
            String nickName = (String) userData.get("nickName");
            String timestamp = (String) userData.get("timestamp");

            chatFrame.appendSystemMessage("\"" + nickName + "\" has joined : " + timestamp);
        }
    }

    @Override
    public void userLeft(Map<String, Object> userData) throws RemoteException {
        if (isInChat && chatFrame != null) {
            String nickName = (String) userData.get("nickName");
            String timestamp = (String) userData.get("timestamp");

            chatFrame.appendSystemMessage("\"" + nickName + "\" left : " + timestamp);
        }
    }

    @Override
    public void chatStarted(Map<String, Object> chatData) throws RemoteException {
        String chatName = (String) chatData.get("chatName");
        String startTime = (String) chatData.get("startTime");

        JOptionPane.showMessageDialog(this,
                "Chat '" + chatName + "' has started at " + startTime,
                "Chat Started",
                JOptionPane.INFORMATION_MESSAGE);

        loadChatsData();
    }

    @Override
    public void chatEnded(Map<String, Object> chatData) throws RemoteException {
        String chatName = (String) chatData.get("chatName");
        String endTime = (String) chatData.get("endTime");

        if (isInChat && chatFrame != null) {
            chatFrame.appendSystemMessage("Chat stopped at: " + endTime);
            chatFrame.disableChat();
        }

        JOptionPane.showMessageDialog(this,
                "Chat '" + chatName + "' has ended at " + endTime,
                "Chat Ended",
                JOptionPane.INFORMATION_MESSAGE);

        loadChatsData();
        isInChat = false;
    }

    @Override
    public void subscriptionChanged(boolean subscribed, long chatId) throws RemoteException {
        loadChatsData();

        String chatName = "the chat";
        for (int i = 0; i < chatsTableModel.getRowCount(); i++) {
            if ((long) chatsTableModel.getValueAt(i, 0) == chatId) {
                chatName = (String) chatsTableModel.getValueAt(i, 1);
                break;
            }
        }

        JOptionPane.showMessageDialog(this,
                subscribed ?
                        "You have been subscribed to '" + chatName + "'" :
                        "You have been unsubscribed from '" + chatName + "'",
                "Subscription Changed",
                JOptionPane.INFORMATION_MESSAGE);
    }

    @Override
    public void userRemoved() throws RemoteException {
        JOptionPane.showMessageDialog(this,
                "Your account has been removed by the administrator.",
                "Account Removed",
                JOptionPane.WARNING_MESSAGE);

        // Close this window
        this.dispose();
    }
}