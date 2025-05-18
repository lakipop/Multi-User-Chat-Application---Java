package com.chatapp;

import com.chatapp.gui.LoginFrame;
import com.chatapp.rmi.RMIServer;

import javax.swing.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Main application class that launches both the RMI server and client UI
 * Automatically starts both components in the correct sequence
 */
public class ChatApp {

    public static void main(String[] args) {
        // Set system look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Create an executor service for the server
        ExecutorService executor = Executors.newSingleThreadExecutor();

        // Start both server and client automatically
        System.out.println("Starting Chat Application (Server + Client)...");

        // First start the server
        startServer(executor);

        try {
            // Wait for server to fully initialize
            System.out.println("Waiting for server to initialize...");
            Thread.sleep(3000); // 3-second delay
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Then start the client
        startClient();
    }

    /**
     * Starts the RMI server in a separate thread
     */
    private static void startServer(ExecutorService executor) {
        executor.submit(() -> {
            try {
                System.out.println("Starting RMI Server...");
                RMIServer.start();
            } catch (Exception e) {
                System.err.println("Error starting RMI server: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    /**
     * Starts the chat client UI
     */
    private static void startClient() {
        SwingUtilities.invokeLater(() -> {
            System.out.println("Starting chat client...");
            new LoginFrame();
        });
    }
}