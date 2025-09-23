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

@Path("/image")
public class ImageResource {

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
        return Response.ok(Image.listAll()).build();
    }

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
            description = "Imagem não encontrada",
            content = @Content(
                    mediaType = "text/plain",
                    schema = @Schema(implementation = String.class))
    )
    public Response getById(
            @Parameter(description = "ID da imagem para busca", required = true)
            @PathParam("id") long id) {
        Image entity = Image.findById(id);
        if (entity == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(entity).build();
    }

    @GET
    @Operation(
            summary = "Todas as imagens com função de busca",
            description = "Todos os resultados no formato JSON"
    )
    @APIResponse(
            responseCode = "200",
            description = "Sucesso",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = Image.class, type = SchemaType.ARRAY)
            )
    )
    @Path("/search")
    public Response search(
            @Parameter(description = "Consulta para busca por url ou descrição")
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
        Set<String> allowed = Set.of("id", "url", "description");
        if (!allowed.contains(sort)) {
            sort = "id";
        }

        Sort sortObj = Sort.by(
                sort,
                "desc".equalsIgnoreCase(direction) ? Sort.Direction.Descending : Sort.Direction.Ascending
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

        return Response.ok(images).build();
    }

    @POST
    @Operation(
            summary = "Inserir imagem",
            description = "Adiciona uma imagem via POST com corpo JSON"
    )
    @RequestBody(
            required = true,
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Image.class))
    )
    @APIResponse(
            responseCode = "201",
            description = "Imagem criada com sucesso",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Image.class))
    )
    @APIResponse(
            responseCode = "400",
            description = "Requisição inválida",
            content = @Content(mediaType = "text/plain", schema = @Schema(implementation = String.class))
    )
    @Transactional
    public Response insert(Image image) {
        Image.persist(image);
        return Response.status(Response.Status.CREATED).entity(image).build();
    }

    @DELETE
    @Operation(
            summary = "Deletar imagem",
            description = "Remove uma imagem pelo ID"
    )
    @APIResponse(
            responseCode = "204",
            description = "Sem conteúdo",
            content = @Content(mediaType = "text/plain", schema = @Schema(implementation = String.class))
    )
    @APIResponse(
            responseCode = "404",
            description = "Imagem não encontrada",
            content = @Content(mediaType = "text/plain", schema = @Schema(implementation = String.class))
    )
    @Transactional
    @Path("{id}")
    public Response delete(@PathParam("id") long id) {
        Image entity = Image.findById(id);
        if (entity == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        Image.deleteById(id);
        return Response.noContent().build();
    }

    @PUT
    @Operation(
            summary = "Editar imagem",
            description = "Edita uma imagem pelo ID e corpo JSON"
    )
    @RequestBody(
            required = true,
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Image.class))
    )
    @APIResponse(
            responseCode = "200",
            description = "Imagem editada com sucesso",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Image.class, type = SchemaType.ARRAY))
    )
    @APIResponse(
            responseCode = "404",
            description = "Imagem não encontrada",
            content = @Content(mediaType = "text/plain", schema = @Schema(implementation = String.class))
    )
    @Transactional
    @Path("{id}")
    public Response update(@PathParam("id") long id, Image newImage) {
        Image entity = Image.findById(id);
        if (entity == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        entity.url = newImage.url;
        entity.description = newImage.description;
        return Response.status(Response.Status.OK).entity(entity).build();
    }
}
