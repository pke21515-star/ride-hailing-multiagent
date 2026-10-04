package io.github.pke21515.ride.agents;

import io.github.pke21515.ride.core.Agent;
import io.github.pke21515.ride.core.DemoStore;
import io.github.pke21515.ride.core.Event;
import io.github.pke21515.ride.core.EventBus;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 风控 Agent：黑名单校验、刷单频次检测、异常订单识别。 */
@Component
public class RiskControlAgent implements Agent {

    private static final int MAX_ORDERS_PER_10MIN = 3;   // 频次阈值

    private final DemoStore store;
    private final EventBus bus;

    public RiskControlAgent(DemoStore store, EventBus bus) {
        this.store = store; this.bus = bus;
        bus.register(this);
    }

    @Override public String name() { return "风控Agent"; }
    @Override public List<String> topics() { return List.of("order.created"); }

    @Override
    public void onEvent(Event e) {
        @SuppressWarnings("unchecked")
        Map<String, Object> created = (Map<String, Object>) e.payload();
        String passengerId = String.valueOf(created.get("passengerId"));

        String reason = check(passengerId);
        if (reason != null) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("reason", reason);
            payload.put("action", "订单拦截，已记录风控日志");
            bus.publish(Event.of("risk.rejected", e.orderId(), payload));
        } else {
            bus.publish(Event.of("risk.passed", e.orderId(), Map.of("msg", "风控校验通过")));
        }
    }

    private String check(String passengerId) {
        // ① 黑名单
        if (store.blacklist.contains(passengerId)) return "命中黑名单乘客";
        // ② 刷单频次：近 10 分钟同一乘客订单数
        long recent = store.orders.values().stream()
                .filter(o -> o.passengerId.equals(passengerId))
                .filter(o -> o.createdAt.isAfter(java.time.LocalDateTime.now().minusMinutes(10)))
                .count();
        if (recent >= MAX_ORDERS_PER_10MIN)
            return "10 分钟内下单 " + recent + " 次，疑似刷单";
        return null;
    }
}
