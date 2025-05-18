
package com.chatapp.model.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Entity representing the subscription of a user to a chat
 */
@Entity
@Table(name = "chat_subscriptions", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "chat_id"}))
public class ChatSubscription implements Serializable {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @ManyToOne
    @JoinColumn(name = "chat_id", nullable = false)
    private Chat chat;
    
    @Column(name = "subscribed_at", nullable = false)
    private LocalDateTime subscribedAt;
    
    @Column(name = "unsubscribed_at")
    private LocalDateTime unsubscribedAt;
    
    @Column(name = "is_active")
    private boolean isActive = true;
    
    // Default constructor
    public ChatSubscription() {
    }
    
    // Constructor with fields
    public ChatSubscription(User user, Chat chat) {
        this.user = user;
        this.chat = chat;
        this.subscribedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
    }
    
    public Chat getChat() {
        return chat;
    }
    
    public void setChat(Chat chat) {
        this.chat = chat;
    }
    
    public LocalDateTime getSubscribedAt() {
        return subscribedAt;
    }
    
    public void setSubscribedAt(LocalDateTime subscribedAt) {
        this.subscribedAt = subscribedAt;
    }
    
    public LocalDateTime getUnsubscribedAt() {
        return unsubscribedAt;
    }
    
    public void setUnsubscribedAt(LocalDateTime unsubscribedAt) {
        this.unsubscribedAt = unsubscribedAt;
    }
    
    public boolean isActive() {
        return isActive;
    }
    
    public void setActive(boolean active) {
        isActive = active;
    }
}
