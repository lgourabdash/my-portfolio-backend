package com.lgourabdash.portfolio;

import java.time.Instant;

/** One stored error row for the admin error log finder (in-memory ring buffer). */
public class AdminErrorLogEntry {

    private final String id;
    private final Instant occurredAt;
    private final String source;
    private final String message;
    private final String detail;
    private final String path;
    private final Integer httpStatus;
    private final String code;
    private final String fixHint;

    private String status;
    private Instant closedAt;

    public AdminErrorLogEntry(
            String id,
            Instant occurredAt,
            String source,
            String message,
            String detail,
            String path,
            Integer httpStatus,
            String code,
            String fixHint) {
        this.id = id;
        this.occurredAt = occurredAt;
        this.source = source;
        this.message = message;
        this.detail = detail;
        this.path = path;
        this.httpStatus = httpStatus;
        this.code = code;
        this.fixHint = fixHint;
        this.status = "OPEN";
        this.closedAt = null;
    }

    public String getId() {
        return id;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public String getSource() {
        return source;
    }

    public String getMessage() {
        return message;
    }

    public String getDetail() {
        return detail;
    }

    public String getPath() {
        return path;
    }

    public Integer getHttpStatus() {
        return httpStatus;
    }

    public String getCode() {
        return code;
    }

    public String getFixHint() {
        return fixHint;
    }

    /** OPEN or CLOSED */
    public String getStatus() {
        return status;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public void markClosed() {
        if ("CLOSED".equals(this.status)) {
            return;
        }
        this.status = "CLOSED";
        this.closedAt = Instant.now();
    }
}
