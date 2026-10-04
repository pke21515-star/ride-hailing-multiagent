package io.github.pke21515.ride.core;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** 演示版领域模型（内存存储；生产替换为 MySQL + MyBatis/JPA）。 */
public class Models {

    public enum OrderStatus {
        CREATED, RISK_REJECTED, MATCHING, DISPATCHED, TRIP, PAID, COMPLETED, CANCELLED
    }

    /** 订单：全生命周期状态由 OrderAgent 管理 */
    public static class Order {
        public String id;
        public String passengerId;
        public String origin;
        public double originLat, originLng;
        public String destination;
        public double destLat, destLng;
        public OrderStatus status = OrderStatus.CREATED;
        public String driverId;
        // 路径规划结果
        public double distanceKm;
        public int durationMin;
        public int etaMin;          // 司机接驾预计分钟
        // 定价结果
        public double basePrice, mileageFee, durationFee, surgeMultiplier = 1.0, couponDeduction, finalPrice;
        public String couponId;
        public String rejectReason;
        public LocalDateTime createdAt = LocalDateTime.now();
        public String payMethod;
        public double refundAmount;

        public String priceBreakdown() {
            return String.format(
                "起步价 %.1f + 里程费 %.1f（%.1fkm） + 时长费 %.1f（%dmin） × 动态溢价 %.2f - 优惠券 %.1f = %.1f 元",
                basePrice, mileageFee, distanceKm, durationFee, durationMin, surgeMultiplier, couponDeduction, finalPrice);
        }
    }

    /** 司机：匹配打分的四个维度 */
    public static class Driver {
        public String id;
        public String name;
        public double lat, lng;
        public double rating;        // 服务分 4.0~5.0
        public double acceptRate;    // 接单率 0.6~1.0
        public boolean idle = true;
        public int todayOrders;

        public Driver(String id, String name, double lat, double lng, double rating, double acceptRate) {
            this.id = id; this.name = name; this.lat = lat; this.lng = lng;
            this.rating = rating; this.acceptRate = acceptRate;
        }
    }

    /** 优惠券：满减 */
    public static class Coupon {
        public String id;
        public double threshold;   // 使用门槛
        public double amount;      // 抵扣金额
        public boolean used = false;

        public Coupon(String id, double threshold, double amount) {
            this.id = id; this.threshold = threshold; this.amount = amount;
        }
    }

    public static final List<String> TOPICS_FLOW = List.of(
        "order.created", "risk.passed", "risk.rejected", "driver.matched",
        "route.planned", "price.quoted", "order.paid", "order.cancelled");
}
