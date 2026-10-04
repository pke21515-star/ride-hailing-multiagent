package io.github.pke21515.ride.core;

import java.util.Map;

/** EventBus 传递的事件：主题 + 订单号 + 载荷。 */
public record Event(String topic, String orderId, Map<String, Object> payload, long timestamp) {
    public static Event of(String topic, String orderId, Map<String, Object> payload) {
        return new Event(topic, orderId, payload, System.currentTimeMillis());
    }
}
