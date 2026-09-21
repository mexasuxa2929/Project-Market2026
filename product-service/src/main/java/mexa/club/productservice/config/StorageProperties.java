package mexa.club.productservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Set;

/**
 * Fayl saqlash — ildiz {@code images/products/...} kabi kengaytiriladigan struktura.
 */
@ConfigurationProperties(prefix = "app.storage")
public class StorageProperties {

    private String type = "local";

    /**
     * Loyiha ildizidan nisbiy yoki mutlaq yo‘l. Masalan: {@code ./storage}.
     */
    private String root = "storage";

    /**
     * API orqali fayllarga URL prefiksi (klientlar rasm URLini quradi).
     */
    private String publicUriPrefix = "/api/files";

    private long maxImageBytes = 5 * 1024 * 1024;

    private Set<String> allowedImageContentTypes = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif");

    private Minio minio = new Minio();

    private S3 s3 = new S3();

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getRoot() {
        return root;
    }

    public void setRoot(String root) {
        this.root = root;
    }

    public String getPublicUriPrefix() {
        return publicUriPrefix;
    }

    public void setPublicUriPrefix(String publicUriPrefix) {
        this.publicUriPrefix = publicUriPrefix;
    }

    public long getMaxImageBytes() {
        return maxImageBytes;
    }

    public void setMaxImageBytes(long maxImageBytes) {
        this.maxImageBytes = maxImageBytes;
    }

    public Set<String> getAllowedImageContentTypes() {
        return allowedImageContentTypes;
    }

    public void setAllowedImageContentTypes(Set<String> allowedImageContentTypes) {
        this.allowedImageContentTypes = allowedImageContentTypes;
    }

    public Minio getMinio() {
        return minio;
    }

    public void setMinio(Minio minio) {
        this.minio = minio;
    }

    public S3 getS3() {
        return s3;
    }

    public void setS3(S3 s3) {
        this.s3 = s3;
    }

    public static class Minio {
        private String endpoint;
        private String accessKey;
        private String secretKey;
        private String bucket;
        private String publicBaseUrl;

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getAccessKey() {
            return accessKey;
        }

        public void setAccessKey(String accessKey) {
            this.accessKey = accessKey;
        }

        public String getSecretKey() {
            return secretKey;
        }

        public void setSecretKey(String secretKey) {
            this.secretKey = secretKey;
        }

        public String getBucket() {
            return bucket;
        }

        public void setBucket(String bucket) {
            this.bucket = bucket;
        }

        public String getPublicBaseUrl() {
            return publicBaseUrl;
        }

        public void setPublicBaseUrl(String publicBaseUrl) {
            this.publicBaseUrl = publicBaseUrl;
        }
    }

    public static class S3 {
        private String endpoint;
        private String accessKey;
        private String secretKey;
        private String bucket;
        private String publicBaseUrl;

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getAccessKey() {
            return accessKey;
        }

        public void setAccessKey(String accessKey) {
            this.accessKey = accessKey;
        }

        public String getSecretKey() {
            return secretKey;
        }

        public void setSecretKey(String secretKey) {
            this.secretKey = secretKey;
        }

        public String getBucket() {
            return bucket;
        }

        public void setBucket(String bucket) {
            this.bucket = bucket;
        }

        public String getPublicBaseUrl() {
            return publicBaseUrl;
        }

        public void setPublicBaseUrl(String publicBaseUrl) {
            this.publicBaseUrl = publicBaseUrl;
        }
    }
}

