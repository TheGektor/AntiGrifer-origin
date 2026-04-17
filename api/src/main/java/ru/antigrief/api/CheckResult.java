package ru.antigrief.api;

import java.util.Map;

public class CheckResult {
    private final int violationScore;
    private final boolean cancelAction;
    private final String message;
    private final Map<String, Object> metadata;

    public CheckResult(int violationScore, boolean cancelAction, String message, Map<String, Object> metadata) {
        this.violationScore = violationScore;
        this.cancelAction = cancelAction;
        this.message = message;
        this.metadata = metadata;
    }

    public static CheckResult pass() {
        return new CheckResult(0, false, null, null);
    }

    public static CheckResult fail(int score, boolean cancel, String message) {
        return new CheckResult(score, cancel, message, null);
    }

    public int getViolationScore() { return violationScore; }
    public boolean isCancelAction() { return cancelAction; }
    public String getMessage() { return message; }
    public Map<String, Object> getMetadata() { return metadata; }
}
