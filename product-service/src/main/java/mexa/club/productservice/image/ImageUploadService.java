package mexa.club.productservice.image;

import net.coobird.thumbnailator.Thumbnails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

@Service
public class ImageUploadService {

    private static final Logger log = LoggerFactory.getLogger(ImageUploadService.class);

    private final Path uploadsRoot;

    public ImageUploadService(mexa.club.productservice.config.StorageProperties properties) {
        this.uploadsRoot = Path.of(properties.getRoot(), "images", "products");
        try {
            Files.createDirectories(uploadsRoot);
        } catch (IOException e) {
            log.warn("Could not create uploads directory: {}", e.getMessage());
        }
    }

    public Map<ImageVariant, String> uploadWithVariants(UUID productId, MultipartFile file) throws IOException {
        Map<ImageVariant, String> variantPaths = new HashMap<>();
        String baseName = UUID.randomUUID().toString();
        String originalFilename = file.getOriginalFilename();
        String ext = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase()
                : "bin";

        byte[] imageBytes = file.getBytes();

        for (ImageVariant variant : ImageVariant.values()) {
            String fileName = variant.prefix() + "_" + baseName + ".jpg";
            Path variantDir = uploadsRoot.resolve(String.valueOf(productId));
            Files.createDirectories(variantDir);
            Path outputFile = variantDir.resolve(fileName);

            if (variant.isOriginal()) {
                // ORIGINAL — faylni to'g'ridan-to'g'ri JPEG formatga o'tkazish.
                // Alpha channel (PNG transparent) bo'lsa, ImageIO.write false qaytaradi — fallback: asl faylni ko'chirish.
                try (InputStream inputStream = new ByteArrayInputStream(imageBytes)) {
                    BufferedImage originalImage = ImageIO.read(inputStream);
                    boolean written = false;
                    if (originalImage != null) {
                        written = ImageIO.write(originalImage, "jpg", outputFile.toFile());
                    }
                    if (!written) {
                        Files.copy(new ByteArrayInputStream(imageBytes), outputFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            } else {
                try (InputStream inputStream = new ByteArrayInputStream(imageBytes)) {
                    Thumbnails.of(inputStream)
                            .size(variant.width, variant.height)
                            .outputQuality(variant.quality)
                            .outputFormat("jpg")
                            .toFile(outputFile.toFile());
                }
            }

            String relativePath = "images/products/" + productId + "/" + fileName;
            variantPaths.put(variant, relativePath);
            log.debug("Created {} variant: {}x{} -> {}", variant.name(), variant.width, variant.height, relativePath);
        }

        log.info("Created {} variants for product {} (original format: {})", variantPaths.size(), productId, ext);
        return variantPaths;
    }

    public java.util.List<Map<ImageVariant, String>> uploadMultipleWithVariants(UUID productId, java.util.List<MultipartFile> files) throws IOException {
        java.util.List<Map<ImageVariant, String>> resultList = new java.util.ArrayList<>();

        for (MultipartFile file : files) {
            if (file != null && !file.isEmpty()) {
                Map<ImageVariant, String> variants = uploadWithVariants(productId, file);
                resultList.add(variants);
            }
        }

        return resultList;
    }

    public String getDefaultImagePath(Map<ImageVariant, String> variantPaths) {
        return variantPaths.getOrDefault(ImageVariant.THUMB, variantPaths.values().iterator().next());
    }
}
