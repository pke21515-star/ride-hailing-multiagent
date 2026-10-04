package io.github.pke21515.ride.entity;

import jakarta.persistence.*;

/** Agent 事件审计：EventBus 每条消息落库，管理端可查全量历史。 */
@Entity
@Table(name = "agent_events")
public class EventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false, length = 40)
    public String topic;

    @Column(length = 24)
    public String orderId;

    @Column(length = 1024)
    public String payload;

    public Long ts;
}
