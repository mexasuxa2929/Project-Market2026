package mexa.club.searchservice.repository;

import mexa.club.searchservice.document.ProductSearchDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

public interface ProductSearchRepository extends ElasticsearchRepository<ProductSearchDocument, String> {
    List<ProductSearchDocument> findTop10ByNameContainingIgnoreCase(String q);
}
