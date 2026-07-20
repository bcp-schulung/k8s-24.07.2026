package de.bcpeducation.jokes.chaos.service;

import de.bcpeducation.jokes.chaos.error.TerminationDisabledException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Service;

@Service
public class ApplicationTerminationService
        implements ApplicationContextAware {

    private static final Logger log =
            LoggerFactory.getLogger(
                    ApplicationTerminationService.class
            );

    private final boolean terminationEnabled;

    private ConfigurableApplicationContext applicationContext;

    public ApplicationTerminationService(
            @Value("${chaos.termination.enabled:false}")
            boolean terminationEnabled
    ) {
        this.terminationEnabled = terminationEnabled;
    }

    @Override
    public void setApplicationContext(
            ApplicationContext applicationContext
    ) throws BeansException {
        if (!(applicationContext
                instanceof ConfigurableApplicationContext configurable)) {
            throw new IllegalStateException(
                    "Application context is not configurable"
            );
        }

        this.applicationContext = configurable;
    }

    public void scheduleTermination(
            long delayMs,
            String reason
    ) {
        if (!terminationEnabled) {
            throw new TerminationDisabledException();
        }

        log.warn(
                "Application termination scheduled in {} ms. Reason: {}",
                delayMs,
                reason
        );

        Thread.startVirtualThread(() -> {
            try {
                Thread.sleep(delayMs);

                int exitCode = SpringApplication.exit(
                        applicationContext,
                        () -> 17
                );

                System.exit(exitCode);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();

                log.warn(
                        "Scheduled application termination was interrupted"
                );
            }
        });
    }

    public boolean isTerminationEnabled() {
        return terminationEnabled;
    }
}
