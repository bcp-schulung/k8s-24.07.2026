package de.bcpeducation.jokes.audience.event;

public interface ReactionEventPublisher {

    boolean publish(AudienceReactionEvent event);
}
