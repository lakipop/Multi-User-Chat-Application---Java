package com.chatapp.rmi;

import com.chatapp.model.entity.Chat;
import com.chatapp.model.entity.ChatSubscription;
import com.chatapp.model.entity.User;
import com.chatapp.service.AdminChatService;
import com.chatapp.service.AdminUserService;
import com.chatapp.rmi.UserClientCallback;

import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


 //Implementation of the admin remote interface

public class AdminRemoteImpl extends UnicastRemoteObject implements AdminRemoteInterface {

    private final AdminUserService adminUserService;
    private final AdminChatService adminChatService;
    private final Map<Long, AdminClientCallback> connectedAdmins;
    private final Map<Long, UserClientCallback> connectedUsers;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final AdminUserService userService = new AdminUserService();
    private final AdminChatService chatService = new AdminChatService();

    public AdminRemoteImpl() throws RemoteException {
        super();
        this.adminUserService = new AdminUserService();
        this.adminChatService = new AdminChatService();
        this.connectedAdmins = new ConcurrentHashMap<>();
        this.connectedUsers = new ConcurrentHashMap<>();
    }

    public AdminRemoteImpl(Map<Long, UserClientCallback> connectedUsers) throws RemoteException {
        super();
        this.adminUserService = new AdminUserService();
        this.adminChatService = new AdminChatService();
        this.connectedAdmins = new ConcurrentHashMap<>();
        this.connectedUsers = connectedUsers;
    }

    @Override
    public Map<String, Object> adminLogin(String username, String password) throws RemoteException {
        Optional<User> optionalAdmin = adminUserService.authenticateAdmin(username, password);

        if (optionalAdmin.isPresent()) {
            User admin = optionalAdmin.get();
            if (!admin.isAdmin()) {
                throw new RemoteException("User is not an admin");
            }

            Map<String, Object> adminData = new HashMap<>();
            adminData.put("id", admin.getId());
            adminData.put("email", admin.getEmail());
            adminData.put("username", admin.getUsername());
            adminData.put("nickName", admin.getNickName());
            adminData.put("isAdmin", true);

            return adminData;
        } else {
            throw new RemoteException("Invalid admin credentials");
        }
    }

    @Override
    public long createChat(String chatName) throws RemoteException {
        try {
            Chat chat = adminChatService.createChat(chatName);

            // Notify all connected admins about new chat
            for (AdminClientCallback callback : connectedAdmins.values()) {
                try {
                    Map<String, Object> chatData = new HashMap<>();
                    chatData.put("id", chat.getId());
                    chatData.put("name", chat.getName());
                    chatData.put("isActive", false);
                    chatData.put("createdAt", chat.getCreatedAt().format(formatter));

                    callback.chatActivityUpdate(chatData);
                } catch (RemoteException e) {
                    // Handle disconnected admin
                }
            }

            return chat.getId();
        } catch (Exception e) {
            throw new RemoteException("Failed to create chat: " + e.getMessage());
        }
    }

    @Override
    public void startChat(long chatId) throws RemoteException {
        try {
            Optional<Chat> optionalChat = adminChatService.getChatById(chatId);

            if (optionalChat.isPresent()) {
                Chat chat = adminChatService.startChat(optionalChat.get());

                // Prepare notification data
                Map<String, Object> chatData = new HashMap<>();
                chatData.put("chatId", chat.getId());
                chatData.put("chatName", chat.getName());
                chatData.put("startTime", chat.getStartedAt().format(formatter));

                // Notify all connected admins
                for (AdminClientCallback callback : connectedAdmins.values()) {
                    try {
                        callback.chatStarted(chatData);
                    } catch (RemoteException e) {
                        // Handle disconnected admin
                    }
                }

                // Notify all subscribed users about chat start
                for (ChatSubscription subscription : adminChatService.getChatSubscribers(chat)) {
                    User subscriber = subscription.getUser();
                    UserClientCallback callback = connectedUsers.get(subscriber.getId());

                    if (callback != null) {
                        try {
                            callback.chatStarted(chatData);
                        } catch (RemoteException e) {
                            // Client might be disconnected
                            connectedUsers.remove(subscriber.getId());
                        }
                    }
                }
            } else {
                throw new RemoteException("Chat not found");
            }
        } catch (IllegalStateException e) {
            throw new RemoteException(e.getMessage());
        }
    }

    @Override
    public void endChat(long chatId) throws RemoteException {
        try {
            Optional<Chat> optionalChat = adminChatService.getChatById(chatId);

            if (optionalChat.isPresent()) {
                Chat chat = optionalChat.get();

                if (chat.isActive()) {
                    Chat endedChat = adminChatService.endChat(chat);

                    // Prepare notification data
                    Map<String, Object> chatData = new HashMap<>();
                    chatData.put("chatId", endedChat.getId());
                    chatData.put("chatName", endedChat.getName());
                    chatData.put("endTime", endedChat.getEndedAt().format(formatter));

                    // Notify all connected admins
                    for (AdminClientCallback callback : connectedAdmins.values()) {
                        try {
                            callback.chatEnded(chatData);
                        } catch (RemoteException e) {
                            // Handle disconnected admin
                        }
                    }

                    // Notify all subscribed users about chat end
                    for (ChatSubscription subscription : adminChatService.getChatSubscribers(endedChat)) {
                        User subscriber = subscription.getUser();
                        UserClientCallback callback = connectedUsers.get(subscriber.getId());

                        if (callback != null) {
                            try {
                                callback.chatEnded(chatData);
                            } catch (RemoteException e) {
                                // Client might be disconnected
                                connectedUsers.remove(subscriber.getId());
                            }
                        }
                    }
                } else {
                    throw new RemoteException("Chat is not active");
                }
            } else {
                throw new RemoteException("Chat not found");
            }
        } catch (Exception e) {
            throw new RemoteException("Failed to end chat: " + e.getMessage());
        }
    }

