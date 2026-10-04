package io.github.pke21515.ride.api;

import io.github.pke21515.ride.agents.ServiceAgent;
import io.github.pke21515.ride.core.DemoStore;
import io.github.pke21515.ride.core.Event;
import io.github.pke21515.ride.core.EventBus;
import io.github.pke21515.ride.core.Models;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** REST API：下单 / 支付 / 取消 / 客服 / 查询。 */
@RestController
@RequestMapping("/api")
public class RideController {

    private final DemoStore store;
    private final EventBus bus;
    private final ServiceAgent serviceAgent;

    public RideController(DemoStore store, EventBus bus, ServiceAgent serviceAgent) {
        this.store = store; this.bus = bus; this.serviceAgent = serviceAgent;
    }

    public record CreateReq(String passengerId, String origin, double originLat, double originLng,
                            String destination, double destLat, double destLng) {}

    @PostMapping("/orders")
    public Map<String, Object> create(@RequestBody CreateReq req) {
        Models.Order o = new Models.Order();
        o.id = store.nextOrderId();
        o.passengerId = req.passengerId() == null ? "passenger-001" : req.passengerId();
        o.origin = req.origin(); o.originLat = req.originLat(); o.originLng = req.originLng();
        o.destination = req.destination(); o.destLat = req.destLat(); o.destLng = req.destLng();
        // 下单 → 风控 → 派单 → 路径 → 定价，全链路同步流经 7 个 Agent
        bus.publish(Event.of("order.create.cmd", o.id, Map.of("order", o)));
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("orderId", o.id);
        resp.put("status", o.status);
        resp.put("driverId", o.driverId);
        resp.put("finalPrice", o.finalPrice);
        resp.put("breakdown", o.finalPrice > 0 ? o.priceBreakdown() : null);
        resp.put("rejectReason", o.rejectReason);
        return resp;
    }

    @PostMapping("/orders/{id}/pay")
    public Map<String, Object> pay(@PathVariable String id,
                                   @RequestParam(defaultValue = "微信支付") String payMethod) {
        bus.publish(Event.of("order.pay.cmd", id, Map.of("payMethod", payMethod)));
        Models.Order o = store.orders.get(id);
        return Map.of("orderId", id, "status", o == null ? "UNKNOWN" : o.status.name(),
                      "amount", o == null ? 0 : o.finalPrice);
    }

    @PostMapping("/orders/{id}/cancel")
    public Map<String, Object> cancel(@PathVariable String id,
                                      @RequestParam(defaultValue = "passenger") String by) {
        bus.publish(Event.of("order.cancel.cmd", id, Map.of("by", by)));
        Models.Order o = store.orders.get(id);
        return Map.of("orderId", id, "status", o == null ? "UNKNOWN" : o.status.name(),
                      "cancelFee", o == null ? 0 : o.refundAmount);
    }

    @PostMapping("/service")
    public Map<String, Object> service(@RequestParam String orderId, @RequestBody Map<String, String> body) {
        String answer = serviceAgent.answer(orderId, body.getOrDefault("question", ""));
        return Map.of("answer", answer);
    }

    @GetMapping("/orders")
    public List<Map<String, Object>> orders() {
        List<Map<String, Object>> list = new ArrayList<>();
        store.orders.values().forEach(o -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", o.id);
            m.put("passengerId", o.passengerId);
            m.put("route", o.origin + " → " + o.destination);
            m.put("status", o.status.name());
            m.put("driverId", o.driverId);
            m.put("distanceKm", o.distanceKm);
            m.put("finalPrice", o.finalPrice);
            list.add(m);
        });
        return list;
    }

    @GetMapping("/drivers")
    public List<Map<String, Object>> drivers() {
        return store.drivers.stream().map(d -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", d.id); m.put("name", d.name);
            m.put("rating", d.rating); m.put("acceptRate", d.acceptRate);
            m.put("idle", d.idle); m.put("todayOrders", d.todayOrders);
            return m;
        }).toList();
    }

    @GetMapping("/events")
    public Map<String, Object> events() {
        return Map.of("count", bus.getMsgCount(), "events", bus.recentEvents());
    }

    @GetMapping("/notifications")
    public List<String> notifications() { return store.notifications; }

    @GetMapping("/tickets")
    public List<Map<String, Object>> tickets() { return store.serviceTickets; }
}
