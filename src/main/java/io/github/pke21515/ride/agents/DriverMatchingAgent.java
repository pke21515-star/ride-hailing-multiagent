package io.github.pke21515.ride.agents;

import io.github.pke21515.ride.core.Agent;
import io.github.pke21515.ride.core.DemoStore;
import io.github.pke21515.ride.core.Event;
import io.github.pke21515.ride.core.EventBus;
import io.github.pke21515.ride.core.Models;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 司机匹配 Agent：多维度打分派单（距离/服务分/接单率/接单量平衡）。 */
@Component
public class DriverMatchingAgent implements Agent {

    // 打分权重（面试口径：距离 45% + 服务分 25% + 接单率 20% + 接单量均衡 10%）
    private static final double W_DIST = 0.45, W_RATING = 0.25, W_ACCEPT = 0.20, W_BALANCE = 0.10;

    private final DemoStore store;
    private final EventBus bus;

    public DriverMatchingAgent(DemoStore store, EventBus bus) {
        this.store = store; this.bus = bus;
        bus.register(this);
    }

    @Override public String name() { return "司机匹配Agent"; }
    @Override public List<String> topics() { return List.of("order.matching"); }

    @Override
    public void onEvent(Event e) {
        Models.Order o = store.orders.get(e.orderId());
        if (o == null) return;

        List<Models.Driver> candidates = store.drivers.stream().filter(d -> d.idle).toList();
        if (candidates.isEmpty()) {
            bus.publish(Event.of("risk.rejected", o.id, Map.of("reason", "当前无空闲司机")));
            return;
        }

        double maxDist = candidates.stream()
                .mapToDouble(d -> haversineKm(o.originLat, o.originLng, d.lat, d.lng)).max().orElse(1);
        int maxOrders = candidates.stream().mapToInt(d -> d.todayOrders).max().orElse(0);

        Models.Driver best = null;
        Map<String, Double> bestScores = new HashMap<>();
        double bestTotal = -1;

        for (Models.Driver d : candidates) {
            double dist = haversineKm(o.originLat, o.originLng, d.lat, d.lng);
            double sDist = (1 - dist / maxDist);                       // 越近越高
            double sRating = (d.rating - 4.0) / 1.0;                   // 4.0~5.0 归一
            double sAccept = d.acceptRate;
            double sBalance = maxOrders == 0 ? 1.0 : 1.0 - (double) d.todayOrders / maxOrders; // 均衡派单
            double total = 100 * (W_DIST * sDist + W_RATING * sRating + W_ACCEPT * sAccept + W_BALANCE * sBalance);
            if (total > bestTotal) {
                bestTotal = total; best = d;
                bestScores = Map.of("距离分", r2(sDist * 100), "服务分", r2(sRating * 100),
                                    "接单率分", r2(sAccept * 100), "均衡分", r2(sBalance * 100), "综合分", r2(total));
            }
        }

        best.idle = false;
        o.driverId = best.id;
        Map<String, Object> payload = new HashMap<>(bestScores);
        payload.put("driverId", best.id);
        payload.put("driverName", best.name);
        bus.publish(Event.of("driver.matched", o.id, payload));
    }

    /** Haversine 公式：两点球面距离（km） */
    public static double haversineKm(double lat1, double lng1, double lat2, double lng2) {
        double R = 6371, dLat = Math.toRadians(lat2 - lat1), dLng = Math.toRadians(lng2 - lng1);
        double a = Math.pow(Math.sin(dLat / 2), 2)
                 + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.pow(Math.sin(dLng / 2), 2);
        return 2 * R * Math.asin(Math.sqrt(a));
    }

    private static double r2(double v) { return Math.round(v * 100) / 100.0; }
}
