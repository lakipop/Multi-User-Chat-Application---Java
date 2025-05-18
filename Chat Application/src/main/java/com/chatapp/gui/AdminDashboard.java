package com.chatapp.gui;

import com.chatapp.rmi.AdminClientCallback;
import com.chatapp.rmi.AdminRemoteInterface;
import com.chatapp.rmi.UserClientCallback;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.Map;
import java.util.Vector;

/**
 * Admin Dashboard GUI for the Chat Application
 */
public class AdminDashboard extends JFrame implements AdminClientCallback {

    private static final int WIDTH = 900;
    private static final int HEIGHT = 700;

    private final long adminId;
    private final AdminRemoteInterface adminService;
    private AdminClientCallback callbackStub;

    private JTabbedPane tabbedPane;
    private JTable usersTable;
    private DefaultTableModel usersTableModel;
    private JTable chatsTable;
    private DefaultTableModel chatsTableModel;
    private JButton createChatButton;
    private JButton startChatButton;
    private JButton endChatButton;  // Added button to end chat
    private JButton subscribeUserButton;
    private JButton unsubscribeUserButton;
    private JButton removeUserButton;
    private JButton refreshButton;

    private boolean isInChat = false;
    private ChatFrame chatFrame;

    public AdminDashboard(long adminId, AdminRemoteInterface adminService) {
        this.adminId = adminId;
        this.adminService = adminService;

        setupCallbacks();
        setupUI();
        setVisible(true);
        loadData();
    }

