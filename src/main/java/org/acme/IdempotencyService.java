package org.acme;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.Response;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@ApplicationScoped
public class IdempotencyService {

    private static final long IDEMPOTENCY_KEY_TTL_HOURS = 24;
    private final Map<String, IdempotencyRecord> idempotencyStore = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupScheduler = Executors.newScheduledThreadPool(1);

    public IdempotencyService() {
        cleanupScheduler.scheduleAtFixedRate(this::cleanupExpiredKeys, 1, 1, TimeUnit.HOURS);
    }

    public Response checkIdempotency(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return null;
        }

        IdempotencyRecord record = idempotencyStore.get(idempotencyKey);

        if (record != null) {

            if (record.isExpired()) {
                idempotencyStore.remove(idempotencyKey);
                return null;
            }

            if (record.isProcessing()) {
                return Response.status(409)
                        .entity("{\"error\": \"Request with this idempotency key is currently being processed\"}")
                        .build();
            }

            return Response.status(record.getStatusCode())
                    .entity(record.getResponseBody())
                    .build();
        }

        return null;
    }

    public void markAsProcessing(String idempotencyKey) {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            idempotencyStore.put(idempotencyKey, new IdempotencyRecord());
        }
    }

    public void storeResponse(String idempotencyKey, int statusCode, String responseBody) {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            idempotencyStore.put(idempotencyKey, new IdempotencyRecord(
                    statusCode,
                    responseBody,
                    Instant.now().plusSeconds(IDEMPOTENCY_KEY_TTL_HOURS * 3600)
            ));
        }
    }

    public void removeKey(String idempotencyKey) {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            idempotencyStore.remove(idempotencyKey);
        }
    }

    private void cleanupExpiredKeys() {
        Instant now = Instant.now();
        idempotencyStore.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    private static class IdempotencyRecord {

        private final int statusCode;
        private final String responseBody;
        private final Instant expiresAt;
        private final boolean processing;

        public IdempotencyRecord() {
            this.statusCode = 0;
            this.responseBody = null;
            this.expiresAt = Instant.now().plusSeconds(300);
            this.processing = true;
        }

        public IdempotencyRecord(int statusCode, String responseBody, Instant expiresAt) {
            this.statusCode = statusCode;
            this.responseBody = responseBody;
            this.expiresAt = expiresAt;
            this.processing = false;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public String getResponseBody() {
            return responseBody;
        }

        public boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }

        public boolean isProcessing() {
            return processing;
        }
    }
}
