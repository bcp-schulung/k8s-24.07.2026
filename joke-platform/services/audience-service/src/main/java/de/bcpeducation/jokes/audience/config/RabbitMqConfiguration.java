package de.bcpeducation.jokes.audience.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(
        name = "audience.events.enabled",
        havingValue = "true"
)
public class RabbitMqConfiguration {

    public static final String EXCHANGE_NAME =
            "joke-platform.events";

    public static final String QUEUE_NAME =
            "audience.reactions";

    public static final String ROUTING_KEY =
            "audience.reaction.created";

    @Bean
    public DirectExchange jokePlatformExchange() {
        return new DirectExchange(
                EXCHANGE_NAME,
                true,
                false
        );
    }

    @Bean
    public Queue audienceReactionQueue() {
        return new Queue(
                QUEUE_NAME,
                true
        );
    }

    @Bean
    public Binding audienceReactionBinding(
            Queue audienceReactionQueue,
            DirectExchange jokePlatformExchange
    ) {
        return BindingBuilder
                .bind(audienceReactionQueue)
                .to(jokePlatformExchange)
                .with(ROUTING_KEY);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
