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

/** 路径规划 Agent：距离估算（Haversine×绕路系数）、接驾时长、行程时长预估。 */
@Component
public class RoutingAgent implements Agent {

    private static final double DETOUR_FACTOR = 1.25;  // 实际路网绕路系数（直线距离 × 1.25）
    private static final double AVG_SPEED_KMH = 32;    // 城市平均车速

    private final DemoStore store;
    private final EventBus bus;

    public RoutingAgent(DemoStore store, EventBus bus) {
        this.store = store; this.bus = bus;
        bus.register(this);
    }

    @Override public String name() { return "路径规划Agent"; }
    @Override public List<String> topics() { return List.of("driver.matched"); }

    @Override
    public void onEvent(Event e) {
        Models.Order o = store.orders.get(e.orderId());
        if (o == null) return;

        // 行程直线距离 → 路网距离
        double straight = DriverMatchingAgent.haversineKm(o.originLat, o.originLng, o.destLat, o.destLng);
        o.distanceKm = round1(straight * DETOUR_FACTOR);
        o.durationMin = (int) Math.max(3, Math.round(o.distanceKm / AVG_SPEED_KMH * 60));

        // 接驾：司机当前位置 → 上车点（ETA 物理上限 120 分钟，防御异常坐标）
        Models.Driver d = store.drivers.stream().filter(x -> x.id.equals(o.driverId)).findFirst().orElse(null);
        if (d != null) {
            double pickKm = DriverMatchingAgent.haversineKm(o.originLat, o.originLng, d.lat, d.lng) * DETOUR_FACTOR;
            o.etaMin = (int) Math.min(120, Math.max(1, Math.round(pickKm / AVG_SPEED_KMH * 60)));
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("distanceKm", o.distanceKm);
        payload.put("durationMin", o.durationMin);
        payload.put("etaMin", o.etaMin);
        payload.put("msg", "规划路径（模拟路网，接入高德/百度路径规划API后替换）");
        bus.publish(Event.of("route.planned", o.id, payload));
    }

    private static double round1(double v) { return Math.round(v * 10) / 10.0; }
}
