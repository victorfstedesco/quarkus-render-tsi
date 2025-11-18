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

@Path("/brand")
public class BrandResource {

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
    @Operation(summary = "Todas as marcas (getAll)", description = "Lista de marcas no formato JSON")
    @APIResponse(responseCode = "200", description = "Sucesso",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Brand.class, type = SchemaType.ARRAY)))
    public Response getAll() {

        List<Brand> list = Brand.listAll();

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

    public Response getAllFallback() {
        return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                .entity("Serviço indisponível no momento. (brand.getAll)")
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
    @Operation(summary = "Marca por ID", description = "Retorna uma marca específica pelo ID")
    @APIResponse(responseCode = "200", description = "Sucesso",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Brand.class)))
    public Response getById(@PathParam("id") long id) {

        Brand entity = Brand.findById(id);
        if (entity == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        entity.links = Map.of(
                "self", "/brand/" + id,
                "update", "/brand/" + id,
                "delete", "/brand/" + id,
                "all", "/brand"
        );

        return Response.ok(entity).build();
    }

    public Response getByIdFallback(long id) {
        return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                .entity("Falha ao buscar marca id=" + id)
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
    @Operation(summary = "Busca marcas", description = "Busca com paginação e ordenação")
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
                direction.equalsIgnoreCase("desc") ?
                        Sort.Direction.Descending : Sort.Direction.Ascending
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
        response.NextPage = response.HasMore ?
                "/brand/search?q=" + (q != null ? q : "") + "&page=" + (effectivePage + 1) :
                "";

        return Response.ok(response).build();
    }

    public Response searchFallback(String q, String sort, String direction, int page, int size) {
        return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                .entity("Serviço indisponível na busca de marcas.")
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
    @Operation(summary = "Inserir marca", description = "Adiciona uma nova marca")
    @Transactional
    public Response insert(Brand brand) {

        if (brand.logo != null && brand.logo.id != null)
            brand.logo = Image.findById(brand.logo.id);

        if (brand.segment != null && brand.segment.id != null)
            brand.segment = Segment.findById(brand.segment.id);

        Brand.persist(brand);

        brand.links = Map.of(
                "self", "/brand/" + brand.id,
                "update", "/brand/" + brand.id,
                "delete", "/brand/" + brand.id,
                "all", "/brand"
        );

        return Response.status(Response.Status.CREATED).entity(brand).build();
    }

    public Response insertFallback(Brand brand) {
        return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                .entity("Falha ao inserir marca.")
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
    @Transactional
    public Response delete(@PathParam("id") long id) {

        Brand entity = Brand.findById(id);
        if (entity == null)
            return Response.status(Response.Status.NOT_FOUND).build();

        Brand.deleteById(id);
        return Response.noContent().build();
    }

    public Response deleteFallback(long id) {
        return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                .entity("Falha ao deletar marca id=" + id)
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
    @Transactional
    public Response update(@PathParam("id") long id, Brand newBrand) {

        Brand entity = Brand.findById(id);
        if (entity == null)
            return Response.status(Response.Status.NOT_FOUND).build();

        entity.name = newBrand.name;
        entity.description = newBrand.description;
        entity.websiteUrl = newBrand.websiteUrl;
        entity.release = newBrand.release;

        if (newBrand.logo != null && newBrand.logo.id != null)
            entity.logo = Image.findById(newBrand.logo.id);

        if (newBrand.segment != null && newBrand.segment.id != null)
            entity.segment = Segment.findById(newBrand.segment.id);

        entity.links = Map.of(
                "self", "/brand/" + entity.id,
                "update", "/brand/" + entity.id,
                "delete", "/brand/" + entity.id,
                "all", "/brand"
        );

        return Response.ok(entity).build();
    }

    public Response updateFallback(long id, Brand newBrand) {
        return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                .entity("Falha ao atualizar marca id=" + id)
                .build();
    }
}
