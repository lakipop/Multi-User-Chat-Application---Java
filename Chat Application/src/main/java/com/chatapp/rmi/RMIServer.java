package com.chatapp.rmi;

import com.chatapp.rmi.AdminRemoteImpl;
import com.chatapp.rmi.AdminRemoteInterface;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RMIServer {
    private static final int PORT = 1099;
    private static final String USER_SERVICE_NAME = "UserService";
    private static final String ADMIN_SERVICE_NAME = "AdminService";

    public static void start() throws Exception {
        Map<Long, UserClientCallback> connectedUsers = new ConcurrentHashMap<>();
        UserRemoteInterface userService = new UserRemoteImpl();
        AdminRemoteInterface adminService = new AdminRemoteImpl(connectedUsers);

        if (adminService instanceof AdminRemoteImpl) {
            ((AdminRemoteImpl) adminService).setConnectedUsers(connectedUsers);
        }

        Registry registry = LocateRegistry.createRegistry(PORT);

        registry.rebind(USER_SERVICE_NAME, userService);
        registry.rebind(ADMIN_SERVICE_NAME, adminService);

        System.out.println("RMI Server started on port " + PORT);
        System.out.println("Service '" + USER_SERVICE_NAME + "' is ready for clients");
        System.out.println("Service '" + ADMIN_SERVICE_NAME + "' is ready for clients");
    }
}