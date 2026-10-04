package io.github.pke21515.ride.repository;

import io.github.pke21515.ride.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepo extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByPhone(String phone);
}
