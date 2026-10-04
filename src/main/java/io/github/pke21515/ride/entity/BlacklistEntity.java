package io.github.pke21515.ride.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** 风控黑名单：管理端可增删，持久化。 */
@Entity
@Table(name = "blacklist")
public class BlacklistEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(unique = true, nullable = false, length = 32)
    public String passengerId;

    @Column(length = 128)
    public String reason;

    public LocalDateTime createdAt = LocalDateTime.now();
}
