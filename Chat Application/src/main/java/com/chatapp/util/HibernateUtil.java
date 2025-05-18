
package com.chatapp.util;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

/**
 * Utility class for Hibernate operations
 */
public class HibernateUtil {
    private static final String PERSISTENCE_UNIT_NAME = "ChatAppPU";
    private static volatile EntityManagerFactory entityManagerFactory;

    public static EntityManager getEntityManager() {
        if (entityManagerFactory == null) {
            synchronized (HibernateUtil.class) {
                if (entityManagerFactory == null) {
                    try {
                        entityManagerFactory = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT_NAME);
                    } catch (Exception e) {
                        System.err.println("Failed to create EntityManagerFactory: " + e.getMessage());
                        throw new RuntimeException("Could not initialize JPA", e);
                    }
                }
            }
        }
        return entityManagerFactory.createEntityManager();
    }

    public static void shutdown() {
        if (entityManagerFactory != null && entityManagerFactory.isOpen()) {
            entityManagerFactory.close();
        }
    }
}
