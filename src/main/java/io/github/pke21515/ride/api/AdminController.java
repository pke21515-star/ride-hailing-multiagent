package io.github.pke21515.ride.api;

import io.github.pke21515.ride.core.DemoStore;
import io.github.pke21515.ride.core.EventBus;
import io.github.pke21515.ride.entity.OrderEntity;
import io.github.pke21515.ride.repository.*;
import io.github.pke21515.ride.security.AuthInterceptor;
import io.github.pke21515.ride.security.TokenService.Claims;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 管理后台：运营看板 / 订单管理 / 司机管理 / 黑名单 / 用户 / 事件审计。需 ADMIN 令牌。 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final OrderRepo orderRepo;
    private final DriverRepo driverRepo;
    private final BlacklistRepo blacklistRepo;
    private final UserRepo userRepo;
    private final EventRepo eventRepo;
    private final EventBus bus;
    private final DemoStore store;

    public AdminController(OrderRepo orderRepo, DriverRepo driverRepo, BlacklistRepo blacklistRepo,
                           UserRepo userRepo, EventRepo eventRepo, EventBus bus, DemoStore store) {
        this.orderRepo = orderRepo; this.driverRepo = driverRepo; this.blacklistRepo = blacklistRepo;
        this.userRepo = userRepo; this.eventRepo = eventRepo; this.bus = bus; this.store = store;
    }

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        long total = orderRepo.count();
        long completed = orderRepo.countByStatus("COMPLETED");
        long cancelled = orderRepo.countByStatus("CANCELLED");
        long rejected = orderRepo.countByStatus("RISK_REJECTED");
        long active = orderRepo.countByStatus("DISPATCHED") + orderRepo.countByStatus("TRIP");
        double gmv = orderRepo.findAll().stream()
                .filter(o -> "COMPLETED".equals(o.status) || "PAID".equals(o.status))
                .mapToDouble(o -> o.finalPrice == null ? 0 : o.finalPrice).sum();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("totalOrders", total);
        m.put("completed", completed);
        m.put("cancelled", cancelled);
        m.put("riskRejected", rejected);
        m.put("active", active);
        m.put("gmv", Math.round(gmv * 10) / 10.0);
        m.put("avgPrice", completed > 0 ? Math.round(gmv / completed * 10) / 10.0 : 0);
        m.put("drivers", driverRepo.count());
        m.put("idleDrivers", driverRepo.findAll().stream().filter(d -> Boolean.TRUE.equals(d.idle)).count());
        m.put("eventBusMsg", bus.getMsgCount());
        m.put("users", userRepo.count());
        return m;
    }

    @GetMapping("/orders")
    public Page<OrderEntity> orders(@RequestParam(defaultValue = "") String status,
                                    @RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "10") int size) {
        PageRequest p = PageRequest.of(page, Math.min(size, 100));
        return status.isBlank() ? orderRepo.findAll(p) : orderRepo.findByStatus(status, p);
    }

    @GetMapping("/drivers")
    public Object drivers() { return driverRepo.findAll(); }

    @GetMapping("/blacklist")
    public Object blacklist() { return blacklistRepo.findAll(); }

    public record BlackReq(String passengerId, String reason) {}

    @PostMapping("/blacklist")
    public Map<String, Object> addBlack(@RequestBody BlackReq req) {
        if (req.passengerId() == null || req.passengerId().isBlank())
            return Map.of("error", "乘客标识不能为空");
        store.addBlacklist(req.passengerId().trim(),
                req.reason() == null || req.reason().isBlank() ? "管理员手动加入" : req.reason());
        return Map.of("ok", true);
    }

    @DeleteMapping("/blacklist/{passengerId}")
    public Map<String, Object> delBlack(@PathVariable String passengerId) {
        return Map.of("ok", store.removeBlacklist(passengerId));
    }

    @GetMapping("/users")
    public Object users() { return userRepo.findAll(); }

    @GetMapping("/events")
    public Object events(@RequestParam(defaultValue = "0") int page,
                         @RequestParam(defaultValue = "20") int size) {
        return eventRepo.findAllByOrderByIdDesc(PageRequest.of(page, Math.min(size, 100)));
    }
}
