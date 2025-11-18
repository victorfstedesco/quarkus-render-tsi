package org.acme;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Sort;
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

import java.util.List;
import java.util.Map;
import java.util.Set;

@Path("/segment")
public class SegmentResource {

    // --------------------------
    // GET ALL
    // --------------------------
    @GET
    @Operation(summary = "Todos os segmentos (getAll)",
            description = "Lista de segmentos no formato JSON")
    @APIResponse(responseCode = "200", description = "Sucesso",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Segment.class, type = SchemaType.ARRAY)))
    public Response getAll() {

        List<Segment> list = Segment.listAll();

        // HATEOAS
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

    // --------------------------
    // GET BY ID
    // --------------------------
    @GET
    @Path("{id}")
    @Operation(summary = "Segmento por ID", description = "Retorna um segmento específico pelo ID")
    @APIResponse(responseCode = "200", description = "Sucesso",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Segment.class)))
    @APIResponse(responseCode = "404", description = "Segmento não encontrado")
    public Response getById(
            @Parameter(description = "ID do segmento", required = true)
            @PathParam("id") long id) {

        Segment entity = Segment.findById(id);
        if (entity == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        // HATEOAS
        entity.links = Map.of(
                "self", "/segment/" + id,
                "update", "/segment/" + id,
                "delete", "/segment/" + id,
                "all", "/segment"
        );

        return Response.ok(entity).build();
    }

    // --------------------------
    // SEARCH
    // --------------------------
    @GET
    @Path("/search")
    @Operation(summary = "Todos os segmentos com função de busca",
            description = "Todos os resultados no formato JSON")
    @APIResponse(responseCode = "200", description = "Sucesso")
    public Response search(
            @QueryParam("q") String q,
            @QueryParam("sort") @DefaultValue("id") String sort,
            @QueryParam("direction") @DefaultValue("asc") String direction,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("4") int size) {

        Set<String> allowed = Set.of("id", "name", "description");
        if (!allowed.contains(sort)) sort = "id";

        Sort sortObj = Sort.by(
                sort,
                "desc".equalsIgnoreCase(direction) ? Sort.Direction.Descending : Sort.Direction.Ascending
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

        // HATEOAS para cada item
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

    // --------------------------
    // INSERT
    // --------------------------
    @POST
    @Operation(summary = "Inserir segmento", description = "Adiciona um segmento via POST")
    @RequestBody(required = true,
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Segment.class)))
    @Transactional
    public Response insert(Segment segment) {

        Segment.persist(segment);

        // HATEOAS
        segment.links = Map.of(
                "self", "/segment/" + segment.id,
                "update", "/segment/" + segment.id,
                "delete", "/segment/" + segment.id,
                "all", "/segment"
        );

        return Response.status(Response.Status.CREATED).entity(segment).build();
    }

    // --------------------------
    // DELETE
    // --------------------------
    @DELETE
    @Path("{id}")
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

    // --------------------------
    // UPDATE
    // --------------------------
    @PUT
    @Path("{id}")
    @Operation(summary = "Editar segmento", description = "Edita um segmento pelo ID")
    @Transactional
    public Response update(@PathParam("id") long id, Segment newSegment) {

        Segment entity = Segment.findById(id);
        if (entity == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        entity.name = newSegment.name;
        entity.description = newSegment.description;

        // HATEOAS
        entity.links = Map.of(
                "self", "/segment/" + entity.id,
                "update", "/segment/" + entity.id,
                "delete", "/segment/" + entity.id,
                "all", "/segment"
        );

        return Response.ok(entity).build();
    }
}
