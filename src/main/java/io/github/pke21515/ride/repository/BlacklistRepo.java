package io.github.pke21515.ride.repository;

import io.github.pke21515.ride.entity.BlacklistEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BlacklistRepo extends JpaRepository<BlacklistEntity, Long> {
    boolean existsByPassengerId(String passengerId);
}
