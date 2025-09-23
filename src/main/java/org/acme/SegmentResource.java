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
import java.util.Set;

@Path("/segment")
public class SegmentResource {

    @GET
    @Operation(
            summary = "Todos os segmentos (getAll)",
            description = "Lista de segmentos no formato JSON"
    )
    @APIResponse(
            responseCode = "200",
            description = "Sucesso",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = Segment.class, type = SchemaType.ARRAY)
            )
    )
    public Response getAll() {
        return Response.ok(Segment.listAll()).build();
    }

    @GET
    @Path("{id}")
    @Operation(
            summary = "Segmento por ID",
            description = "Retorna um segmento específico pelo ID"
    )
    @APIResponse(
            responseCode = "200",
            description = "Sucesso",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = Segment.class, type = SchemaType.ARRAY)
            )
    )
    @APIResponse(
            responseCode = "404",
            description = "Segmento não encontrado",
            content = @Content(
                    mediaType = "text/plain",
                    schema = @Schema(implementation = String.class))
    )
    public Response getById(
            @Parameter(description = "ID do segmento para busca", required = true)
            @PathParam("id") long id) {
        Segment entity = Segment.findById(id);
        if (entity == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(entity).build();
    }

    @GET
    @Operation(
            summary = "Todos os segmentos com função de busca",
            description = "Todos os resultados no formato JSON"
    )
    @APIResponse(
            responseCode = "200",
            description = "Sucesso",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = Segment.class, type = SchemaType.ARRAY)
            )
    )
    @Path("/search")
    public Response search(
            @Parameter(description = "Consulta para busca por nome ou descrição")
            @QueryParam("q") String q,
            @Parameter(description = "Campo para ordenação da lista")
            @QueryParam("sort") @DefaultValue("id") String sort,
            @Parameter(description = "Direção da ordenação: ascendente ou descendente")
            @QueryParam("direction") @DefaultValue("asc") String direction,
            @Parameter(description = "Número da página a ser retornada")
            @QueryParam("page") @DefaultValue("0") int page,
            @Parameter(description = "Quantidade de itens por página")
            @QueryParam("size") @DefaultValue("4") int size
    ) {
        Set<String> allowed = Set.of("id", "name", "description");
        if (!allowed.contains(sort)) {
            sort = "id";
        }

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

        return Response.ok(segments).build();
    }

    @POST
    @Operation(
            summary = "Inserir segmento",
            description = "Adiciona um segmento via POST com corpo JSON"
    )
    @RequestBody(
            required = true,
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Segment.class))
    )
    @APIResponse(
            responseCode = "201",
            description = "Segmento criado com sucesso",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Segment.class))
    )
    @APIResponse(
            responseCode = "400",
            description = "Requisição inválida",
            content = @Content(mediaType = "text/plain", schema = @Schema(implementation = String.class))
    )
    @Transactional
    public Response insert(Segment segment) {
        Segment.persist(segment);
        return Response.status(Response.Status.CREATED).entity(segment).build();
    }

    @DELETE
    @Operation(
            summary = "Deletar segmento",
            description = "Remove um segmento pelo ID"
    )
    @APIResponse(
            responseCode = "204",
            description = "Sem conteúdo",
            content = @Content(mediaType = "text/plain", schema = @Schema(implementation = String.class))
    )
    @APIResponse(
            responseCode = "404",
            description = "Segmento não encontrado",
            content = @Content(mediaType = "text/plain", schema = @Schema(implementation = String.class))
    )
    @Transactional
    @Path("{id}")
    public Response delete(@PathParam("id") long id) {
        Segment entity = Segment.findById(id);
        if (entity == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        Segment.deleteById(id);
        return Response.noContent().build();
    }

    @PUT
    @Operation(
            summary = "Editar segmento",
            description = "Edita um segmento pelo ID e corpo JSON"
    )
    @RequestBody(
            required = true,
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Segment.class))
    )
    @APIResponse(
            responseCode = "200",
            description = "Segmento editado com sucesso",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Segment.class, type = SchemaType.ARRAY))
    )
    @APIResponse(
            responseCode = "404",
            description = "Segmento não encontrado",
            content = @Content(mediaType = "text/plain", schema = @Schema(implementation = String.class))
    )
    @Transactional
    @Path("{id}")
    public Response update(@PathParam("id") long id, Segment newSegment) {
        Segment entity = Segment.findById(id);
        if (entity == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        entity.name = newSegment.name;
        entity.description = newSegment.description;
        return Response.status(Response.Status.OK).entity(entity).build();
    }
}
