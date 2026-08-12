package com.superhumans.medicationsheet.repository;


import com.superhumans.medicationsheet.entity.TelegramSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TelegramSubscriptionRepository extends JpaRepository<TelegramSubscription, Long> {
}
