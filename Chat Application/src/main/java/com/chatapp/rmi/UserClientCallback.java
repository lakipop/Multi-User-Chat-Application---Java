package com.chatapp.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;


 //Callback interface for user client notifications

public interface UserClientCallback extends Remote {


      //Receive a message from another user

    void receiveMessage(Map<String, Object> messageData) throws RemoteException;


    // Notification when another user joins the chat

    void userJoined(Map<String, Object> userData) throws RemoteException;


     //Notification when another user leaves the chat

    void userLeft(Map<String, Object> userData) throws RemoteException;


    // Notification when subscription to a chat changes

    void subscriptionChanged(boolean subscribed, long chatId) throws RemoteException;


    // Notification when a chat is started

    void chatStarted(Map<String, Object> chatData) throws RemoteException;


     //Notification when a chat is ended

    void chatEnded(Map<String, Object> chatData) throws RemoteException;


     //Notification when user is removed from the system

    void userRemoved() throws RemoteException;
}