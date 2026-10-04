package io.github.pke21515.ride.core;

import io.github.pke21515.ride.entity.BlacklistEntity;
import io.github.pke21515.ride.entity.DriverEntity;
import io.github.pke21515.ride.entity.OrderEntity;
import io.github.pke21515.ride.entity.UserEntity;
import io.github.pke21515.ride.repository.BlacklistRepo;
import io.github.pke21515.ride.repository.DriverRepo;
import io.github.pke21515.ride.repository.OrderRepo;
import io.github.pke21515.ride.repository.UserRepo;
import io.github.pke21515.ride.security.TokenService;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 存储层：内存工作集（供 Agent 网络同步读写） + 写穿持久化（H2 文件库，重启恢复）。
 * 生产替换为 MySQL + Redis（司机位置用 GEO），仅改本类实现。
 */
@Component
public class DemoStore {

    public final Map<String, Models.Order> orders = new ConcurrentHashMap<>();
    public final List<Models.Driver> drivers = new ArrayList<>();
    public final List<Models.Coupon> coupons = new ArrayList<>();
    public final List<String> blacklist = new ArrayList<>();
    public final List<String> notifications = new ArrayList<>();
    public final List<Map<String, Object>> serviceTickets = new ArrayList<>();

    private final OrderRepo orderRepo;
    private final DriverRepo driverRepo;
    private final BlacklistRepo blacklistRepo;
    private final UserRepo userRepo;
    private int seq = 1000;

    public DemoStore(OrderRepo orderRepo, DriverRepo driverRepo,
                     BlacklistRepo blacklistRepo, UserRepo userRepo) {
        this.orderRepo = orderRepo; this.driverRepo = driverRepo;
        this.blacklistRepo = blacklistRepo; this.userRepo = userRepo;
    }

    /** 启动：种子数据落库（仅首次） + 从库恢复内存工作集。 */
    @PostConstruct
    public void init() {
        // 1) 司机池：首启种子 8 人（坐标与 Geo 地标同域：郑州）
        if (driverRepo.count() == 0) {
            seed("D001", "张师傅", 34.7540, 113.7660, 4.9, 0.98);   // 郑州东站
            seed("D002", "李师傅", 34.7500, 113.6650, 4.7, 0.90);   // 二七广场
            seed("D003", "王师傅", 34.7690, 113.7280, 4.8, 0.85);   // 郑东新区CBD
            seed("D004", "赵师傅", 34.7970, 113.5900, 4.6, 0.95);   // 高新区
            seed("D005", "陈师傅", 34.7300, 113.6800, 4.5, 0.80);   // 管城
            seed("D006", "刘师傅", 34.8000, 113.7500, 4.9, 0.70);   // 北龙湖
            seed("D007", "孙师傅", 34.7580, 113.6200, 4.4, 0.99);   // 碧沙岗
            seed("D008", "周师傅", 34.5300, 113.8400, 4.3, 0.88);   // 新郑机场
        }
        driverRepo.findAll().forEach(e -> drivers.add(fromEntity(e)));

        // 2) 账号：首启种子管理员 + 司机端账号（乘客自助注册）
        if (userRepo.count() == 0) {
            seedUser("admin", "admin123", "系统管理员", "ADMIN", null);
            for (Models.Driver d : drivers) seedUser(d.id, "123456", d.name, "DRIVER", d.id);
        }

        // 3) 黑名单：首启种子 passenger-996（注意：不能用双大括号初始化，匿名子类不是 JPA 实体）
        if (blacklistRepo.count() == 0) {
            BlacklistEntity seedBlack = new BlacklistEntity();
            seedBlack.passengerId = "passenger-996";
            seedBlack.reason = "种子黑名单（演示）";
            blacklistRepo.save(seedBlack);
        }
        blacklistRepo.findAll().forEach(b -> blacklist.add(b.passengerId));

        // 4) 历史订单恢复 + 订单号序列续接
        List<OrderEntity> all = orderRepo.findAll();
        int maxSeq = 1000;
        for (OrderEntity e : all) {
            Models.Order o = fromEntity(e);
            orders.put(o.id, o);
            try { maxSeq = Math.max(maxSeq, Integer.parseInt(o.id.replace("ORD", ""))); } catch (Exception ignore) {}
        }
        seq = maxSeq;
    }

    private void seed(String id, String name, double lat, double lng, double rating, double acceptRate) {
        DriverEntity e = new DriverEntity();
        e.id = id; e.name = name; e.lat = lat; e.lng = lng; e.rating = rating; e.acceptRate = acceptRate;
        driverRepo.save(e);
    }

