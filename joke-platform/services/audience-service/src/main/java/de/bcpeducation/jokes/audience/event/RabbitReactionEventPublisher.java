package de.bcpeducation.jokes.audience.event;

import de.bcpeducation.jokes.audience.config.RabbitMqConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "audience.events.enabled",
        havingValue = "true"
)
public class RabbitReactionEventPublisher
        implements ReactionEventPublisher {

    private static final Logger log =
            LoggerFactory.getLogger(
                    RabbitReactionEventPublisher.class
            );

    private final RabbitTemplate rabbitTemplate;

    public RabbitReactionEventPublisher(
            RabbitTemplate rabbitTemplate
    ) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public boolean publish(
            AudienceReactionEvent event
    ) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMqConfiguration.EXCHANGE_NAME,
                    RabbitMqConfiguration.ROUTING_KEY,
                    event
            );

            return true;
        } catch (AmqpException exception) {
            log.warn(
                    "Could not publish audience reaction event {}",
                    event.reactionId(),
                    exception
            );

            return false;
        }
    }
}
