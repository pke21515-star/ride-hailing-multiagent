package io.github.pke21515.ride.core;

import java.util.List;

/** Agent 统一接口：按主题订阅事件并处理。 */
public interface Agent {
    /** Agent 名称（订单/司机匹配/路径规划/定价/风控/客服/通知） */
    String name();

    /** 订阅的主题列表 */
    List<String> topics();

    /** 收到事件时的处理逻辑 */
    void onEvent(Event event);
}
