package mexa.club.productservice.config;

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
                new CaffeineCache("brands", Caffeine.newBuilder().maximumSize(100).expireAfterWrite(30, TimeUnit.MINUTES).build()),
                new CaffeineCache("categories", Caffeine.newBuilder().maximumSize(100).expireAfterWrite(30, TimeUnit.MINUTES).build()),
                // products: detail ichida jonli stock ham bor — 10 daqiqa stale qoldiq
                // oversell xavfi tug'dirardi (buyurtmadan keyin ham eski qoldiq ko'rinadi).
                // 2 daqiqa: kesh samarasi saqlanadi, noaniqlik oynasi 5 baravar kichik.
                new CaffeineCache("products", Caffeine.newBuilder().maximumSize(500).expireAfterWrite(2, TimeUnit.MINUTES).build())
        ));
        return manager;
    }
}
