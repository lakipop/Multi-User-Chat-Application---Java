package com.chatapp.service;

import com.chatapp.model.dao.UserDAO;
import com.chatapp.model.entity.User;

import java.util.List;
import java.util.Optional;

/**
 * Service class for Admin User related operations
 */
public class AdminUserService {

    private final UserDAO userDAO;

    public AdminUserService() {
        this.userDAO = new UserDAO();
    }

    /**
     * Authenticate an admin user
     */
    public Optional<User> authenticateAdmin(String username, String password) {
        Optional<User> optionalUser = userDAO.findByUsername(username);

        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            if (user.getPassword().equals(password) && user.isAdmin()) {
                return Optional.of(user);
            }
        }

        return Optional.empty();
    }

    /**
     * Get user by ID
     */
    public Optional<User> getUserById(Long id) {
        return userDAO.findById(id);
    }

    /**
     * Get all users
     */
    public List<User> getAllUsers() {
        return userDAO.findAll();
    }

    /**
     * Delete a user
     */
    public void deleteUser(User user) {
        userDAO.delete(user);
    }

    /**
     * Get admin user
     */
    public Optional<User> getAdminUser() {
        return userDAO.findAdmin();
    }

    /**
     * Make a user an admin
     */
    public User makeAdmin(User user) {
        user.setAdmin(true);
        return userDAO.save(user);
    }

    /**
     * Remove admin privileges from a user
     */
    public User removeAdmin(User user) {
        // Make sure we're not removing the last admin
        List<User> adminUsers = userDAO.findAllAdmins();
        if (adminUsers.size() <= 1 && user.isAdmin()) {
            throw new IllegalStateException("Cannot remove the last admin");
        }

        user.setAdmin(false);
        return userDAO.save(user);
    }
}