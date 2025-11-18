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

@Path("/brand")
public class BrandResource {

    // --------------------------
    // GET ALL
    // --------------------------
    @GET
    @Operation(summary = "Todas as marcas (getAll)", description = "Lista de marcas no formato JSON")
    @APIResponse(responseCode = "200", description = "Sucesso",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Brand.class, type = SchemaType.ARRAY)))
    public Response getAll() {

        List<Brand> list = Brand.listAll();

        // HATEOAS para cada item
        list.forEach(b -> {
            b.links = Map.of(
                    "self", "/brand/" + b.id,
                    "update", "/brand/" + b.id,
                    "delete", "/brand/" + b.id,
                    "all", "/brand"
            );
        });

        return Response.ok(list).build();
    }

    // --------------------------
    // GET BY ID
    // --------------------------
    @GET
    @Path("{id}")
    @Operation(summary = "Marca por ID", description = "Retorna uma marca específica pelo ID")
    @APIResponse(responseCode = "200", description = "Sucesso",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Brand.class)))
    @APIResponse(responseCode = "404", description = "Marca não encontrada")
    public Response getById(@Parameter(description = "ID da marca", required = true)
                            @PathParam("id") long id) {

        Brand entity = Brand.findById(id);
        if (entity == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        // HATEOAS
        entity.links = Map.of(
                "self", "/brand/" + id,
                "update", "/brand/" + id,
                "delete", "/brand/" + id,
                "all", "/brand"
        );

        return Response.ok(entity).build();
    }

    // --------------------------
    // SEARCH
    // --------------------------
    @GET
    @Path("/search")
    @Operation(summary = "Todas as marcas com função de busca",
            description = "Todos os resultados no formato JSON")
    public Response search(
            @QueryParam("q") String q,
            @QueryParam("sort") @DefaultValue("id") String sort,
            @QueryParam("direction") @DefaultValue("asc") String direction,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("4") int size
    ) {

        Set<String> allowed = Set.of("id", "name", "description", "websiteUrl", "release");
        if (!allowed.contains(sort)) sort = "id";

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
                int numero = Integer.parseInt(q);
                query = Brand.find("release = ?1", sortObj, numero);
            } catch (NumberFormatException e) {
                query = Brand.find("lower(name) like ?1", sortObj, "%" + q.toLowerCase() + "%");
            }
        }

        List<Brand> brands = query.page(effectivePage, size).list();

        // HATEOAS em cada item
        brands.forEach(b -> {
            b.links = Map.of(
                    "self", "/brand/" + b.id,
                    "update", "/brand/" + b.id,
                    "delete", "/brand/" + b.id,
                    "all", "/brand"
            );
        });

        var response = new SearchBrandResponse();
        response.Brand = brands;
        response.TotalBrand = query.list().size();
        response.TotalPages = query.pageCount();
        response.HasMore = effectivePage < query.pageCount() - 1;
        response.NextPage = response.HasMore
                ? "/brand/search?q=" + (q != null ? q : "") + "&page=" + (effectivePage + 1) + "&size=" + size
                : "";

        return Response.ok(response).build();
    }

    // --------------------------
    // INSERT
    // --------------------------
    @POST
    @Operation(summary = "Inserir marca", description = "Adiciona uma marca via POST")
    @RequestBody(required = true, content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = Brand.class)))
    @Transactional
    public Response insert(Brand brand) {

        if (brand.logo != null && brand.logo.id != null)
            brand.logo = Image.findById(brand.logo.id);

        if (brand.segment != null && brand.segment.id != null)
            brand.segment = Segment.findById(brand.segment.id);

        Brand.persist(brand);

        // HATEOAS
        brand.links = Map.of(
                "self", "/brand/" + brand.id,
                "update", "/brand/" + brand.id,
                "delete", "/brand/" + brand.id,
                "all", "/brand"
        );

        return Response.status(Response.Status.CREATED).entity(brand).build();
    }

    // --------------------------
    // DELETE
    // --------------------------
    @DELETE
    @Path("{id}")
    @Transactional
    public Response delete(@PathParam("id") long id) {
        Brand entity = Brand.findById(id);
        if (entity == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        Brand.deleteById(id);
        return Response.noContent().build();
    }

    // --------------------------
    // UPDATE
    // --------------------------
    @PUT
    @Path("{id}")
    @Transactional
    public Response update(@PathParam("id") long id, Brand newBrand) {

        Brand entity = Brand.findById(id);
        if (entity == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        entity.name = newBrand.name;
        entity.description = newBrand.description;
        entity.websiteUrl = newBrand.websiteUrl;
        entity.release = newBrand.release;

        if (newBrand.logo != null && newBrand.logo.id != null)
            entity.logo = Image.findById(newBrand.logo.id);

        if (newBrand.segment != null && newBrand.segment.id != null)
            entity.segment = Segment.findById(newBrand.segment.id);

        // HATEOAS
        entity.links = Map.of(
                "self", "/brand/" + entity.id,
                "update", "/brand/" + entity.id,
                "delete", "/brand/" + entity.id,
                "all", "/brand"
        );

        return Response.ok(entity).build();
    }
}
