package com.chatapp.gui;

import com.chatapp.rmi.UserRemoteInterface;
import com.chatapp.rmi.AdminRemoteInterface;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.rmi.RemoteException;

/**
 * Chat Interface GUI for the Chat Application
 * Works with both admin and user remote interfaces
 */
public class ChatFrame extends JFrame {

    private static final int WIDTH = 600;
    private static final int HEIGHT = 500;

    private final long chatId;
    private final String chatName;
    private final long userId;
    private final String nickName;
    private final Object remoteService; // Can be either UserRemoteInterface or AdminRemoteInterface
    private final Object parent;
    private final boolean isAdmin;

    private JTextPane chatTextPane;
    private JTextField messageField;
    private JButton sendButton;
    private JLabel statusLabel;

    private SimpleAttributeSet userStyle;
    private SimpleAttributeSet systemStyle;
    private SimpleAttributeSet selfStyle;

    /**
     * Constructor for ChatFrame that works with either user or admin service
     */
    public ChatFrame(long chatId, String chatName, long userId, String nickName,
                     Object remoteService, Object parent, boolean isAdmin) {
        this.chatId = chatId;
        this.chatName = chatName;
        this.userId = userId;
        this.nickName = nickName;
        this.remoteService = remoteService;
        this.parent = parent;
        this.isAdmin = isAdmin;

        initStyles();
        setupUI();

        // Handle window closing
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                leaveChat();
            }
        });
    }

    private void initStyles() {
        userStyle = new SimpleAttributeSet();
        StyleConstants.setForeground(userStyle, Color.BLUE);
        StyleConstants.setBold(userStyle, true);

        systemStyle = new SimpleAttributeSet();
        StyleConstants.setForeground(systemStyle, Color.GRAY);
        StyleConstants.setItalic(systemStyle, true);

        selfStyle = new SimpleAttributeSet();
        StyleConstants.setForeground(selfStyle, Color.GREEN.darker());
        StyleConstants.setBold(selfStyle, true);
    }

    private void setupUI() {
        setTitle("Chat: " + chatName);
        setSize(WIDTH, HEIGHT);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Chat display area
        chatTextPane = new JTextPane();
        chatTextPane.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(chatTextPane);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // Message input area
        JPanel inputPanel = new JPanel(new BorderLayout(5, 0));
        messageField = new JTextField();
        sendButton = new JButton("Send");
        inputPanel.add(messageField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        // Status area
        statusLabel = new JLabel("Connected as: " + nickName + (isAdmin ? " (Admin)" : ""));

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(inputPanel, BorderLayout.CENTER);
        bottomPanel.add(statusLabel, BorderLayout.SOUTH);

        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        // Add action listeners
        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });

        messageField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });

        add(mainPanel);
    }

    private void sendMessage() {
        String message = messageField.getText().trim();
        if (message.isEmpty()) {
            return;
        }

        // Clear field immediately for better UX
        messageField.setText("");
        messageField.requestFocus();

        // Execute RMI call in background thread
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                try {
                    if (isAdmin) {
                        appendSystemMessage("Admin messaging not implemented in this version");
                    } else {
                        // User message sending
                        ((UserRemoteInterface)remoteService).sendMessage(userId, message);
                    }

                    // If message is "Bye", close the chat window
                    if ("Bye".equalsIgnoreCase(message)) {
                        SwingUtilities.invokeLater(() -> {
                            notifyParentChatClosed();
                            dispose();
                        });
                    }
                } catch (RemoteException e) {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(ChatFrame.this,
                                "Failed to send message: " + e.getMessage(),
                                "Error",
                                JOptionPane.ERROR_MESSAGE);
                    });
                }
                return null;
            }
        }.execute();
    }

    private void leaveChat() {
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                try {
                    if (isAdmin) {

                    } else {
                        // User leaving chat
                        ((UserRemoteInterface)remoteService).leaveChat(userId);
                    }
                } catch (RemoteException e) {
                    // Log error but continue with closing
                    System.err.println("Error leaving chat: " + e.getMessage());
                }
                return null;
            }

            @Override
            protected void done() {
                notifyParentChatClosed();
            }
        }.execute();
    }

    private void notifyParentChatClosed() {
        if (parent instanceof AdminDashboard) {
            ((AdminDashboard) parent).chatClosed();
        } else if (parent instanceof UserDashboard) {
            ((UserDashboard) parent).chatClosed();
        }
    }

    public void appendSystemMessage(String message) {
        appendToChat(message + "\n", systemStyle);
    }

    public void appendUserMessage(String userName, String message, String timestamp) {
        SimpleAttributeSet style = userName.equals(nickName) ? selfStyle : userStyle;
        appendToChat("[" + timestamp + "] " + userName + ": ", style);
        appendToChat(message + "\n", null);
    }

    public void appendToChat(String text, SimpleAttributeSet style) {
        Document doc = chatTextPane.getDocument();
        try {
            doc.insertString(doc.getLength(), text, style);
            chatTextPane.setCaretPosition(doc.getLength());
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    public void disableChat() {
        SwingUtilities.invokeLater(() -> {
            messageField.setEnabled(false);
            sendButton.setEnabled(false);
            statusLabel.setText("Chat has ended");

            // Ensure UI updates are processed
            revalidate();
            repaint();
        });
    }
}