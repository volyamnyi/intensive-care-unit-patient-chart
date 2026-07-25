package com.superhumans.repository;

import com.superhumans.entity.TelegramSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TelegramSubscriptionRepository extends JpaRepository<TelegramSubscription, Long> {
}
