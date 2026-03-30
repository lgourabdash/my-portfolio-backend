package com.lgourabdash.portfolio;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AdminErrorLogService {

    private static final int MAX_ENTRIES = 400;

    private final ArrayList<AdminErrorLogEntry> buffer = new ArrayList<>();

    public void logFrontendReport(
            String message, String detail, String path, String code) {
        String fix =
                AdminErrorLogFixHints.forFrontend(
                        message, code, path == null ? "" : path);
        add(
                new AdminErrorLogEntry(
                        UUID.randomUUID().toString(),
                        Instant.now(),
                        "FRONTEND",
                        truncate(message, 500),
                        truncate(detail, 8000),
                        truncate(path, 512),
                        null,
                        truncate(code, 64),
                        fix));
    }

    public void logBackendException(
            Throwable ex, String path, Integer httpStatus) {
        String msg = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
        String fix = AdminErrorLogFixHints.forBackend(msg, httpStatus, path == null ? "" : path);
        add(
                new AdminErrorLogEntry(
                        UUID.randomUUID().toString(),
                        Instant.now(),
                        "BACKEND",
                        truncate(ex.getClass().getSimpleName() + ": " + msg, 500),
                        truncate(stackTrace(ex), 8000),
                        truncate(path, 512),
                        httpStatus,
                        null,
                        fix));
    }

    public List<AdminErrorLogEntry> listNewestFirst() {
        synchronized (buffer) {
            return new ArrayList<>(buffer);
        }
    }

    public void clear() {
        synchronized (buffer) {
            buffer.clear();
        }
    }

    /** Mark a log entry as solved / closed. Returns false if id not found. */
    public boolean closeById(String id) {
        if (id == null || id.isBlank()) {
            return false;
        }
        synchronized (buffer) {
            for (AdminErrorLogEntry e : buffer) {
                if (id.equals(e.getId())) {
                    e.markClosed();
                    return true;
                }
            }
        }
        return false;
    }

    private void add(AdminErrorLogEntry entry) {
        synchronized (buffer) {
            buffer.add(0, entry);
            while (buffer.size() > MAX_ENTRIES) {
                buffer.remove(buffer.size() - 1);
            }
        }
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        if (s.length() <= max) {
            return s;
        }
        return s.substring(0, max) + "…";
    }

    private static String stackTrace(Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
}
