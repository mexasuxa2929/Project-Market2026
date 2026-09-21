package mexa.club.reportservice.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Configuration
public class CacheConfig {
    @Bean
    CacheManager cacheManager() {
        SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(List.of(
                new CaffeineCache("dashboard", Caffeine.newBuilder().maximumSize(100).expireAfterWrite(5, TimeUnit.MINUTES).build()),
                new CaffeineCache("salesReports", Caffeine.newBuilder().maximumSize(300).expireAfterWrite(15, TimeUnit.MINUTES).build()),
                new CaffeineCache("productReports", Caffeine.newBuilder().maximumSize(300).expireAfterWrite(15, TimeUnit.MINUTES).build()),
                new CaffeineCache("customerReports", Caffeine.newBuilder().maximumSize(300).expireAfterWrite(15, TimeUnit.MINUTES).build())
        ));
        return manager;
    }
}
