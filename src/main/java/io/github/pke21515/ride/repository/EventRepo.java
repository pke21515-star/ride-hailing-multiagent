package io.github.pke21515.ride.repository;

import io.github.pke21515.ride.entity.EventEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepo extends JpaRepository<EventEntity, Long> {
    Page<EventEntity> findAllByOrderByIdDesc(Pageable pageable);
}
