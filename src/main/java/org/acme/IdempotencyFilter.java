package org.acme;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

@Provider
@Priority(2000)
public class IdempotencyFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
    private static final String IDEMPOTENCY_KEY_PROPERTY = "idempotency.key";

    @Inject
    IdempotencyService idempotencyService;

    @Inject
    ObjectMapper objectMapper;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {

        if (!"POST".equalsIgnoreCase(requestContext.getMethod())) {
            return;
        }

        String idempotencyKey = requestContext.getHeaderString(IDEMPOTENCY_KEY_HEADER);

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {

            requestContext.setProperty(IDEMPOTENCY_KEY_PROPERTY, idempotencyKey);

            Response cachedResponse = idempotencyService.checkIdempotency(idempotencyKey);

            if (cachedResponse != null) {

                Response responseWithCors = Response.fromResponse(cachedResponse)
                        .header("Access-Control-Allow-Origin", "*")
                        .header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS")
                        .header("Access-Control-Allow-Headers", "Content-Type, Accept, Authorization, Idempotency-Key")
                        .header("Access-Control-Allow-Credentials", "true")
                        .type(MediaType.APPLICATION_JSON)
                        .build();

                requestContext.abortWith(responseWithCors);
                return;
            }

            idempotencyService.markAsProcessing(idempotencyKey);

            if (requestContext.hasEntity()) {

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                InputStream entityStream = requestContext.getEntityStream();
                byte[] buffer = new byte[1024];
                int length;

                while ((length = entityStream.read(buffer)) != -1) {
                    baos.write(buffer, 0, length);
                }

                byte[] requestBody = baos.toByteArray();

                requestContext.setEntityStream(new ByteArrayInputStream(requestBody));
            }
        }
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
            throws IOException {

        if (!"POST".equalsIgnoreCase(requestContext.getMethod())) {
            return;
        }

        String idempotencyKey = (String) requestContext.getProperty(IDEMPOTENCY_KEY_PROPERTY);

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {

            int statusCode = responseContext.getStatus();

            if (statusCode >= 200 && statusCode < 500) {

                Object entity = responseContext.getEntity();
                String responseBody = "";

                if (entity != null) {
                    try {
                        responseBody = objectMapper.writeValueAsString(entity);
                    } catch (Exception e) {
                        System.err.println("Erro ao serializar resposta para idempotência: " + e.getMessage());
                        responseBody = "{\"error\": \"Serialization failed\"}";
                    }
                }

                idempotencyService.storeResponse(idempotencyKey, statusCode, responseBody);

            } else {
                idempotencyService.removeKey(idempotencyKey);
            }
        }

        if (!responseContext.getHeaders().containsKey("Access-Control-Allow-Origin")) {
            responseContext.getHeaders().add("Access-Control-Allow-Origin", "*");
            responseContext.getHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            responseContext.getHeaders().add("Access-Control-Allow-Headers", "Content-Type, Accept, Authorization, Idempotency-Key");
            responseContext.getHeaders().add("Access-Control-Allow-Credentials", "true");
        }
    }
}
