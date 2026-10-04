package io.github.pke21515.ride.repository;

import io.github.pke21515.ride.entity.DriverEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DriverRepo extends JpaRepository<DriverEntity, String> {
}
