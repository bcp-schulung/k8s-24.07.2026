package de.bcpeducation.jokes.generator.error;

public class JokeCategoryNotFoundException extends RuntimeException {

    public JokeCategoryNotFoundException(String category) {
        super("Unknown joke category: " + category);
    }
}
