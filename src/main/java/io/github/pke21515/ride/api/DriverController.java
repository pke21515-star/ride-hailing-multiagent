package io.github.pke21515.ride.api;

import io.github.pke21515.ride.core.DemoStore;
import io.github.pke21515.ride.core.Event;
import io.github.pke21515.ride.core.EventBus;
import io.github.pke21515.ride.core.Models;
import io.github.pke21515.ride.entity.OrderEntity;
import io.github.pke21515.ride.repository.OrderRepo;
import io.github.pke21515.ride.security.TokenService.Claims;
import io.github.pke21515.ride.security.AuthInterceptor;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 司机端：接单看板 / 出发接驾 / 完成行程。需 DRIVER 令牌。 */
@RestController
@RequestMapping("/api/driver")
public class DriverController {

    private static final List<String> ACTIVE = List.of("DISPATCHED", "TRIP");

    private final EventBus bus;
    private final DemoStore store;
    private final OrderRepo orderRepo;

    public DriverController(EventBus bus, DemoStore store, OrderRepo orderRepo) {
        this.bus = bus; this.store = store; this.orderRepo = orderRepo;
    }

    /** 我的待办订单（派给我的且未完结） */
    @GetMapping("/orders")
    public List<OrderEntity> myOrders(@RequestAttribute("claims") Claims c) {
        return orderRepo.findByDriverIdAndStatusInOrderByCreatedAtDesc(c.driverId(), ACTIVE);
    }

    @PostMapping("/orders/{id}/start")
    public Map<String, Object> start(@RequestAttribute("claims") Claims c, @PathVariable String id) {
        Models.Order o = store.orders.get(id);
        if (o == null || !idBelongsTo(c, o)) return Map.of("error", "订单不存在或不属于你");
        bus.publish(Event.of("order.start.cmd", id, Map.of("by", c.driverId())));
        return result(id);
    }

    @PostMapping("/orders/{id}/complete")
    public Map<String, Object> complete(@RequestAttribute("claims") Claims c, @PathVariable String id) {
        Models.Order o = store.orders.get(id);
        if (o == null || !idBelongsTo(c, o)) return Map.of("error", "订单不存在或不属于你");
        bus.publish(Event.of("order.complete.cmd", id, Map.of("by", c.driverId())));
        return result(id);
    }

    @GetMapping("/profile")
    public Map<String, Object> profile(@RequestAttribute("claims") Claims c) {
        Models.Driver d = store.drivers.stream().filter(x -> x.id.equals(c.driverId())).findFirst().orElse(null);
        if (d == null) return Map.of("error", "司机档案不存在");
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("driverId", d.id); m.put("name", d.name); m.put("rating", d.rating);
        m.put("acceptRate", d.acceptRate); m.put("idle", d.idle); m.put("todayOrders", d.todayOrders);
        return m;
    }

    private boolean idBelongsTo(Claims c, Models.Order o) {
        return c.driverId() != null && c.driverId().equals(o.driverId);
    }

    private Map<String, Object> result(String id) {
        Models.Order o = store.orders.get(id);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("orderId", id);
        m.put("status", o == null ? "UNKNOWN" : o.status.name());
        return m;
    }
}
