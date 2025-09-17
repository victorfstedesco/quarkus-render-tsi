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

@Path("/brand")
public class BrandResource {

    @GET
    @Operation(
            summary = "All brands (getAll)",
            description = "Brand list in JSON format"
    )
    @APIResponse(
            responseCode = "200",
            description = "Sucess",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = Brand.class, type = SchemaType.ARRAY)
            )
    )
    public Response getAll(){
        return Response.ok(Brand.listAll()).build();
    }

    @GET
    @Path("{id}")
    @Operation(
            summary = "Brand by ID",
            description = "Retorna um filme específico pela busca de ID colocado na URL no formato JSON por padrão"
    )
    @APIResponse(
            responseCode = "200",
            description = "Sucess",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = Brand.class, type = SchemaType.ARRAY)
            )
    )
    @APIResponse(
            responseCode = "404",
            description = "Not found",
            content = @Content(
                    mediaType = "text/plain",
                    schema = @Schema(implementation = String.class))
    )
    public Response getById(
            @Parameter(description = "Brand ID for search", required = true)
            @PathParam("id") long id){
        Brand entity = Brand.findById(id);
        if(entity == null){
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(entity).build();
    }

    @GET
    @Operation(
            summary = "All Brand with search function",
            description = "All result in JSON format"
    )
    @APIResponse(
            responseCode = "200",
            description = "Sucess",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = Brand.class, type = SchemaType.ARRAY)
            )
    )
    @Path("/search") // Temos uma response do tipo ok 200
    public Response search(
            @Parameter(description = "Query de buscar por titulo, ano de lançamento ou idade indicativa")
            @QueryParam("q") String q,
            @Parameter(description = "Campo de ordenação da lista de retorno")
            @QueryParam("sort") @DefaultValue("id") String sort,
            @Parameter(description = "Esquema de filtragem de livros por ordem crescente ou decrescente")
            @QueryParam("direction") @DefaultValue("asc") String direction,
            @Parameter(description = "Define qual página será retornada na response")
            @QueryParam("page") @DefaultValue("0") int page,
            @Parameter(description = "Define quantos objetos serão retornados por query")
            @QueryParam("size") @DefaultValue("4") int size
    ){
        Set<String> allowed = Set.of("id", "name", "description", "logoUrl ", "websiteUrl ", "release");
        if(!allowed.contains(sort)){
            sort = "id";
        }

        Sort sortObj = Sort.by(
                sort,
                "desc".equalsIgnoreCase(direction) ? Sort.Direction.Descending : Sort.Direction.Ascending
        );

        int effectivePage = Math.max(page, 0);

        PanacheQuery<Brand> query;

        if (q == null || q.isBlank()) {
            query = Brand.findAll(sortObj);
        } else {
            try {
                // tenta converter a pesquisa em número
                int numero = Integer.parseInt(q);

                // busca apenas em campos numéricos
                query = Brand.find(
                        "release = ?1 or release = ?1",
                        sortObj,
                        numero
                );

            } catch (NumberFormatException e) {
                // se não for número, busca só em campos textuais
                query = Brand.find(
                        "lower(name) like ?1",
                        sortObj,
                        "%" + q.toLowerCase() + "%"
                );
            }
        }

        List<Brand> Brand = query.page(effectivePage, size).list();

        var response = new SearchBrandResponse();
        response.Brand = Brand;
        response.TotalBrand = query.list().size();
        response.TotalPages = query.pageCount();
        response.HasMore = effectivePage < query.pageCount() - 1; // Faz o pagecount - 1 pois a pagina 1 seria o indice 0, a comparação é feita com o índice da última página valida
        response.NextPage = response.HasMore ? "http://localhost:8080/brand/search?q="+(q != null ? q : "")+"&page="+(effectivePage + 1) + (size > 0 ? "&size="+size : "") : "";

        return Response.ok(response).build();
    }

    @POST
    @Operation(
            summary = "Brand insert",
            description = "Adiciona um item a lista de filmes por meio de POST e request body JSON"
    )
    @RequestBody(
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = Brand.class)
            )
    )
    @APIResponse(
            responseCode = "201",
            description = "Created",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = Brand.class))
    )
    @APIResponse(
            responseCode = "400",
            description = "Bad Request",
            content = @Content(
                    mediaType = "text/plain",
                    schema = @Schema(implementation = String.class))
    )
    @Transactional
    public Response insert(Brand brand){
        Brand.persist(brand);
        return Response.status(Response.Status.CREATED).build();
    }

    @DELETE
    @Operation(
            summary = "Brand delete",
            description = "Remove um item da lista de filmes por meio de Id na URL"
    )
    @APIResponse(
            responseCode = "204",
            description = "Sem conteúdo",
            content = @Content(
                    mediaType = "text/plain",
                    schema = @Schema(implementation = String.class))
    )
    @APIResponse(
            responseCode = "404",
            description = "Item não encontrado",
            content = @Content(
                    mediaType = "text/plain",
                    schema = @Schema(implementation = String.class))
    )
    @Transactional
    @Path("{id}")
    public Response delete(@PathParam("id") long id){
        Brand entity = Brand.findById(id);
        if(entity == null){
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        Brand.deleteById(id);
        return Response.noContent().build();
    }

    @PUT
    @Operation(
            summary = "Brand edit",
            description = "Edita um item da lista de filmes por meio de Id na URL e request body JSON"
    )
    @RequestBody(
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = Brand.class)
            )
    )
    @APIResponse(
            responseCode = "200",
            description = "Item editado com sucesso",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = Brand.class, type = SchemaType.ARRAY)
            )
    )
    @APIResponse(
            responseCode = "404",
            description = "Item não encontrado",
            content = @Content(
                    mediaType = "text/plain",
                    schema = @Schema(implementation = String.class))
    )
    @Transactional
    @Path("{id}") // Temos 2 responses uma de not found e outra de ok
    public Response update(@PathParam("id") long id, Brand newBrand){
        Brand entity = Brand.findById(id);
        if(entity == null){
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        entity.name  = newBrand.name ;
        entity.description  = newBrand.description ;
        entity.logoUrl = newBrand.logoUrl;
        entity.websiteUrl  = newBrand.websiteUrl ;
        entity.release = newBrand.release;

        return Response.status(Response.Status.OK).entity(entity).build();
    }
}
