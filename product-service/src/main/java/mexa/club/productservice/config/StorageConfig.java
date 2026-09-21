package mexa.club.productservice.config;

import mexa.club.productservice.image.ImageUploadService;
import mexa.club.productservice.storage.LocalStorageService;
import mexa.club.productservice.storage.StorageService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class StorageConfig {

    /**
     * Hozircha faqat mahalliy (local) saqlash qo'llaniladi.
     * Kelajakda {@code app.storage.type=minio} yoki {@code s3} asosida
     * boshqa implementatsiyaga almashtirish mumkin.
     */
    @Bean
    public StorageService storageService(StorageProperties properties, ImageUploadService imageUploadService) {
        return new LocalStorageService(properties, imageUploadService);
    }
}
