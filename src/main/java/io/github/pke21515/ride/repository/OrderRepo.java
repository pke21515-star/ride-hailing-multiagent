package io.github.pke21515.ride.repository;

import io.github.pke21515.ride.entity.OrderEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrderRepo extends JpaRepository<OrderEntity, String> {

    List<OrderEntity> findByPassengerIdOrderByCreatedAtDesc(String passengerId);

    List<OrderEntity> findByDriverIdAndStatusInOrderByCreatedAtDesc(String driverId, List<String> statuses);

    Page<OrderEntity> findByStatus(String status, Pageable pageable);

    long countByStatus(String status);
}
