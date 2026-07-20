package de.bcpeducation.jokes.punchline.error;

public class PunchlineNotFoundException extends RuntimeException {

    public PunchlineNotFoundException(String setupId) {
        super("No punchline exists for setup ID: " + setupId);
    }
}