    private void setupCallbacks() {
        try {
            callbackStub = (AdminClientCallback) UnicastRemoteObject.exportObject(this, 0);
            adminService.registerAdminClient(adminId, callbackStub);

            // Add shutdown hook to unregister client
            addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    try {
                        adminService.unregisterAdminClient(adminId);
                        UnicastRemoteObject.unexportObject(AdminDashboard.this, true);
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
        setTitle("Chat Application - Admin Dashboard");
        setSize(WIDTH, HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Create the tabbed pane
        tabbedPane = new JTabbedPane();

        // Create Users panel
        JPanel usersPanel = createUsersPanel();
        tabbedPane.addTab("Users", usersPanel);

        // Create Chats panel
        JPanel chatsPanel = createChatsPanel();
        tabbedPane.addTab("Chats", chatsPanel);

        getContentPane().add(tabbedPane);
    }

    private JPanel createUsersPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Create the table model with column names
        String[] columnNames = {"ID", "Email", "Username", "Nick Name", "Admin"};
        usersTableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        // Create the table
        usersTable = new JTable(usersTableModel);
        usersTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Set column widths
        usersTable.getColumnModel().getColumn(0).setPreferredWidth(30);
        usersTable.getColumnModel().getColumn(1).setPreferredWidth(150);
        usersTable.getColumnModel().getColumn(2).setPreferredWidth(100);
        usersTable.getColumnModel().getColumn(3).setPreferredWidth(100);
        usersTable.getColumnModel().getColumn(4).setPreferredWidth(50);

        // Create scroll pane for the table
        JScrollPane scrollPane = new JScrollPane(usersTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Create buttons panel
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        removeUserButton = new JButton("Remove User");
        refreshButton = new JButton("Refresh");

        buttonsPanel.add(removeUserButton);
        buttonsPanel.add(refreshButton);

        panel.add(buttonsPanel, BorderLayout.SOUTH);

        // Add action listeners
        removeUserButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                removeSelectedUser();
            }
        });

        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                loadUsersData();
            }
        });

        return panel;
    }

    private JPanel createChatsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Create top panel with create chat controls
        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        topPanel.setBorder(new TitledBorder("Create New Chat"));

        JPanel createChatPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel chatNameLabel = new JLabel("Chat Name:");
        final JTextField chatNameField = new JTextField(20);
        createChatButton = new JButton("Create Chat");

        createChatPanel.add(chatNameLabel);
        createChatPanel.add(chatNameField);
        createChatPanel.add(createChatButton);

        topPanel.add(createChatPanel, BorderLayout.NORTH);

        panel.add(topPanel, BorderLayout.NORTH);

        // Create the table model with column names
        String[] columnNames = {"ID", "Name", "Created At", "Status", "Subscribers"};
        chatsTableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        // Create the table
        chatsTable = new JTable(chatsTableModel);
        chatsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Set column widths
        chatsTable.getColumnModel().getColumn(0).setPreferredWidth(30);
        chatsTable.getColumnModel().getColumn(1).setPreferredWidth(150);
        chatsTable.getColumnModel().getColumn(2).setPreferredWidth(150);
        chatsTable.getColumnModel().getColumn(3).setPreferredWidth(70);
        chatsTable.getColumnModel().getColumn(4).setPreferredWidth(80);

        // Create scroll pane for the table
        JScrollPane scrollPane = new JScrollPane(chatsTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Create buttons panel
        JPanel buttonsPanel = new JPanel(new GridLayout(2, 3, 10, 10));
        buttonsPanel.setBorder(new EmptyBorder(10, 0, 0, 0));

        startChatButton = new JButton("Start Selected Chat");
        endChatButton = new JButton("End Selected Chat");
        subscribeUserButton = new JButton("Subscribe User to Chat");
        unsubscribeUserButton = new JButton("Unsubscribe User from Chat");
        JButton refreshChatsButton = new JButton("Refresh");

        buttonsPanel.add(startChatButton);
        buttonsPanel.add(endChatButton);
        buttonsPanel.add(subscribeUserButton);
        buttonsPanel.add(unsubscribeUserButton);
        buttonsPanel.add(refreshChatsButton);

        panel.add(buttonsPanel, BorderLayout.SOUTH);

        // Add action listeners
        createChatButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String chatName = chatNameField.getText().trim();
                if (!chatName.isEmpty()) {
                    createNewChat(chatName);
                    chatNameField.setText("");
                } else {
                    JOptionPane.showMessageDialog(AdminDashboard.this,
                            "Please enter a chat name",
                            "Input Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        startChatButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                startSelectedChat();
            }
        });

        endChatButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                endSelectedChat();
            }
        });

        subscribeUserButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showSubscriptionDialog(true);
            }
        });

        unsubscribeUserButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showSubscriptionDialog(false);
            }
        });

        refreshChatsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                loadChatsData();
            }
        });

        return panel;
    }

    private void loadData() {
        loadUsersData();
        loadChatsData();
    }

    private void loadUsersData() {
        try {
            List<Map<String, Object>> users = adminService.getAllUsers();

            // Clear table model
            usersTableModel.setRowCount(0);

            // Add users to table
            for (Map<String, Object> user : users) {
                Vector<Object> row = new Vector<>();
                row.add(user.get("id"));
                row.add(user.get("email"));
                row.add(user.get("username"));
                row.add(user.get("nickName"));
                row.add(user.get("isAdmin"));

                usersTableModel.addRow(row);
            }
        } catch (RemoteException e) {
            JOptionPane.showMessageDialog(this,
                    "Failed to load users: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadChatsData() {
        try {
            List<Map<String, Object>> chats = adminService.getAdminChatList();

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

                row.add(chat.get("subscriberCount"));

                chatsTableModel.addRow(row);
            }
        } catch (RemoteException e) {
            JOptionPane.showMessageDialog(this,
                    "Failed to load chats: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void removeSelectedUser() {
        int selectedRow = usersTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                    "Please select a user to remove",
                    "Selection Required",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        long userId = (long) usersTableModel.getValueAt(selectedRow, 0);
        boolean isAdmin = (boolean) usersTableModel.getValueAt(selectedRow, 4);

        if (isAdmin) {
            JOptionPane.showMessageDialog(this,
                    "Cannot remove admin user",
                    "Operation Not Allowed",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to remove this user?",
                "Confirm Removal",
                JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                adminService.removeUser(userId);
                loadUsersData();
                JOptionPane.showMessageDialog(this,
                        "User removed successfully",
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (RemoteException e) {
                JOptionPane.showMessageDialog(this,
                        "Failed to remove user: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void createNewChat(String chatName) {
        try {
            long chatId = adminService.createChat(chatName);
            loadChatsData();
            JOptionPane.showMessageDialog(this,
                    "Chat '" + chatName + "' created successfully",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (RemoteException e) {
            JOptionPane.showMessageDialog(this,
                    "Failed to create chat: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void startSelectedChat() {
        int selectedRow = chatsTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                    "Please select a chat to start",
                    "Selection Required",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        final long chatId = (long) chatsTableModel.getValueAt(selectedRow, 0);
        final String chatName = (String) chatsTableModel.getValueAt(selectedRow, 1);
        String status = (String) chatsTableModel.getValueAt(selectedRow, 3);

        if ("Active".equals(status)) {
            JOptionPane.showMessageDialog(this,
                    "This chat is already active",
                    "Information",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // Show loading indicator
        final JDialog loadingDialog = new JDialog(this, "Starting Chat", true);
        loadingDialog.add(new JLabel("Starting chat, please wait..."));
        loadingDialog.setSize(300, 100);
        loadingDialog.setLocationRelativeTo(this);

        // Run in background thread
        new Thread(() -> {
            try {
                // Show loading dialog in EDT
                SwingUtilities.invokeLater(() -> loadingDialog.setVisible(true));

                // Make remote call in background thread
                adminService.startChat(chatId);

                // Update UI in EDT
                SwingUtilities.invokeLater(() -> {
                    loadingDialog.dispose();
                    loadChatsData();
                    JOptionPane.showMessageDialog(AdminDashboard.this,
                            "Chat '" + chatName + "' started successfully",
                            "Success",
                            JOptionPane.INFORMATION_MESSAGE);
                });
            } catch (RemoteException e) {
                SwingUtilities.invokeLater(() -> {
                    loadingDialog.dispose();
                    JOptionPane.showMessageDialog(AdminDashboard.this,
                            "Failed to start chat: " + e.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                });
            }
        }).start();
    }

    private void endSelectedChat() {
        int selectedRow = chatsTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                    "Please select a chat to end",
                    "Selection Required",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        final long chatId = (long) chatsTableModel.getValueAt(selectedRow, 0);
        final String chatName = (String) chatsTableModel.getValueAt(selectedRow, 1);
        String status = (String) chatsTableModel.getValueAt(selectedRow, 3);

        if (!"Active".equals(status)) {
            JOptionPane.showMessageDialog(this,
                    "This chat is not active",
                    "Information",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to end this chat?",
                "Confirm End Chat",
                JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            // Show loading indicator
            final JDialog loadingDialog = new JDialog(this, "Ending Chat", true);
            loadingDialog.add(new JLabel("Ending chat, please wait..."));
            loadingDialog.setSize(300, 100);
            loadingDialog.setLocationRelativeTo(this);

            // Run in background thread
            new Thread(() -> {
                try {
                    // Show loading dialog in EDT
                    SwingUtilities.invokeLater(() -> loadingDialog.setVisible(true));

                    // Make remote call in background thread
                    adminService.endChat(chatId);

                    // Update UI in EDT
                    SwingUtilities.invokeLater(() -> {
                        loadingDialog.dispose();
                        loadChatsData();
                    });
                } catch (RemoteException e) {
                    SwingUtilities.invokeLater(() -> {
                        loadingDialog.dispose();
                        JOptionPane.showMessageDialog(AdminDashboard.this,
                                "Failed to end chat: " + e.getMessage(),
                                "Error",
                                JOptionPane.ERROR_MESSAGE);
                    });
                }
            }).start();
        }
    }
    private void showSubscriptionDialog(boolean subscribe) {
        int selectedRow = chatsTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                    "Please select a chat first",
                    "Selection Required",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        long chatId = (long) chatsTableModel.getValueAt(selectedRow, 0);

        try {
            List<Map<String, Object>> users = adminService.getAllUsers();
            if (users.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "No users available",
                        "Information",
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            String[] userNames = new String[users.size()];
            long[] userIds = new long[users.size()];

            for (int i = 0; i < users.size(); i++) {
                Map<String, Object> user = users.get(i);
                userNames[i] = user.get("username") + " (" + user.get("nickName") + ")";
                userIds[i] = (long) user.get("id");
            }

            String selectedUser = (String) JOptionPane.showInputDialog(
                    this,
                    subscribe ? "Select user to subscribe to chat:" : "Select user to unsubscribe from chat:",
                    subscribe ? "Subscribe User" : "Unsubscribe User",
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    userNames,
                    userNames[0]);

            if (selectedUser != null) {
                int index = -1;
                for (int i = 0; i < userNames.length; i++) {
                    if (userNames[i].equals(selectedUser)) {
                        index = i;
                        break;
                    }
                }

                if (index != -1) {
                    long userId = userIds[index];

                    if (subscribe) {
                        adminService.subscribeUserToChat(userId, chatId);
                        JOptionPane.showMessageDialog(this,
                                "User subscribed to chat successfully.",
                                "Success",
                                JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        adminService.unsubscribeUserFromChat(userId, chatId);
                        JOptionPane.showMessageDialog(this,
                                "User unsubscribed from chat successfully.",
                                "Success",
                                JOptionPane.INFORMATION_MESSAGE);
                    }
                }
            }
        } catch (RemoteException e) {
            JOptionPane.showMessageDialog(this,
                    "Operation failed: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }


    // AdminClientCallback implementation
    @Override
    public void userJoinedChat(Map<String, Object> userData) throws RemoteException {
        // Update UI or show notification as needed
        loadChatsData(); // Refresh to show updated subscriber count
    }

    @Override
    public void userLeftChat(Map<String, Object> userData) throws RemoteException {
        // Update UI or show notification as needed
        loadChatsData(); // Refresh to show updated subscriber count
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

        JOptionPane.showMessageDialog(this,
                "Chat '" + chatName + "' has ended at " + endTime,
                "Chat Ended",
                JOptionPane.INFORMATION_MESSAGE);

        loadChatsData();
    }

    @Override
    public void userRegistered(Map<String, Object> userData) throws RemoteException {
        // Load user data to show the newly registered user
        loadUsersData();

        String username = (String) userData.get("username");
        JOptionPane.showMessageDialog(this,
                "New user registered: " + username,
                "User Registered",
                JOptionPane.INFORMATION_MESSAGE);
    }

    @Override
    public void chatActivityUpdate(Map<String, Object> activityData) throws RemoteException {
        // Update any activity metrics or refresh the chat list
        loadChatsData();
    }


    public void chatClosed() {
        isInChat = false;
        chatFrame = null;

        // Optional: Re-enable any UI elements that should be available when not in a chat
        // For example, you might want to re-enable certain buttons:
        startChatButton.setEnabled(true);

        // Refresh the chat list to update any changes
        loadChatsData();
    }
}