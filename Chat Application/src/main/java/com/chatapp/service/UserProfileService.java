package com.chatapp.service;

import com.chatapp.model.dao.UserDAO;
import com.chatapp.model.entity.User;

import java.util.List;
import java.util.Optional;

/**
 * Service class for User profile related operations
 */
public class UserProfileService {

    private final UserDAO userDAO;

    public UserProfileService() {
        this.userDAO = new UserDAO();
    }

    /**
     * Register a new user
     */
    public User register(String email, String username, String password, String nickName, byte[] profilePicture) throws Exception {
        // Check if email is already taken
        if (userDAO.findByEmail(email).isPresent()) {
            throw new Exception("Email is already registered");
        }

        // Check if username is already taken
        if (userDAO.findByUsername(username).isPresent()) {
            throw new Exception("Username is already taken");
        }

        // Create and save new user
        User user = new User(email, username, password, nickName);
        user.setProfilePicture(profilePicture);

        // Check if this is the first user, make them admin if so
        if (userDAO.findAll().isEmpty()) {
            user.setAdmin(true);
        }

        return userDAO.save(user);
    }

    /**
     * Authenticate a user
     */
    public Optional<User> authenticate(String username, String password) {
        Optional<User> optionalUser = userDAO.findByUsername(username);

        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            if (user.getPassword().equals(password)) {
                return Optional.of(user);
            }
        }

        return Optional.empty();
    }

    /**
     * Update user profile
     */
    public User updateProfile(User user, String username, String password, String nickName, byte[] profilePicture) throws Exception {
        // Check if username is already taken by someone else
        Optional<User> existingUser = userDAO.findByUsername(username);
        if (existingUser.isPresent() && !existingUser.get().getId().equals(user.getId())) {
            throw new Exception("Username is already taken");
        }

        user.setUsername(username);
        user.setPassword(password);
        user.setNickName(nickName);
        if (profilePicture != null) {
            user.setProfilePicture(profilePicture);
        }

        return userDAO.save(user);
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
}