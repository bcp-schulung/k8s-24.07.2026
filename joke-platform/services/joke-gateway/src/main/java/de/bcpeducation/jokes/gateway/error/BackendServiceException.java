package de.bcpeducation.jokes.gateway.error;

public class BackendServiceException
        extends RuntimeException {

    private final String serviceName;
    private final Integer downstreamStatus;

    public BackendServiceException(
            String serviceName,
            String message,
            Integer downstreamStatus,
            Throwable cause
    ) {
        super(message, cause);

        this.serviceName = serviceName;
        this.downstreamStatus = downstreamStatus;
    }

    public BackendServiceException(
            String serviceName,
            String message,
            Throwable cause
    ) {
        this(
                serviceName,
                message,
                null,
                cause
        );
    }

    public String serviceName() {
        return serviceName;
    }

    public Integer downstreamStatus() {
        return downstreamStatus;
    }
}
