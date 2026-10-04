package io.github.pke21515.ride.config;

import io.github.pke21515.ride.agents.NotifyAgent;
import io.github.pke21515.ride.core.Agent;
import io.github.pke21515.ride.core.DemoStore;
import io.github.pke21515.ride.core.EventBus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Agent 注册：其余 Agent 构造时自注册到 EventBus；通知 Agent 在此统一注册。 */
@Configuration
public class AgentConfig {

    @Bean
    public Agent notifyAgent(DemoStore store, EventBus bus) {
        NotifyAgent agent = new NotifyAgent(store);
        bus.register(agent);
        return agent;
    }
}
