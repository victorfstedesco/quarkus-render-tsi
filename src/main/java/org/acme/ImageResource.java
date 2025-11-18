package org.acme;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Sort;
import io.smallrye.faulttolerance.api.RateLimit;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Timeout;

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

@Path("/image")
public class ImageResource {

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
            summary = "Todas as imagens (getAll)",
            description = "Lista de imagens no formato JSON"
    )
    @APIResponse(
            responseCode = "200",
            description = "Sucesso",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = Image.class, type = SchemaType.ARRAY)
            )
    )
    public Response getAll() {

        List<Image> list = Image.listAll();

        list.forEach(img -> {
            img.links = Map.of(
                    "self", "/image/" + img.id,
                    "update", "/image/" + img.id,
                    "delete", "/image/" + img.id,
                    "all", "/image"
            );
        });

        return Response.ok(list).build();
    }

    public Response getAllFallback() {
        return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                .entity("Serviço indisponível para listar imagens.")
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
            summary = "Imagem por ID",
            description = "Retorna uma imagem específica pelo ID"
    )
    @APIResponse(
            responseCode = "200",
            description = "Sucesso",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = Image.class)
            )
    )
    @APIResponse(responseCode = "404", description = "Imagem não encontrada")
    public Response getById(
            @Parameter(description = "ID da imagem", required = true)
            @PathParam("id") long id) {

        Image entity = Image.findById(id);
        if (entity == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        entity.links = Map.of(
                "self", "/image/" + id,
                "update", "/image/" + id,
                "delete", "/image/" + id,
                "all", "/image"
        );

        return Response.ok(entity).build();
    }

    public Response getByIdFallback(long id) {
        return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                .entity("Serviço indisponível ao buscar imagem id=" + id)
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
    @Operation(summary = "Busca imagens", description = "Retorna todas as imagens com filtros")
    public Response search(
            @QueryParam("q") String q,
            @QueryParam("sort") @DefaultValue("id") String sort,
            @QueryParam("direction") @DefaultValue("asc") String direction,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("4") int size
    ) {

        Set<String> allowed = Set.of("id", "url", "description");
        if (!allowed.contains(sort)) sort = "id";

        Sort sortObj = Sort.by(
                sort,
                direction.equalsIgnoreCase("desc")
                        ? Sort.Direction.Descending
                        : Sort.Direction.Ascending
        );

        int effectivePage = Math.max(page, 0);

        PanacheQuery<Image> query;

        if (q == null || q.isBlank()) {
            query = Image.findAll(sortObj);
        } else {
            query = Image.find(
                    "lower(url) like ?1 or lower(description) like ?1",
                    sortObj,
                    "%" + q.toLowerCase() + "%"
            );
        }

        List<Image> images = query.page(effectivePage, size).list();

        images.forEach(img -> {
            img.links = Map.of(
                    "self", "/image/" + img.id,
                    "update", "/image/" + img.id,
                    "delete", "/image/" + img.id,
                    "all", "/image"
            );
        });

        return Response.ok(images).build();
    }

    public Response searchFallback(String q, String sort, String direction, int page, int size) {
        return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                .entity("Serviço indisponível para buscar imagens.")
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
    @Operation(
            summary = "Inserir imagem",
            description = "Adiciona uma imagem via POST"
    )
    @RequestBody(
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = Image.class)
            )
    )
    @APIResponse(responseCode = "201", description = "Imagem criada com sucesso")
    @Transactional
    public Response insert(Image image) {

        Image.persist(image);

        image.links = Map.of(
                "self", "/image/" + image.id,
                "update", "/image/" + image.id,
                "delete", "/image/" + image.id,
                "all", "/image"
        );

        return Response.status(Response.Status.CREATED).entity(image).build();
    }

    public Response insertFallback(Image image) {
        return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                .entity("Serviço indisponível ao inserir imagem.")
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
    @Operation(
            summary = "Deletar imagem",
            description = "Remove uma imagem pelo ID"
    )
    @Transactional
    public Response delete(@PathParam("id") long id) {

        Image entity = Image.findById(id);
        if (entity == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        Image.deleteById(id);

        return Response.noContent().build();
    }

    public Response deleteFallback(long id) {
        return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                .entity("Serviço indisponível ao deletar imagem id=" + id)
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
    @Operation(
            summary = "Editar imagem",
            description = "Edita uma imagem pelo ID"
    )
    @Transactional
    public Response update(@PathParam("id") long id, Image newImage) {

        Image entity = Image.findById(id);
        if (entity == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        entity.url = newImage.url;
        entity.description = newImage.description;

        entity.links = Map.of(
                "self", "/image/" + entity.id,
                "update", "/image/" + entity.id,
                "delete", "/image/" + entity.id,
                "all", "/image"
        );

        return Response.ok(entity).build();
    }

    public Response updateFallback(long id, Image newImage) {
        return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                .entity("Serviço indisponível ao atualizar imagem id=" + id)
                .build();
    }
}
