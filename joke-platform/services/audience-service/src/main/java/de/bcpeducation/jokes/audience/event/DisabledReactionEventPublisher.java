package de.bcpeducation.jokes.audience.event;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "audience.events.enabled",
        havingValue = "false",
        matchIfMissing = true
)
public class DisabledReactionEventPublisher
        implements ReactionEventPublisher {

    @Override
    public boolean publish(
            AudienceReactionEvent event
    ) {
        return false;
    }
}
