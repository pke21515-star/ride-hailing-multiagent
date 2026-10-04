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

/** 客服 Agent：订单咨询（规则话术）、改单、纠纷登记。可接入 LLM 生成话术（LLM_BASE_URL）。 */
@Component
public class ServiceAgent implements Agent {

    private final DemoStore store;
    private final EventBus bus;

    public ServiceAgent(DemoStore store, EventBus bus) {
        this.store = store; this.bus = bus;
        bus.register(this);
    }

    @Override public String name() { return "客服Agent"; }
    @Override public List<String> topics() { return List.of("order.cancelled"); }

    @Override
    public void onEvent(Event e) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "取消安抚");
        payload.put("text", "您的订单已取消" + (((double) e.payload().get("cancelFee")) > 0
                ? "，收取空驶补偿费 " + e.payload().get("cancelFee") + " 元"
                : "，未产生费用") + "，如有疑问可继续咨询。");
        bus.publish(Event.of("notify.passenger", e.orderId(), payload));
    }

    /** 规则话术：FAQ 命中即答；未命中回退通用话术（生产可替换为 LLM）。 */
    public String answer(String orderId, String question) {
        String q = question == null ? "" : question;
        Models.Order o = orderId != null ? store.orders.get(orderId) : null;
        if (q.contains("价格") || q.contains("多少钱")) {
            return o != null && o.finalPrice > 0
                    ? "本单费用：" + o.priceBreakdown()
                    : "费用在下单时由定价 Agent 实时预估：起步价 10 元（含 3 公里）+ 里程费 2.4 元/公里 + 时长费 0.6 元/分钟，高峰时段有动态溢价。";
        }
        if (q.contains("取消") || q.contains("改单")) {
            return "司机接单前取消不收费；接单后取消将收取应付费用 30% 的空驶补偿。改单请先取消再重新下单。";
        }
        if (q.contains("纠纷") || q.contains("投诉")) {
            store.serviceTickets.add(new HashMap<>(Map.of("orderId", orderId == null ? "-" : orderId,
                    "question", q, "status", "已登记，人工客服将在 30 分钟内跟进")));
            return "已为您登记纠纷工单，人工客服将在 30 分钟内跟进处理。";
        }
        return "亲，我是客服 Agent，可以帮您解答价格、取消/改单、纠纷投诉等问题。";
    }
}
