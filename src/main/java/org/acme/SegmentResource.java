package org.acme;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Sort;

import io.smallrye.faulttolerance.api.RateLimit;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Path("/segment")
public class SegmentResource {

    // =========================================================
    // GET ALL
    // =========================================================
    @GET
    @Timeout(30000)
    @RateLimit(value = 10, window = 10, windowUnit = ChronoUnit.SECONDS)
    @CircuitBreaker(
            requestVolumeThreshold = 4,
            failureRatio = 0.5,
            delay = 5000,
            successThreshold = 1
    )
    @Fallback(fallbackMethod = "getAllFallback")
    @Operation(
            summary = "Todos os segmentos (getAll)",
            description = "Lista de segmentos no formato JSON"
    )
    @APIResponse(
            responseCode = "200",
            description = "Sucesso",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Segment.class, type = SchemaType.ARRAY))
    )
    public Response getAll() {

        List<Segment> list = Segment.listAll();

        list.forEach(s -> {
            s.links = Map.of(
                    "self", "/segment/" + s.id,
                    "update", "/segment/" + s.id,
                    "delete", "/segment/" + s.id,
                    "all", "/segment"
            );
        });

        return Response.ok(list).build();
    }

    public Response getAllFallback() {
        return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                .entity("Serviço indisponível para listar segmentos.")
                .build();
    }


    // =========================================================
    // GET BY ID
    // =========================================================
    @GET
    @Path("{id}")
    @Timeout(25000)
    @RateLimit(value = 8, window = 10, windowUnit = ChronoUnit.SECONDS)
    @CircuitBreaker(
            requestVolumeThreshold = 4,
            failureRatio = 0.5,
            delay = 5000,
            successThreshold = 1
    )
    @Fallback(fallbackMethod = "getByIdFallback")
    @Operation(
            summary = "Segmento por ID",
            description = "Retorna um segmento específico pelo ID"
    )
    @APIResponse(
            responseCode = "200",
            description = "Sucesso",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Segment.class))
    )
    public Response getById(
            @Parameter(description = "ID do segmento", required = true)
            @PathParam("id") long id) {

        Segment entity = Segment.findById(id);
        if (entity == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        entity.links = Map.of(
                "self", "/segment/" + id,
                "update", "/segment/" + id,
                "delete", "/segment/" + id,
                "all", "/segment"
        );

        return Response.ok(entity).build();
    }

    public Response getByIdFallback(long id) {
        return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                .entity("Serviço indisponível ao buscar segmento id=" + id)
                .build();
    }


    // =========================================================
    // SEARCH
    // =========================================================
    @GET
    @Path("/search")
    @Timeout(15000)
    @RateLimit(value = 7, window = 10, windowUnit = ChronoUnit.SECONDS)
    @CircuitBreaker(
            requestVolumeThreshold = 4,
            failureRatio = 0.5,
            delay = 5000,
            successThreshold = 1
    )
    @Fallback(fallbackMethod = "searchFallback")
    @Operation(
            summary = "Busca de segmentos",
            description = "Resultados filtrados com paginação"
    )
    public Response search(
            @QueryParam("q") String q,
            @QueryParam("sort") @DefaultValue("id") String sort,
            @QueryParam("direction") @DefaultValue("asc") String direction,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("4") int size) {

        Set<String> allowed = Set.of("id", "name", "description");
        if (!allowed.contains(sort)) sort = "id";

        Sort sortObj = Sort.by(sort,
                direction.equalsIgnoreCase("desc")
                        ? Sort.Direction.Descending
                        : Sort.Direction.Ascending
        );

        int effectivePage = Math.max(page, 0);

        PanacheQuery<Segment> query;

        if (q == null || q.isBlank()) {
            query = Segment.findAll(sortObj);
        } else {
            query = Segment.find(
                    "lower(name) like ?1 or lower(description) like ?1",
                    sortObj,
                    "%" + q.toLowerCase() + "%"
            );
        }

        List<Segment> segments = query.page(effectivePage, size).list();

        segments.forEach(s -> {
            s.links = Map.of(
                    "self", "/segment/" + s.id,
                    "update", "/segment/" + s.id,
                    "delete", "/segment/" + s.id,
                    "all", "/segment"
            );
        });

        var response = new SearchSegmentResponse();
        response.segment = segments;
        response.totalSegment = query.list().size();
        response.totalPages = query.pageCount();
        response.hasMore = effectivePage < query.pageCount() - 1;

        response.nextPage = response.hasMore
                ? "/segment/search?q=" + (q != null ? q : "") + "&page=" + (effectivePage + 1) + "&size=" + size
                : "";

        return Response.ok(response).build();
    }

    public Response searchFallback(String q, String sort, String direction, int page, int size) {
        return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                .entity("Serviço indisponível ao buscar segmentos.")
                .build();
    }


    // =========================================================
    // INSERT
    // =========================================================
    @POST
    @Timeout(10000)
    @RateLimit(value = 3, window = 10, windowUnit = ChronoUnit.SECONDS)
    @CircuitBreaker(
            requestVolumeThreshold = 3,
            failureRatio = 0.5,
            delay = 10000,
            successThreshold = 1
    )
    @Fallback(fallbackMethod = "insertFallback")
    @Operation(summary = "Inserir segmento", description = "Adiciona um novo segmento")
    @RequestBody(required = true,
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Segment.class)))
    @Transactional
    public Response insert(Segment segment) {

        Segment.persist(segment);

        segment.links = Map.of(
                "self", "/segment/" + segment.id,
                "update", "/segment/" + segment.id,
                "delete", "/segment/" + segment.id,
                "all", "/segment"
        );

        return Response.status(Response.Status.CREATED).entity(segment).build();
    }

    public Response insertFallback(Segment segment) {
        return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                .entity("Serviço indisponível ao inserir segmento.")
                .build();
    }


    // =========================================================
    // DELETE
    // =========================================================
    @DELETE
    @Path("{id}")
    @Timeout(10000)
    @RateLimit(value = 3, window = 10, windowUnit = ChronoUnit.SECONDS)
    @CircuitBreaker(
            requestVolumeThreshold = 3,
            failureRatio = 0.5,
            delay = 10000,
            successThreshold = 1
    )
    @Fallback(fallbackMethod = "deleteFallback")
    @Operation(summary = "Deletar segmento", description = "Remove um segmento pelo ID")
    @Transactional
    public Response delete(@PathParam("id") long id) {

        Segment entity = Segment.findById(id);
        if (entity == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        Segment.deleteById(id);
        return Response.noContent().build();
    }

    public Response deleteFallback(long id) {
        return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                .entity("Serviço indisponível ao deletar segmento id=" + id)
                .build();
    }


    // =========================================================
    // UPDATE
    // =========================================================
    @PUT
    @Path("{id}")
    @Timeout(20000)
    @RateLimit(value = 3, window = 10, windowUnit = ChronoUnit.SECONDS)
    @CircuitBreaker(
            requestVolumeThreshold = 3,
            failureRatio = 0.5,
            delay = 10000,
            successThreshold = 1
    )
    @Fallback(fallbackMethod = "updateFallback")
    @Operation(summary = "Editar segmento", description = "Edita um segmento existente")
    @Transactional
    public Response update(@PathParam("id") long id, Segment newSegment) {

        Segment entity = Segment.findById(id);
        if (entity == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        entity.name = newSegment.name;
        entity.description = newSegment.description;

        entity.links = Map.of(
                "self", "/segment/" + entity.id,
                "update", "/segment/" + entity.id,
                "delete", "/segment/" + entity.id,
                "all", "/segment"
        );

        return Response.ok(entity).build();
    }

    public Response updateFallback(long id, Segment newSegment) {
        return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                .entity("Serviço indisponível ao atualizar segmento id=" + id)
                .build();
    }
}
