package com.chatapp.model.dao;

import com.chatapp.model.entity.Chat;
import com.chatapp.model.entity.ChatSubscription;
import com.chatapp.model.entity.User;
import com.chatapp.util.HibernateUtil;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for ChatSubscription entity
 */
public class ChatSubscriptionDAO {

    /**
     * Save a new subscription or update an existing one
     */
    public ChatSubscription save(ChatSubscription subscription) {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            if (subscription.getId() == null) {
                em.persist(subscription);
            } else {
                subscription = em.merge(subscription);
            }
            em.getTransaction().commit();
            return subscription;
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
     * Find subscription by ID
     */
    public Optional<ChatSubscription> findById(Long id) {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            ChatSubscription subscription = em.find(ChatSubscription.class, id);
            return Optional.ofNullable(subscription);
        } finally {
            em.close();
        }
    }

    /**
     * Find subscription by user and chat
     */
    public Optional<ChatSubscription> findByUserAndChat(User user, Chat chat) {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<ChatSubscription> query = cb.createQuery(ChatSubscription.class);
            Root<ChatSubscription> root = query.from(ChatSubscription.class);

            Predicate userPredicate = cb.equal(root.get("user"), user);
            Predicate chatPredicate = cb.equal(root.get("chat"), chat);

            query.select(root).where(cb.and(userPredicate, chatPredicate));

            TypedQuery<ChatSubscription> typedQuery = em.createQuery(query);
            try {
                ChatSubscription subscription = typedQuery.getSingleResult();
                return Optional.of(subscription);
            } catch (NoResultException e) {
                return Optional.empty();
            }
        } finally {
            em.close();
        }
    }

    /**
     * Find all active subscriptions for a chat
     */
    public List<ChatSubscription> findActiveSubscriptionsByChat(Chat chat) {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<ChatSubscription> query = cb.createQuery(ChatSubscription.class);
            Root<ChatSubscription> root = query.from(ChatSubscription.class);

            Predicate chatPredicate = cb.equal(root.get("chat"), chat);
            Predicate activePredicate = cb.equal(root.get("isActive"), true);

            query.select(root).where(cb.and(chatPredicate, activePredicate));

            return em.createQuery(query).getResultList();
        } finally {
            em.close();
        }
    }

    /**
     * Find all active subscriptions for a user
     */
    public List<ChatSubscription> findActiveSubscriptionsByUser(User user) {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<ChatSubscription> query = cb.createQuery(ChatSubscription.class);
            Root<ChatSubscription> root = query.from(ChatSubscription.class);

            Predicate userPredicate = cb.equal(root.get("user"), user);
            Predicate activePredicate = cb.equal(root.get("isActive"), true);

            query.select(root).where(cb.and(userPredicate, activePredicate));

            return em.createQuery(query).getResultList();
        } finally {
            em.close();
        }
    }

    /**
     * Find all subscriptions (active and inactive) for a chat
     */
    public List<ChatSubscription> findAllSubscriptionsByChat(Chat chat) {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<ChatSubscription> query = cb.createQuery(ChatSubscription.class);
            Root<ChatSubscription> root = query.from(ChatSubscription.class);

            query.select(root).where(cb.equal(root.get("chat"), chat));

            return em.createQuery(query).getResultList();
        } finally {
            em.close();
        }
    }

    /**
     * Find subscription history for a user
     */
    public List<ChatSubscription> findAllSubscriptionsByUser(User user) {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<ChatSubscription> query = cb.createQuery(ChatSubscription.class);
            Root<ChatSubscription> root = query.from(ChatSubscription.class);

            query.select(root).where(cb.equal(root.get("user"), user));

            return em.createQuery(query).getResultList();
        } finally {
            em.close();
        }
    }

    /**
     * Unsubscribe a user from a chat
     */
    public void unsubscribe(User user, Chat chat) {
        Optional<ChatSubscription> optionalSubscription = findByUserAndChat(user, chat);
        if (optionalSubscription.isPresent()) {
            ChatSubscription subscription = optionalSubscription.get();
            subscription.setActive(false);
            subscription.setUnsubscribedAt(LocalDateTime.now());
            save(subscription);
        }
    }

    /**
     * Delete a subscription (admin function)
     */
    public void delete(ChatSubscription subscription) {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            if (!em.contains(subscription)) {
                subscription = em.merge(subscription);
            }
            em.remove(subscription);
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

    /**
     * Find most active users (for admin analytics)
     */
    public List<Object[]> findMostActiveUsers(int limit) {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            String jpql = "SELECT cs.user, COUNT(cs) as subscriptionCount " +
                    "FROM ChatSubscription cs " +
                    "GROUP BY cs.user " +
                    "ORDER BY subscriptionCount DESC";

            TypedQuery<Object[]> query = em.createQuery(jpql, Object[].class);
            query.setMaxResults(limit);

            return query.getResultList();
        } finally {
            em.close();
        }
    }
}