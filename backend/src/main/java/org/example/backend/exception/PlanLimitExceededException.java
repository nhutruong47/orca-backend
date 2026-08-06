package org.example.backend.exception;

public class PlanLimitExceededException extends RuntimeException {

    private final String limitType;
    private final long current;
    private final int limit;

    public PlanLimitExceededException(String limitType, long current, int limit, String message) {
        super(message);
        this.limitType = limitType;
        this.current = current;
        this.limit = limit;
    }

    public String getLimitType() {
        return limitType;
    }

    public long getCurrent() {
        return current;
    }

    public int getLimit() {
        return limit;
    }
}
