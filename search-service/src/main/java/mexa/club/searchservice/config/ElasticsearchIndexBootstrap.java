package mexa.club.searchservice.config;

import mexa.club.searchservice.document.ProductSearchDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "app.search.elasticsearch.enabled", havingValue = "true", matchIfMissing = true)
public class ElasticsearchIndexBootstrap implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(ElasticsearchIndexBootstrap.class);

    private final ElasticsearchOperations operations;

    public ElasticsearchIndexBootstrap(ElasticsearchOperations operations) {
        this.operations = operations;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            IndexOperations ops = operations.indexOps(ProductSearchDocument.class);
            if (ops.exists()) {
                if (ops.getSettings().toString().contains("uzbek_analyzer")) {
                    return;
                }
                // O-61: indeksni O'CHIRISH taqiqlandi (butun qidiruv yo'qolardi).
                // Analyzer yangilanmasa — ishlayveradi (fallback), lekin aniq ogohlantiramiz.
                log.error("Elasticsearch index 'products' eski analyzer bilan mavjud. "
                        + "Indeks avvalgidek qoldirildi (delete taqiqlan); "
                        + "yangilash uchun qo'lda alias/reindex bajaring.");
                return;
            }
            Map<String, Object> settings = Map.of(
                    "analysis", Map.of(
                            "analyzer", Map.of(
                                    "uzbek_analyzer", Map.of(
                                            "type", "custom",
                                            "tokenizer", "standard",
                                            "filter", List.of("lowercase", "asciifolding")),
                                    "ngram_analyzer", Map.of(
                                            "type", "custom",
                                            "tokenizer", "standard",
                                            "filter", List.of("lowercase", "my_edge_ngram"))),
                            "filter", Map.of(
                                    "my_edge_ngram", Map.of(
                                            "type", "edge_ngram",
                                            "min_gram", 2,
                                            "max_gram", 15))));
            ops.create(settings);
            ops.putMapping();
            log.info("Elasticsearch index 'products' created (uzbek_analyzer, ngram_analyzer)");
        } catch (Exception e) {
            // O-61: xato yutilmasdan to'liq stack bilan logga tushadi
            log.error("Elasticsearch index bootstrap failed", e);
        }
    }
}
