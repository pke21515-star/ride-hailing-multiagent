package io.github.pke21515.ride.api;

import io.github.pke21515.ride.core.DemoStore;
import io.github.pke21515.ride.core.Event;
import io.github.pke21515.ride.core.EventBus;
import io.github.pke21515.ride.core.Geo;
import io.github.pke21515.ride.core.Models;
import io.github.pke21515.ride.entity.OrderEntity;
import io.github.pke21515.ride.repository.OrderRepo;
import io.github.pke21515.ride.security.TokenService.Claims;
import io.github.pke21515.ride.security.AuthInterceptor;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/** 乘客端：下单 / 我的订单 / 支付 / 取消。需 PASSENGER（或 ADMIN）令牌。 */
@RestController
@RequestMapping("/api/passenger")
public class PassengerController {

    private final EventBus bus;
    private final DemoStore store;
    private final OrderRepo orderRepo;

    public PassengerController(EventBus bus, DemoStore store, OrderRepo orderRepo) {
        this.bus = bus; this.store = store; this.orderRepo = orderRepo;
    }

    public record CreateReq(String origin, Double originLat, Double originLng,
                            String destination, Double destLat, Double destLng) {}

    @PostMapping("/orders")
    public Map<String, Object> create(@RequestAttribute("claims") Claims c, @RequestBody CreateReq req) {
        Models.Order o = new Models.Order();
        o.id = store.nextOrderId();
        o.passengerId = c.name() + "-" + c.uid();      // 登录身份即乘客标识
        // 坐标兜底：未传/非法时按地名解析（地标表 + 确定性伪坐标），避免 (0,0) 污染 Haversine 距离与计价
        double[] oLL = Geo.resolve(req.origin(), nvl(req.originLat()), nvl(req.originLng()));
        double[] dLL = Geo.resolve(req.destination(), nvl(req.destLat()), nvl(req.destLng()));
        o.origin = req.origin(); o.originLat = oLL[0]; o.originLng = oLL[1];
        o.destination = req.destination(); o.destLat = dLL[0]; o.destLng = dLL[1];
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

    private static double nvl(Double v) { return v == null ? 0.0 : v; }

    @GetMapping("/orders")
    public Object myOrders(@RequestAttribute("claims") Claims c) {
        return orderRepo.findByPassengerIdOrderByCreatedAtDesc(c.name() + "-" + c.uid());
    }

    @PostMapping("/orders/{id}/pay")
    public Map<String, Object> pay(@RequestAttribute("claims") Claims c, @PathVariable String id,
                                   @RequestParam(defaultValue = "微信支付") String payMethod) {
        bus.publish(Event.of("order.pay.cmd", id, Map.of("payMethod", payMethod)));
        Models.Order o = store.orders.get(id);
        return Map.of("orderId", id, "status", o == null ? "UNKNOWN" : o.status.name(),
                      "amount", o == null ? 0 : o.finalPrice);
    }

    @PostMapping("/orders/{id}/cancel")
    public Map<String, Object> cancel(@RequestAttribute("claims") Claims c, @PathVariable String id) {
        bus.publish(Event.of("order.cancel.cmd", id, Map.of("by", "passenger")));
        Models.Order o = store.orders.get(id);
        return Map.of("orderId", id, "status", o == null ? "UNKNOWN" : o.status.name(),
                      "cancelFee", o == null ? 0 : o.refundAmount);
    }
}
