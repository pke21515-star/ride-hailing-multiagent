package io.github.pke21515.ride.agents;

import io.github.pke21515.ride.core.Agent;
import io.github.pke21515.ride.core.DemoStore;
import io.github.pke21515.ride.core.Event;
import io.github.pke21515.ride.core.EventBus;
import io.github.pke21515.ride.core.Models;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/** 订单 Agent：订单创建、支付、取消、完结全生命周期管理。 */
@Component
public class OrderAgent implements Agent {

    private final DemoStore store;
    private final EventBus bus;

    public OrderAgent(DemoStore store, EventBus bus) {
        this.store = store; this.bus = bus;
        bus.register(this);
    }

    @Override public String name() { return "订单Agent"; }
    @Override public List<String> topics() {
        return List.of("order.create.cmd", "risk.passed", "risk.rejected",
                       "order.pay.cmd", "order.cancel.cmd", "price.quoted",
                       "order.start.cmd", "order.complete.cmd");
    }

    @Override
    public void onEvent(Event e) {
        switch (e.topic()) {
            case "order.create.cmd" -> create(e);
            case "risk.passed" -> {
                Models.Order o = store.orders.get(e.orderId());
                o.status = Models.OrderStatus.MATCHING;
                store.persistOrder(o);
                bus.publish(Event.of("order.matching", o.id, Map.of("msg", "风控通过，进入派单")));
            }
            case "risk.rejected" -> {
                Models.Order o = store.orders.get(e.orderId());
                o.status = Models.OrderStatus.RISK_REJECTED;
                o.rejectReason = String.valueOf(e.payload().get("reason"));
                store.persistOrder(o);
            }
            case "price.quoted" -> {
                Models.Order o = store.orders.get(e.orderId());
                o.finalPrice = (double) e.payload().get("finalPrice");
                o.status = Models.OrderStatus.DISPATCHED;
                store.persistOrder(o);
            }
            case "order.pay.cmd" -> pay(e);
            case "order.cancel.cmd" -> cancel(e);
            case "order.start.cmd" -> startTrip(e);
            case "order.complete.cmd" -> completeTrip(e);
        }
    }

    private void create(Event e) {
        Models.Order o = (Models.Order) e.payload().get("order");
        store.orders.put(o.id, o);
        store.persistOrder(o);
        bus.publish(Event.of("order.created", o.id,
                Map.of("passengerId", o.passengerId, "origin", o.origin, "destination", o.destination)));
    }

    /** 司机出发接驾：DISPATCHED → TRIP */
    private void startTrip(Event e) {
        Models.Order o = store.orders.get(e.orderId());
        if (o == null || o.status != Models.OrderStatus.DISPATCHED) return;
        o.status = Models.OrderStatus.TRIP;
        store.persistOrder(o);
        bus.publish(Event.of("order.started", o.id, Map.of("msg", "司机已出发接驾")));
    }

    /** 行程完结：TRIP → COMPLETED（线下支付/余额场景） */
    private void completeTrip(Event e) {
        Models.Order o = store.orders.get(e.orderId());
        if (o == null || o.status != Models.OrderStatus.TRIP) return;
        o.status = Models.OrderStatus.COMPLETED;
        store.persistOrder(o);
        if (o.driverId != null)
            store.drivers.stream().filter(d -> d.id.equals(o.driverId)).findFirst()
                    .ifPresent(d -> { d.idle = true; d.todayOrders++; });
        bus.publish(Event.of("order.completed", o.id, Map.of("msg", "行程完成", "amount", o.finalPrice)));
    }

    private synchronized void pay(Event e) {
        Models.Order o = store.orders.get(e.orderId());
        if (o == null || o.status != Models.OrderStatus.DISPATCHED) return;
        o.payMethod = String.valueOf(e.payload().getOrDefault("payMethod", "微信支付"));
        o.status = Models.OrderStatus.PAID;
        store.persistOrder(o);
        bus.publish(Event.of("order.paid", o.id,
                Map.of("amount", o.finalPrice, "payMethod", o.payMethod)));
        o.status = Models.OrderStatus.COMPLETED;   // 演示：支付即完结
        store.persistOrder(o);
        if (o.driverId != null) {
            store.drivers.stream().filter(d -> d.id.equals(o.driverId)).findFirst()
                    .ifPresent(d -> { d.idle = true; d.todayOrders++; });
            store.drivers.stream().filter(d -> d.id.equals(o.driverId)).findFirst()
                    .ifPresent(d -> { /* 同步司机状态到库 */ store.persistDriver(d); });
        }
        if (o.couponId != null)
            store.coupons.stream().filter(c -> c.id.equals(o.couponId)).findFirst().ifPresent(c -> c.used = true);
    }

    private void cancel(Event e) {
        Models.Order o = store.orders.get(e.orderId());
        if (o == null || o.status == Models.OrderStatus.COMPLETED) return;
        // 取消费规则：司机已接单（DISPATCHED）收起步价 30% 作空驶补偿，未接单免费
        o.refundAmount = o.status == Models.OrderStatus.DISPATCHED ? round(o.finalPrice * 0.3) : 0;
        o.status = Models.OrderStatus.CANCELLED;
        store.persistOrder(o);
        if (o.driverId != null)
            store.drivers.stream().filter(d -> d.id.equals(o.driverId)).findFirst()
                    .ifPresent(d -> { d.idle = true; store.persistDriver(d); });
        bus.publish(Event.of("order.cancelled", o.id,
                Map.of("cancelFee", o.refundAmount, "by", e.payload().getOrDefault("by", "passenger"))));
    }

    private static double round(double v) { return Math.round(v * 10) / 10.0; }
}