    private void seedUser(String phone, String pwd, String name, String role, String driverId) {
        UserEntity u = new UserEntity();
        u.phone = phone; u.passwordHash = TokenService.hash(pwd); u.name = name; u.role = role; u.driverId = driverId;
        userRepo.save(u);
    }

    public synchronized String nextOrderId() { return "ORD" + (++seq); }

    /** 订单状态每次迁移后调用：内存 → 库 写穿 */
    public void persistOrder(Models.Order o) {
        orderRepo.save(toEntity(o));
    }

    /** 司机状态每次变更后调用：内存 → 库 写穿 */
    public void persistDriver(Models.Driver d) {
        driverRepo.save(toEntity(d));
    }

    public void addBlacklist(String passengerId, String reason) {
        if (!blacklist.contains(passengerId)) {
            BlacklistEntity e = new BlacklistEntity();
            e.passengerId = passengerId; e.reason = reason;
            blacklistRepo.save(e);
            blacklist.add(passengerId);
        }
    }

    public boolean removeBlacklist(String passengerId) {
        boolean removed = blacklist.remove(passengerId);
        if (removed) blacklistRepo.findAll().forEach(e -> {
            if (e.passengerId.equals(passengerId)) blacklistRepo.delete(e);
        });
        return removed;
    }

    /* ---------- 领域模型 <-> 实体 ---------- */

    public OrderEntity toEntity(Models.Order o) {
        OrderEntity e = new OrderEntity();
        e.id = o.id; e.passengerId = o.passengerId;
        e.origin = o.origin; e.originLat = o.originLat; e.originLng = o.originLng;
        e.destination = o.destination; e.destLat = o.destLat; e.destLng = o.destLng;
        e.status = o.status.name(); e.driverId = o.driverId;
        e.distanceKm = o.distanceKm; e.durationMin = o.durationMin; e.etaMin = o.etaMin;
        e.basePrice = o.basePrice; e.mileageFee = o.mileageFee; e.durationFee = o.durationFee;
        e.surgeMultiplier = o.surgeMultiplier; e.couponDeduction = o.couponDeduction;
        e.finalPrice = o.finalPrice; e.couponId = o.couponId; e.rejectReason = o.rejectReason;
        e.createdAt = o.createdAt; e.payMethod = o.payMethod; e.refundAmount = o.refundAmount;
        return e;
    }

    public Models.Order fromEntity(OrderEntity e) {
        Models.Order o = new Models.Order();
        o.id = e.id; o.passengerId = e.passengerId;
        o.origin = e.origin; o.originLat = e.originLat; o.originLng = e.originLng;
        o.destination = e.destination; o.destLat = e.destLat; o.destLng = e.destLng;
        o.status = Models.OrderStatus.valueOf(e.status); o.driverId = e.driverId;
        o.distanceKm = e.distanceKm == null ? 0 : e.distanceKm;
        o.durationMin = e.durationMin == null ? 0 : e.durationMin;
        o.etaMin = e.etaMin == null ? 0 : Math.min(120, Math.max(0, e.etaMin));   // 钳制历史脏数据（如坐标缺失时的 28664）
        o.basePrice = e.basePrice == null ? 0 : e.basePrice;
        o.mileageFee = e.mileageFee == null ? 0 : e.mileageFee;
        o.durationFee = e.durationFee == null ? 0 : e.durationFee;
        o.surgeMultiplier = e.surgeMultiplier == null ? 1.0 : e.surgeMultiplier;
        o.couponDeduction = e.couponDeduction == null ? 0 : e.couponDeduction;
        o.finalPrice = e.finalPrice == null ? 0 : e.finalPrice;
        o.couponId = e.couponId; o.rejectReason = e.rejectReason;
        o.createdAt = e.createdAt == null ? LocalDateTime_min() : e.createdAt;
        o.payMethod = e.payMethod; o.refundAmount = e.refundAmount == null ? 0 : e.refundAmount;
        return o;
    }

    private DriverEntity toEntity(Models.Driver d) {
        DriverEntity e = new DriverEntity();
        e.id = d.id; e.name = d.name; e.lat = d.lat; e.lng = d.lng;
        e.rating = d.rating; e.acceptRate = d.acceptRate; e.idle = d.idle; e.todayOrders = d.todayOrders;
        return e;
    }

    private Models.Driver fromEntity(DriverEntity e) {
        Models.Driver d = new Models.Driver(e.id, e.name, e.lat, e.lng, e.rating, e.acceptRate);
        d.idle = Boolean.TRUE.equals(e.idle);
        d.todayOrders = e.todayOrders == null ? 0 : e.todayOrders;
        return d;
    }

    private static java.time.LocalDateTime LocalDateTime_min() {
        return java.time.LocalDateTime.now().minusDays(1);
    }
}
