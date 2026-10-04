package io.github.pke21515.ride.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** 用户：乘客 / 司机 / 管理员 三角色账号。 */
@Entity
@Table(name = "users")
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(unique = true, nullable = false, length = 32)
    public String phone;

    @Column(nullable = false, length = 128)
    public String passwordHash;

    @Column(nullable = false, length = 32)
    public String name;

    /** PASSENGER / DRIVER / ADMIN */
    @Column(nullable = false, length = 16)
    public String role;

    /** 司机账号关联的司机ID（如 D001），乘客/管理员为空 */
    @Column(length = 16)
    public String driverId;

    public LocalDateTime createdAt = LocalDateTime.now();
}
