package com.chatapp.rmi;

import com.chatapp.model.entity.Chat;
import com.chatapp.model.entity.ChatSubscription;
import com.chatapp.model.entity.User;
import com.chatapp.service.UserChatService;
import com.chatapp.service.UserProfileService;

import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of the user remote interface
 */
public class UserRemoteImpl extends UnicastRemoteObject implements UserRemoteInterface {

    private final UserProfileService userProfileService;
    private final UserChatService userChatService;
    private final Map<Long, UserClientCallback> connectedUsers;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public UserRemoteImpl() throws RemoteException {
        super();
        this.userProfileService = new UserProfileService();
        this.userChatService = new UserChatService();
        this.connectedUsers = new ConcurrentHashMap<>();
    }

    @Override
    public long registerUser(String email, String username, String password, String nickName, byte[] profilePicture) throws RemoteException {
        try {
            User user = userProfileService.register(email, username, password, nickName, profilePicture);
            return user.getId();
        } catch (Exception e) {
            throw new RemoteException("Registration failed: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> login(String username, String password) throws RemoteException {
        Optional<User> optionalUser = userProfileService.authenticate(username, password);

        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            Map<String, Object> userData = new HashMap<>();
            userData.put("id", user.getId());
            userData.put("email", user.getEmail());
            userData.put("username", user.getUsername());
            userData.put("nickName", user.getNickName());
            userData.put("isAdmin", user.isAdmin());

            return userData;
        } else {
            throw new RemoteException("Invalid credentials");
        }
    }

    @Override
    public void sendMessage(long userId, String message) throws RemoteException {
        Optional<User> optionalUser = userProfileService.getUserById(userId);
        Optional<Chat> optionalActiveChat = userChatService.getActiveChat();

        if (optionalUser.isPresent() && optionalActiveChat.isPresent()) {
            User user = optionalUser.get();
            Chat activeChat = optionalActiveChat.get();

            if (userChatService.isUserSubscribedToChat(user, activeChat)) {
                try {

                    userChatService.appendMessageToChatTranscript(activeChat, user, message);


                    if ("Bye".equalsIgnoreCase(message.trim())) {
                        leaveChat(userId);
                        return;
                    }

                    // Broadcast message to all participants
                    Map<String, Object> messageData = new HashMap<>();
                    messageData.put("userId", user.getId());
                    messageData.put("nickName", user.getNickName());
                    messageData.put("message", message);
                    messageData.put("timestamp", LocalDateTime.now().format(formatter));
                    messageData.put("hasProfilePicture", user.getProfilePicture() != null && user.getProfilePicture().length > 0);

                    for (ChatSubscription subscription : userChatService.getChatSubscribers(activeChat)) {
                        User subscriber = subscription.getUser();
                        UserClientCallback callback = connectedUsers.get(subscriber.getId());

                        if (callback != null) {
                            try {
                                callback.receiveMessage(messageData);
                            } catch (RemoteException e) {

                                connectedUsers.remove(subscriber.getId());  // Client might be disconnected, remove from connected clients
                            }
                        }
                    }
                } catch (IOException e) {
                    throw new RemoteException("Failed to record message: " + e.getMessage());
                }
            } else {
                throw new RemoteException("You are not subscribed to the active chat");
            }
        } else {
            throw new RemoteException("Invalid user ID or no active chat");
        }
    }

    @Override
    public void subscribeToChat(long userId, long chatId) throws RemoteException {
        Optional<User> optionalUser = userProfileService.getUserById(userId);
        Optional<Chat> optionalChat = userChatService.getChatById(chatId);

        if (optionalUser.isPresent() && optionalChat.isPresent()) {
            User user = optionalUser.get();
            Chat chat = optionalChat.get();

            userChatService.subscribeUserToChat(user, chat);

            // Notify the user about subscription change
            UserClientCallback callback = connectedUsers.get(userId);
            if (callback != null) {
                try {
                    callback.subscriptionChanged(true, chatId);
                } catch (RemoteException e) {
                    connectedUsers.remove(userId);
                }
            }
        } else {
            throw new RemoteException("Invalid user ID or chat ID");
        }
    }

    @Override
    public void unsubscribeFromChat(long userId, long chatId) throws RemoteException {
        Optional<User> optionalUser = userProfileService.getUserById(userId);
        Optional<Chat> optionalChat = userChatService.getChatById(chatId);

        if (optionalUser.isPresent() && optionalChat.isPresent()) {
            User user = optionalUser.get();
            Chat chat = optionalChat.get();

            userChatService.unsubscribeUserFromChat(user, chat);

            // Notify the user about subscription change
            UserClientCallback callback = connectedUsers.get(userId);
            if (callback != null) {
                try {
                    callback.subscriptionChanged(false, chatId);
                } catch (RemoteException e) {
                    connectedUsers.remove(userId);
                }
            }
        } else {
            throw new RemoteException("Invalid user ID or chat ID");
        }
    }


    @Override
    public List<Map<String, Object>> getAllChats() throws RemoteException {
        try {
            List<Chat> chats = userChatService.getAllChats(); // You'll need to implement this method in UserChatService
            List<Map<String, Object>> chatDataList = new ArrayList<>();

            for (Chat chat : chats) {
                Map<String, Object> chatData = new HashMap<>();
                chatData.put("id", chat.getId());
                chatData.put("name", chat.getName());
                chatData.put("isActive", chat.isActive());
                chatData.put("createdAt", chat.getCreatedAt().format(formatter));

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
            throw new RemoteException("Failed to get all chats: " + e.getMessage());
        }
    }


    @Override
    public List<Map<String, Object>> getUserChats(long userId) throws RemoteException {
        Optional<User> optionalUser = userProfileService.getUserById(userId);

        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            List<Chat> chats = userChatService.getUserSubscribedChats(user);
            List<Map<String, Object>> chatDataList = new ArrayList<>();

            for (Chat chat : chats) {
                Map<String, Object> chatData = new HashMap<>();
                chatData.put("id", chat.getId());
                chatData.put("name", chat.getName());
                chatData.put("isActive", chat.isActive());
                chatData.put("createdAt", chat.getCreatedAt().format(formatter));

                if (chat.getStartedAt() != null) {
                    chatData.put("startedAt", chat.getStartedAt().format(formatter));
                }

                if (chat.getEndedAt() != null) {
                    chatData.put("endedAt", chat.getEndedAt().format(formatter));
                }

                chatDataList.add(chatData);
            }

            return chatDataList;
        } else {
            throw new RemoteException("Invalid user ID");
        }
    }

    @Override
    public void registerClient(long userId, UserClientCallback callback) throws RemoteException {
        Optional<User> optionalUser = userProfileService.getUserById(userId);

        if (optionalUser.isPresent()) {
            connectedUsers.put(userId, callback);
        } else {
            throw new RemoteException("Invalid user ID");
        }
    }

    @Override
    public void unregisterClient(long userId) throws RemoteException {
        connectedUsers.remove(userId);
    }

    @Override
    public Map<String, Object> joinChat(long userId) throws RemoteException {
        Optional<User> optionalUser = userProfileService.getUserById(userId);
        Optional<Chat> optionalActiveChat = userChatService.getActiveChat();

        if (optionalUser.isPresent() && optionalActiveChat.isPresent()) {
            User user = optionalUser.get();
            Chat activeChat = optionalActiveChat.get();

            if (userChatService.isUserSubscribedToChat(user, activeChat)) {
                try {

                    userChatService.recordUserJoinedChat(activeChat, user);   // Record user join in chat transcript

                    // Prepare response data
                    Map<String, Object> chatData = new HashMap<>();
                    chatData.put("chatId", activeChat.getId());
                    chatData.put("chatName", activeChat.getName());
                    chatData.put("startTime", activeChat.getStartedAt().format(formatter));

                    // Notify other participants about the user joining
                    Map<String, Object> userData = new HashMap<>();
                    userData.put("userId", user.getId());
                    userData.put("nickName", user.getNickName());
                    userData.put("timestamp", LocalDateTime.now().format(formatter));
                    userData.put("hasProfilePicture", user.getProfilePicture() != null && user.getProfilePicture().length > 0);

                    for (ChatSubscription subscription : userChatService.getChatSubscribers(activeChat)) {
                        User subscriber = subscription.getUser();
                        if (!subscriber.getId().equals(user.getId())) {
                            UserClientCallback callback = connectedUsers.get(subscriber.getId());

                            if (callback != null) {
                                try {
                                    callback.userJoined(userData);
                                } catch (RemoteException e) {
                                    connectedUsers.remove(subscriber.getId());
                                }
                            }
                        }
                    }

                    return chatData;
                } catch (IOException e) {
                    throw new RemoteException("Failed to join chat: " + e.getMessage());
                }
            } else {
                throw new RemoteException("You are not subscribed to the active chat");
            }
        } else {
            throw new RemoteException("Invalid user ID or no active chat");
        }
    }

    @Override
    public void leaveChat(long userId) throws RemoteException {
        Optional<User> optionalUser = userProfileService.getUserById(userId);
        Optional<Chat> optionalActiveChat = userChatService.getActiveChat();

        if (optionalUser.isPresent() && optionalActiveChat.isPresent()) {
            User user = optionalUser.get();
            Chat activeChat = optionalActiveChat.get();

            try {
                // Record user leaving in chat transcript
                userChatService.recordUserLeftChat(activeChat, user);

                // Notify other participants about the user leaving
                Map<String, Object> userData = new HashMap<>();
                userData.put("userId", user.getId());
                userData.put("nickName", user.getNickName());
                userData.put("timestamp", LocalDateTime.now().format(formatter));

                for (ChatSubscription subscription : userChatService.getChatSubscribers(activeChat)) {
                    User subscriber = subscription.getUser();
                    if (!subscriber.getId().equals(user.getId())) {
                        UserClientCallback callback = connectedUsers.get(subscriber.getId());

                        if (callback != null) {
                            try {
                                callback.userLeft(userData);
                            } catch (RemoteException e) {
                                connectedUsers.remove(subscriber.getId());
                            }
                        }
                    }
                }

                // Check if this was the last user in the chat
                List<ChatSubscription> activeSubscribers = userChatService.getChatSubscribers(activeChat);
                boolean anyOtherActive = false;

                for (ChatSubscription subscription : activeSubscribers) {
                    if (!subscription.getUser().getId().equals(user.getId()) &&
                            connectedUsers.containsKey(subscription.getUser().getId())) {
                        anyOtherActive = true;
                        break;
                    }
                }

                if (!anyOtherActive) {
                    // End the chat since this was the last active user
                    Chat endedChat = userChatService.endChat(activeChat);

                    // Notify the user who left about chat ending
                    UserClientCallback callback = connectedUsers.get(userId);
                    if (callback != null) {
                        Map<String, Object> chatData = new HashMap<>();
                        chatData.put("chatId", endedChat.getId());
                        chatData.put("chatName", endedChat.getName());
                        chatData.put("endTime", endedChat.getEndedAt().format(formatter));

                        try {
                            callback.chatEnded(chatData);
                        } catch (RemoteException e) {
                            connectedUsers.remove(userId);
                        }
                    }
                }
            } catch (IOException e) {
                throw new RemoteException("Failed to leave chat: " + e.getMessage());
            }
        } else {
            throw new RemoteException("Invalid user ID or no active chat");
        }
    }

    @Override
    public void updateUserProfile(long userId, String username, String password, String nickName, byte[] profilePicture) throws RemoteException {
        Optional<User> optionalUser = userProfileService.getUserById(userId);

        if (optionalUser.isPresent()) {
            User user = optionalUser.get();

            try {
                userProfileService.updateProfile(user, username, password, nickName, profilePicture);
            } catch (Exception e) {
                throw new RemoteException("Failed to update profile: " + e.getMessage());
            }
        } else {
            throw new RemoteException("Invalid user ID");
        }
    }

    @Override
    public Map<String, Object> getUserProfile(long userId) throws RemoteException {
        Optional<User> optionalUser = userProfileService.getUserById(userId);

        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            Map<String, Object> profileData = new HashMap<>();
            profileData.put("id", user.getId());
            profileData.put("email", user.getEmail());
            profileData.put("username", user.getUsername());
            profileData.put("nickName", user.getNickName());
            profileData.put("isAdmin", user.isAdmin());
            profileData.put("hasProfilePicture", user.getProfilePicture() != null && user.getProfilePicture().length > 0);

            return profileData;
        } else {
            throw new RemoteException("Invalid user ID");
        }
    }

    @Override
    public byte[] getUserProfilePicture(long userId) throws RemoteException {
        Optional<User> optionalUser = userProfileService.getUserById(userId);

        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            return user.getProfilePicture();
        } else {
            throw new RemoteException("Invalid user ID");
        }
    }
}