    @Override
    public List<Map<String, Object>> getAllUsers() throws RemoteException {
        try {
            List<User> users = adminUserService.getAllUsers();
            List<Map<String, Object>> userDataList = new ArrayList<>();

            for (User user : users) {
                Map<String, Object> userData = new HashMap<>();
                userData.put("id", user.getId());
                userData.put("email", user.getEmail());
                userData.put("username", user.getUsername());
                userData.put("nickName", user.getNickName());
                userData.put("isAdmin", user.isAdmin());
                userData.put("hasProfilePicture", user.getProfilePicture() != null && user.getProfilePicture().length > 0);

                userDataList.add(userData);
            }

            return userDataList;
        } catch (Exception e) {
            throw new RemoteException("Failed to get users: " + e.getMessage());
        }
    }

    @Override
    public void removeUser(long userId) throws RemoteException {
        try {
            Optional<User> optionalUser = adminUserService.getUserById(userId);

            if (optionalUser.isPresent()) {
                User userToRemove = optionalUser.get();

                // Cannot remove the admin
                if (userToRemove.isAdmin()) {
                    throw new RemoteException("Cannot remove admin users");
                }

                adminUserService.deleteUser(userToRemove);

                // Notify connected admins about user removal
                for (AdminClientCallback callback : connectedAdmins.values()) {
                    try {
                        Map<String, Object> userData = new HashMap<>();
                        userData.put("id", userId);
                        userData.put("removed", true);

                        callback.userLeftChat(userData);
                    } catch (RemoteException e) {
                        // Handle disconnected admin
                    }
                }

                // If user is connected, disconnect them
                if (connectedUsers.containsKey(userId)) {
                    connectedUsers.remove(userId);
                }
            } else {
                throw new RemoteException("User not found");
            }
        } catch (Exception e) {
            throw new RemoteException("Failed to remove user: " + e.getMessage());
        }
    }

    @Override
    public List<Map<String, Object>> getAdminChatList() throws RemoteException {
        try {
            List<Chat> chats = adminChatService.getAllChats();
            List<Map<String, Object>> chatDataList = new ArrayList<>();

            for (Chat chat : chats) {
                Map<String, Object> chatData = new HashMap<>();
                chatData.put("id", chat.getId());
                chatData.put("name", chat.getName());
                chatData.put("isActive", chat.isActive());
                chatData.put("createdAt", chat.getCreatedAt().format(formatter));
                chatData.put("subscriberCount", adminChatService.getChatSubscribers(chat).size());

                if (chat.getStartedAt() != null) {
                    chatData.put("startedAt", chat.getStartedAt().format(formatter));
                }

                if (chat.getEndedAt() != null) {
                    chatData.put("endedAt", chat.getEndedAt().format(formatter));
                }

                chatDataList.add(chatData);
            }

            return chatDataList;
        } catch (Exception e) {
            throw new RemoteException("Failed to get chats: " + e.getMessage());
        }
    }

    @Override
    public void registerAdminClient(long adminId, AdminClientCallback callback) throws RemoteException {
        try {
            Optional<User> optionalAdmin = adminUserService.getUserById(adminId);

            if (optionalAdmin.isPresent() && optionalAdmin.get().isAdmin()) {
                connectedAdmins.put(adminId, callback);
            } else {
                throw new RemoteException("Not an admin user");
            }
        } catch (Exception e) {
            throw new RemoteException("Failed to register admin client: " + e.getMessage());
        }
    }

    @Override
    public void unregisterAdminClient(long adminId) throws RemoteException {
        connectedAdmins.remove(adminId);
    }
    @Override
    public void subscribeUserToChat(long userId, long chatId) throws RemoteException {
        try {
            Optional<User> userOpt = userService.getUserById(userId);
            Optional<Chat> chatOpt = chatService.getChatById(chatId);

            if (userOpt.isPresent() && chatOpt.isPresent()) {
                chatService.subscribeUserToChat(userOpt.get(), chatOpt.get());
            } else {
                throw new RemoteException("User or Chat not found.");
            }
        } catch (Exception e) {
            throw new RemoteException("Failed to subscribe user to chat: " + e.getMessage(), e);
        }
    }

    @Override
    public void unsubscribeUserFromChat(long userId, long chatId) throws RemoteException {
        try {
            Optional<User> userOpt = userService.getUserById(userId);
            Optional<Chat> chatOpt = chatService.getChatById(chatId);

            if (userOpt.isPresent() && chatOpt.isPresent()) {
                chatService.forceUnsubscribeUserFromChat(userOpt.get(), chatOpt.get());
            } else {
                throw new RemoteException("User or Chat not found.");
            }
        } catch (Exception e) {
            throw new RemoteException("Failed to unsubscribe user from chat: " + e.getMessage(), e);
        }
    }



    //Set the map of connected users from the user service

    public void setConnectedUsers(Map<Long, UserClientCallback> connectedUsers) {
        this.connectedUsers.clear();
        this.connectedUsers.putAll(connectedUsers);
    }
}