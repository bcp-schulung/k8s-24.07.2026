package de.bcpeducation.jokes.chaos.error;

public class TerminationDisabledException
        extends RuntimeException {

    public TerminationDisabledException() {
        super(
                "Process termination is disabled. Set CHAOS_TERMINATION_ENABLED=true to enable it."
        );
    }
}
