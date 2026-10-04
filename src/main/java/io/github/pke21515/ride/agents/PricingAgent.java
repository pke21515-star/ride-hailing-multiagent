package io.github.pke21515.ride.agents;

import io.github.pke21515.ride.core.Agent;
import io.github.pke21515.ride.core.DemoStore;
import io.github.pke21515.ride.core.Event;
import io.github.pke21515.ride.core.EventBus;
import io.github.pke21515.ride.core.Models;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 定价 Agent：起步价 + 里程费 + 时长费，动态溢价（供需比），优惠券抵扣。 */
@Component
public class PricingAgent implements Agent {

    private static final double BASE_PRICE = 10.0;   // 起步价（含 3km）
    private static final double BASE_KM = 3.0;
    private static final double PER_KM = 2.4;        // 里程费
    private static final double PER_MIN = 0.6;       // 时长费
    private static final double MIN_FARE = 10.0;     // 最低消费
    private static final double MAX_SURGE = 2.0;     // 溢价上限（防止舆情，行业惯例封顶）

    private final DemoStore store;
    private final EventBus bus;

    public PricingAgent(DemoStore store, EventBus bus) {
        this.store = store; this.bus = bus;
        bus.register(this);
    }

    @Override public String name() { return "定价Agent"; }
    @Override public List<String> topics() { return List.of("route.planned"); }

    @Override
    public synchronized void onEvent(Event e) {
        Models.Order o = store.orders.get(e.orderId());
        if (o == null || o.status == Models.OrderStatus.CANCELLED
                || o.status == Models.OrderStatus.RISK_REJECTED) return;

        // 1) 基础价格
        o.basePrice = BASE_PRICE;
        o.mileageFee = o.distanceKm <= BASE_KM ? 0 : round1((o.distanceKm - BASE_KM) * PER_KM);
        o.durationFee = round1(o.durationMin * PER_MIN);
        double subtotal = o.basePrice + o.mileageFee + o.durationFee;

        // 2) 动态溢价：供需比 = 周边待匹配订单数 / 空闲司机数，向上取整映射到 1.0~2.0
        long pending = store.orders.values().stream()
                .filter(x -> x.status == Models.OrderStatus.MATCHING).count();
        long idle = store.drivers.stream().filter(d -> d.idle).count();
        double ratio = idle == 0 ? 2.0 : (double) pending / idle;
        o.surgeMultiplier = Math.min(MAX_SURGE, Math.max(1.0, round1(1.0 + ratio)));

        // 3) 优惠券：找一张未使用、且门槛 ≤ 应付金额的最大满减
        double payable = subtotal * o.surgeMultiplier;
        Models.Coupon best = store.coupons.stream()
                .filter(c -> !c.used && payable >= c.threshold)
                .filter(c -> c.amount == store.coupons.stream()
                        .filter(x -> !x.used && payable >= x.threshold)
                        .mapToDouble(x -> x.amount).max().orElse(0))
                .findFirst().orElse(null);
        if (best != null) {
            o.couponId = best.id;
            o.couponDeduction = best.amount;
        }

        o.finalPrice = Math.max(MIN_FARE, round1(payable - o.couponDeduction));

        Map<String, Object> payload = new HashMap<>();
        payload.put("finalPrice", o.finalPrice);
        payload.put("breakdown", o.priceBreakdown());
        payload.put("pendingOrders", pending);
        payload.put("idleDrivers", idle);
        bus.publish(Event.of("price.quoted", o.id, payload));
    }

    private static double round1(double v) { return Math.round(v * 10) / 10.0; }
}
