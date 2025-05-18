package com.chatapp.model.dao;

import com.chatapp.model.entity.Chat;
import com.chatapp.model.entity.User;
import com.chatapp.util.HibernateUtil;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.NonUniqueResultException;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for Chat entity
 */
public class ChatDAO {

    /**
     * Save a new chat or update an existing one
     */
    public Chat save(Chat chat) {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            if (chat.getId() == null) {
                em.persist(chat);
            } else {
                chat = em.merge(chat);
            }
            em.getTransaction().commit();
            return chat;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }

    /**
     * Find chat by ID
     */
    public Optional<Chat> findById(Long id) {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            Chat chat = em.find(Chat.class, id);
            return Optional.ofNullable(chat);
        } finally {
            em.close();
        }
    }

    /**
     * Get all chats
     */
    public List<Chat> findAll() {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Chat> query = cb.createQuery(Chat.class);
            Root<Chat> root = query.from(Chat.class);

            query.select(root);

            return em.createQuery(query).getResultList();
        } finally {
            em.close();
        }
    }

    /**
     * Find active chats and return as a list
     */
    public List<Chat> findActiveChats() {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Chat> query = cb.createQuery(Chat.class);
            Root<Chat> root = query.from(Chat.class);

            query.select(root).where(cb.equal(root.get("isActive"), true));

            return em.createQuery(query).getResultList();
        } finally {
            em.close();
        }
    }

    /**
     * Find active chat - returns Optional of the first active chat or empty if none found
     * Modified to handle cases where multiple active chats exist
     */
    public Optional<Chat> findActiveChat() {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Chat> query = cb.createQuery(Chat.class);
            Root<Chat> root = query.from(Chat.class);

            query.select(root).where(cb.equal(root.get("isActive"), true));

            TypedQuery<Chat> typedQuery = em.createQuery(query);
            try {
                Chat chat = typedQuery.getSingleResult();
                return Optional.of(chat);
            } catch (NoResultException e) {
                return Optional.empty();
            } catch (NonUniqueResultException e) {
                // Handle case where multiple active chats exist
                // Get the first one from the list
                List<Chat> activeChats = typedQuery.getResultList();
                if (!activeChats.isEmpty()) {
                    return Optional.of(activeChats.get(0));
                }
                return Optional.empty();
            }
        } finally {
            em.close();
        }
    }

    /**
     * Find chats by date range
     */
    public List<Chat> findByDateRange(LocalDateTime start, LocalDateTime end) {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Chat> query = cb.createQuery(Chat.class);
            Root<Chat> root = query.from(Chat.class);

            query.select(root).where(
                    cb.and(
                            cb.greaterThanOrEqualTo(root.get("startedAt"), start),
                            cb.lessThanOrEqualTo(root.get("startedAt"), end)
                    )
            );

            return em.createQuery(query).getResultList();
        } finally {
            em.close();
        }
    }

    /**
     * Delete a chat
     */
    public void delete(Chat chat) {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            if (!em.contains(chat)) {
                chat = em.merge(chat);
            }
            em.remove(chat);
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }
}