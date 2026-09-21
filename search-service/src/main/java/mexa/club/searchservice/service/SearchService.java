package mexa.club.searchservice.service;

import mexa.club.searchservice.document.ProductSearchDocument;
import mexa.club.searchservice.dto.ProductIndexRequest;
import mexa.club.searchservice.dto.SearchResponse;
import mexa.club.searchservice.repository.ProductSearchRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders.*;

@Service
@ConditionalOnProperty(name = "app.search.elasticsearch.enabled", havingValue = "true", matchIfMissing = true)
public class SearchService {
    private final ProductSearchRepository repository;
    private final ElasticsearchOperations operations;

    public SearchService(ProductSearchRepository repository, ElasticsearchOperations operations) {
        this.repository = repository;
        this.operations = operations;
    }

    public void index(ProductIndexRequest req) {
        ProductSearchDocument d = new ProductSearchDocument();
        d.setId(req.id());
        d.setName(req.name());
        d.setNameNgram(req.name());
        d.setBarcode(req.barcode());
        d.setDescription(req.description());
        d.setCategoryId(req.categoryId());
        d.setCategoryName(req.categoryName());
        d.setBrandId(req.brandId());
        d.setBrandName(req.brandName());
        d.setTags(req.tags());
        d.setSalePrice(req.salePrice());
        d.setActive(req.active());
        d.setImageUrl(req.imageUrl());
        d.setInStock(req.inStock());
        d.setCreatedAt(req.createdAt());
        repository.save(d);
    }

    public void delete(String id) {
        repository.deleteById(id);
    }

    public SearchResponse search(
            String q, String category, String brand, Double minPrice, Double maxPrice, Boolean inStock,
            List<String> tags, String sort, int page, int size
    ) {
        var bool = bool();
        if (q != null && !q.isBlank()) {
            bool.must(m -> m.multiMatch(mm -> mm.query(q).fields("name", "description", "brandName", "categoryName", "tags")));
        }
        if (category != null && !category.isBlank()) {
            bool.filter(f -> f.term(t -> t.field("categoryName").value(category)));
        }
        if (brand != null && !brand.isBlank()) {
            bool.filter(f -> f.term(t -> t.field("brandName").value(brand)));
        }
        if (minPrice != null || maxPrice != null) {
            bool.filter(f -> f.range(r -> {
                r.field("salePrice");
                if (minPrice != null) r.gte(co.elastic.clients.json.JsonData.of(minPrice));
                if (maxPrice != null) r.lte(co.elastic.clients.json.JsonData.of(maxPrice));
                return r;
            }));
        }
        if (inStock != null) {
            bool.filter(f -> f.term(t -> t.field("inStock").value(inStock)));
        }
        if (tags != null && !tags.isEmpty()) {
            for (String t : tags) {
                bool.filter(f -> f.term(tt -> tt.field("tags").value(t)));
            }
        }
        NativeQuery query = NativeQuery.builder()
                .withQuery(bool.build()._toQuery())
                .withPageable(PageRequest.of(page, size))
                .build();
        SearchHits<ProductSearchDocument> hits = operations.search(query, ProductSearchDocument.class);
        List<ProductSearchDocument> products = hits.getSearchHits().stream().map(SearchHit::getContent).toList();
        products = sortProducts(products, sort);
        Map<String, Object> facets = buildFacets(products);
        return new SearchResponse(q, hits.getTotalHits(), page, size, products, facets);
    }

    public List<String> autocomplete(String q, int limit) {
        if (q == null || q.isBlank()) {
            return List.of();
        }
        return repository.findTop10ByNameContainingIgnoreCase(q).stream()
                .map(ProductSearchDocument::getName)
                .distinct()
                .limit(Math.max(1, limit))
                .toList();
    }

    public List<ProductSearchDocument> similar(String id, int limit) {
        ProductSearchDocument src = repository.findById(id).orElse(null);
        if (src == null) return List.of();
        var bool = bool()
                .mustNot(m -> m.term(t -> t.field("id").value(id)))
                .should(s -> s.term(t -> t.field("categoryId").value(src.getCategoryId())))
                .should(s -> s.term(t -> t.field("brandId").value(src.getBrandId())));
        if (src.getSalePrice() != null) {
            double p = src.getSalePrice();
            bool.should(s -> s.range(r -> r.field("salePrice")
                    .gte(co.elastic.clients.json.JsonData.of(p * 0.8))
                    .lte(co.elastic.clients.json.JsonData.of(p * 1.2))));
        }
        NativeQuery query = NativeQuery.builder().withQuery(bool.build()._toQuery()).withPageable(PageRequest.of(0, Math.max(1, limit))).build();
        return operations.search(query, ProductSearchDocument.class).getSearchHits().stream().map(SearchHit::getContent).toList();
    }

    private List<ProductSearchDocument> sortProducts(List<ProductSearchDocument> products, String sort) {
        if (sort == null || sort.isBlank() || "relevance".equalsIgnoreCase(sort)) return products;
        List<ProductSearchDocument> copy = new ArrayList<>(products);
        if ("price_asc".equalsIgnoreCase(sort)) copy.sort(Comparator.comparing(p -> p.getSalePrice() == null ? 0d : p.getSalePrice()));
        if ("price_desc".equalsIgnoreCase(sort)) copy.sort(Comparator.comparing((ProductSearchDocument p) -> p.getSalePrice() == null ? 0d : p.getSalePrice()).reversed());
        if ("newest".equalsIgnoreCase(sort)) copy.sort(Comparator.comparing(ProductSearchDocument::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())));
        return copy;
    }

    private Map<String, Object> buildFacets(List<ProductSearchDocument> products) {
        Map<String, Long> catCounts = new HashMap<>();
        Map<String, Long> brandCounts = new HashMap<>();
        long r1 = 0, r2 = 0, r3 = 0;
        for (ProductSearchDocument p : products) {
            catCounts.merge(p.getCategoryName(), 1L, Long::sum);
            brandCounts.merge(p.getBrandName(), 1L, Long::sum);
            double price = p.getSalePrice() == null ? 0d : p.getSalePrice();
            if (price < 5_000_000) r1++;
            else if (price <= 15_000_000) r2++;
            else r3++;
        }
        return Map.of(
                "categories", catCounts.entrySet().stream().map(e -> Map.of("id", e.getKey(), "name", e.getKey(), "count", e.getValue())).toList(),
                "brands", brandCounts.entrySet().stream().map(e -> Map.of("name", e.getKey(), "count", e.getValue())).toList(),
                "priceRanges", List.of(
                        Map.of("from", 0, "to", 5000000, "count", r1),
                        Map.of("from", 5000000, "to", 15000000, "count", r2),
                        Map.of("from", 15000000, "to", 999999999, "count", r3)
                )
        );
    }
}
