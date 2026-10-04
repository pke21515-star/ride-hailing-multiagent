package io.github.pke21515.ride.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** 订单：全生命周期持久化，重启不丢。 */
@Entity
@Table(name = "orders")
public class OrderEntity {

    @Id
    @Column(length = 24)
    public String id;

    @Column(nullable = false, length = 32)
    public String passengerId;

    @Column(length = 64)
    public String origin;
    public Double originLat, originLng;

    @Column(nullable = false, length = 64)
    public String destination;
    public Double destLat, destLng;

    /** CREATED / RISK_REJECTED / MATCHING / DISPATCHED / TRIP / PAID / COMPLETED / CANCELLED */
    @Column(nullable = false, length = 20)
    public String status;

    @Column(length = 16)
    public String driverId;

    public Double distanceKm;
    public Integer durationMin;
    public Integer etaMin;

    public Double basePrice, mileageFee, durationFee, surgeMultiplier, couponDeduction, finalPrice;
    @Column(length = 16)
    public String couponId;
    @Column(length = 128)
    public String rejectReason;

    public LocalDateTime createdAt = LocalDateTime.now();
    @Column(length = 24)
    public String payMethod;
    public Double refundAmount;
}
