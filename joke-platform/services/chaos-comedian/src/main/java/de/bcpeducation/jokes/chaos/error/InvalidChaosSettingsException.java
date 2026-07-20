package de.bcpeducation.jokes.chaos.error;

public class InvalidChaosSettingsException
        extends RuntimeException {

    public InvalidChaosSettingsException(
            String message
    ) {
        super(message);
    }
}
