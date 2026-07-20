package de.bcpeducation.jokes.chaos.error;

public class SimulatedChaosException
        extends RuntimeException {

    private final String requestId;
    private final String instanceName;

    public SimulatedChaosException(
            String requestId,
            String instanceName
    ) {
        super(
                "The chaos comedian deliberately dropped the microphone"
        );

        this.requestId = requestId;
        this.instanceName = instanceName;
    }

    public String requestId() {
        return requestId;
    }

    public String instanceName() {
        return instanceName;
    }
}
