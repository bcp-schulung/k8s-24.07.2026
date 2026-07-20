package de.bcpeducation.jokes.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class JokeGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(
                JokeGatewayApplication.class,
                args
        );
    }
}
