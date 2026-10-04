package io.github.pke21515.ride.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.pke21515.ride.entity.EventEntity;
import io.github.pke21515.ride.repository.EventRepo;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * EventBus（Mesh 中枢）：Agent 按主题订阅/发布，广播式分发。
 * 每条消息审计落库（agent_events 表）；演示版为进程内同步实现，
 * 生产可替换为 RocketMQ/Kafka，接口不变。
 */
@Component
public class EventBus {

    public interface Subscriber extends Agent {}

    private final Map<String, List<Agent>> subscribers = new ConcurrentHashMap<>();
    private final Deque<Event> recent = new ConcurrentLinkedDeque<>();
    private final EventRepo eventRepo;
    private final ObjectMapper om = new ObjectMapper();
    private long msgCount = 0;

    public EventBus(EventRepo eventRepo) { this.eventRepo = eventRepo; }

    public void register(Agent agent) {
        for (String topic : agent.topics()) {
            subscribers.computeIfAbsent(topic, k -> new ArrayList<>()).add(agent);
        }
    }

    public void publish(Event event) {
        msgCount++;
        recent.addLast(event);
        while (recent.size() > 200) recent.pollFirst();
        try {
            EventEntity e = new EventEntity();
            e.topic = event.topic(); e.orderId = event.orderId(); e.ts = event.timestamp();
            e.payload = om.writeValueAsString(event.payload());
            eventRepo.save(e);
        } catch (Exception ignore) { /* 审计失败不阻塞主链路 */ }
        List<Agent> list = subscribers.get(event.topic());
        if (list == null) return;
        for (Agent a : list) {
            try {
                a.onEvent(event);
            } catch (Exception e) {
                recent.addLast(Event.of("agent.error", event.orderId(),
                        Map.of("agent", a.name(), "error", String.valueOf(e.getMessage()))));
            }
        }
    }

    public List<Event> recentEvents() {
        return new ArrayList<>(recent);
    }

    public long getMsgCount() {
        return msgCount;
    }
}
