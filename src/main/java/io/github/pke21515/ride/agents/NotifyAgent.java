package io.github.pke21515.ride.agents;

import io.github.pke21515.ride.core.Agent;
import io.github.pke21515.ride.core.DemoStore;
import io.github.pke21515.ride.core.Event;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/** 通知 Agent：统一收口短信 / APP 推送（演示写入通知日志，生产接短信网关/个推）。由 AgentConfig 注册到 EventBus。 */
public class NotifyAgent implements Agent {

    private final DemoStore store;

    public NotifyAgent(DemoStore store) {
        this.store = store;
    }

    @Override public String name() { return "通知Agent"; }
    @Override public List<String> topics() {
        return List.of("order.created", "driver.matched", "price.quoted",
                       "order.paid", "order.cancelled", "notify.passenger", "notify.driver", "risk.rejected");
    }

    @Override
    public void onEvent(Event e) {
        String text = switch (e.topic()) {
            case "order.created" -> "[APP] 司机端：新订单待匹配（" + e.payload().get("origin") + " → " + e.payload().get("destination") + "）";
            case "driver.matched" -> "[短信+APP] 乘客：已为您匹配 " + e.payload().get("driverName") + "，请留意接驾信息";
            case "price.quoted" -> "[APP] 计价完成：" + e.payload().get("breakdown");
            case "order.paid" -> "[短信] 乘客：支付成功 " + e.payload().get("amount") + " 元（" + e.payload().get("payMethod") + "）";
            case "order.cancelled" -> "[短信+APP] 双端：订单已取消，补偿/取消费 " + e.payload().get("cancelFee") + " 元";
            case "risk.rejected" -> "[APP] 乘客：订单未能受理（风控）";
            case "notify.passenger", "notify.driver" -> "[" + (e.topic().endsWith("driver") ? "APP·司机端" : "APP·乘客端") + "] " + e.payload().get("text");
            default -> "[APP] " + e.topic();
        };
        store.notifications.add("[" + e.orderId() + "] " + text);
        while (store.notifications.size() > 100) store.notifications.remove(0);
    }
}
