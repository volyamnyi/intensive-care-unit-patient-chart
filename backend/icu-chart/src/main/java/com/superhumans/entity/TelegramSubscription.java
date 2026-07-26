package com.superhumans.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import java.time.LocalDateTime;

@Entity
@Table(name = "telegram_subscriptions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TelegramSubscription {

    @Id
    @Column(name = "chat_id")
    Long chatId;

    @Column(name = "subscribed_at", nullable = false)
    LocalDateTime subscribedAt;

    @PrePersist
    void prePersist() {
        if (subscribedAt == null) {
            subscribedAt = LocalDateTime.now();
        }
    }
}
