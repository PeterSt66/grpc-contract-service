package nl.jdriven.blogs.svc.contract.model.exception;

import java.util.Arrays;
import java.util.stream.Stream;

public class PreConditionNotMetException extends RuntimeException {
    private String[] conditionFailures;

    public PreConditionNotMetException(String... conditionFailures) {
        this.conditionFailures = conditionFailures;
    }

    public PreConditionNotMetException(String conditionFailures) {
                this.conditionFailures = conditionFailures.split(";");
    }

    public Stream<String> getConditionFailures() {
        return Arrays.stream(conditionFailures);
    }

    public String getConditionFailuresAsString() {
        return String.join(";", conditionFailures);
    }
}
