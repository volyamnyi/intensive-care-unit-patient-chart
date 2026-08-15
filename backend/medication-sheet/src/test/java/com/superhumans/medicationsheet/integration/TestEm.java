package com.superhumans.medicationsheet.integration;

import jakarta.persistence.EntityManager;

final class TestEm {

    private final EntityManager em;

    TestEm(EntityManager em) {
        this.em = em;
    }

    EntityManager getEntityManager() {
        return em;
    }

    <T> T persistFlushFind(T entity) {
        em.persist(entity);
        em.flush();
        em.refresh(entity);
        return entity;
    }

    void persist(Object entity) {
        em.persist(entity);
    }

    void persistAndFlush(Object entity) {
        em.persist(entity);
        em.flush();
    }

    <T> T find(Class<T> entityClass, Object id) {
        return em.find(entityClass, id);
    }

    void flush() {
        em.flush();
    }

    void clear() {
        em.clear();
    }
}