package com.chatapp.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.List;


 // Callback interface for admin client notifications

public interface AdminClientCallback extends Remote {


     //Notification when a user joins a chat

    void userJoinedChat(Map<String, Object> userData) throws RemoteException;


      //Notification when a user leaves a chat

    void userLeftChat(Map<String, Object> userData) throws RemoteException;


     //Notification when a chat is started

    void chatStarted(Map<String, Object> chatData) throws RemoteException;

    //Notification when a chat is ended

    void chatEnded(Map<String, Object> chatData) throws RemoteException;


     //Notification when a new user registers

    void userRegistered(Map<String, Object> userData) throws RemoteException;


     //Notification of chat activity metrics

    void chatActivityUpdate(Map<String, Object> activityData) throws RemoteException;
}