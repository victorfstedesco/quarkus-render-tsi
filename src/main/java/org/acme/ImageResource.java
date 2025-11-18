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

@Path("/image")
public class ImageResource {

    // --------------------------
    // GET ALL
    // --------------------------
    @GET
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

        // HATEOAS
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

    // --------------------------
    // GET BY ID
    // --------------------------
    @GET
    @Path("{id}")
    @Operation(
            summary = "Imagem por ID",
            description = "Retorna uma imagem específica pelo ID"
    )
    @APIResponse(
            responseCode = "200",
            description = "Sucesso",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = Image.class, type = SchemaType.ARRAY)
            )
    )
    @APIResponse(
            responseCode = "404",
            description = "Imagem não encontrada"
    )
    public Response getById(
            @Parameter(description = "ID da imagem", required = true)
            @PathParam("id") long id) {

        Image entity = Image.findById(id);
        if (entity == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        // HATEOAS
        entity.links = Map.of(
                "self", "/image/" + id,
                "update", "/image/" + id,
                "delete", "/image/" + id,
                "all", "/image"
        );

        return Response.ok(entity).build();
    }

    // --------------------------
    // SEARCH
    // --------------------------
    @GET
    @Path("/search")
    @Operation(
            summary = "Todas as imagens com função de busca",
            description = "Todos os resultados no formato JSON"
    )
    @APIResponse(responseCode = "200", description = "Sucesso")
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
                "desc".equalsIgnoreCase(direction) ?
                        Sort.Direction.Descending : Sort.Direction.Ascending
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

        // HATEOAS
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

    // --------------------------
    // INSERT
    // --------------------------
    @POST
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
    @APIResponse(responseCode = "400", description = "Requisição inválida")
    @Transactional
    public Response insert(Image image) {

        Image.persist(image);

        // HATEOAS
        image.links = Map.of(
                "self", "/image/" + image.id,
                "update", "/image/" + image.id,
                "delete", "/image/" + image.id,
                "all", "/image"
        );

        return Response.status(Response.Status.CREATED).entity(image).build();
    }

    // --------------------------
    // DELETE
    // --------------------------
    @DELETE
    @Path("{id}")
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

    // --------------------------
    // UPDATE
    // --------------------------
    @PUT
    @Path("{id}")
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

        // HATEOAS
        entity.links = Map.of(
                "self", "/image/" + entity.id,
                "update", "/image/" + entity.id,
                "delete", "/image/" + entity.id,
                "all", "/image"
        );

        return Response.ok(entity).build();
    }
}
