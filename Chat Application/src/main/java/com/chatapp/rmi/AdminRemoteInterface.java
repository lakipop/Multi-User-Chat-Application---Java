package com.chatapp.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;


 //Remote interface for admin-specific chat operations

public interface AdminRemoteInterface extends Remote {


    // Authenticate an admin user

    Map<String, Object> adminLogin(String username, String password) throws RemoteException;


     //Create a new chat

    long createChat(String chatName) throws RemoteException;


     //Start a chat

    void startChat(long chatId) throws RemoteException;


    //End an active chat
    void endChat(long chatId) throws RemoteException;


     //Get all users in the system

    List<Map<String, Object>> getAllUsers() throws RemoteException;


     //Remove a user from the system

    void removeUser(long userId) throws RemoteException;


     // Get all chats in the system

    List<Map<String, Object>> getAdminChatList() throws RemoteException;


    //  Register admin client for receiving notifications

    void registerAdminClient(long adminId, AdminClientCallback callback) throws RemoteException;


    // Unregister admin client from receiving notifications

    void unregisterAdminClient(long adminId) throws RemoteException;
    // Subscribe a user to a chat
    void subscribeUserToChat(long userId, long chatId) throws RemoteException;

    // Unsubscribe (force) a user from a chat
    void unsubscribeUserFromChat(long userId, long chatId) throws RemoteException;

}