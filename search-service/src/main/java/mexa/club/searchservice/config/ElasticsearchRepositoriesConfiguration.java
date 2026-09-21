package mexa.club.searchservice.config;

import mexa.club.searchservice.repository.ProductSearchRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

@Configuration
@ConditionalOnProperty(name = "app.search.elasticsearch.enabled", havingValue = "true", matchIfMissing = true)
@EnableElasticsearchRepositories(basePackageClasses = ProductSearchRepository.class)
public class ElasticsearchRepositoriesConfiguration {
}
