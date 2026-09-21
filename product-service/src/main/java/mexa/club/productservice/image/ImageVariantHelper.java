package mexa.club.productservice.image;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Rasm variant URL'larini hisoblash yordamchisi.
 *
 * THUMB path'dan ORIGINAL va MEDIUM URL'larni hisoblaydi.
 * Masalan:
 *   ORIGINAL: /api/files/images/products/{id}/original_xxx.jpg
 *   THUMB:    /api/files/images/products/{id}/thumb_xxx.jpg
 *   MEDIUM:   /api/files/images/products/{id}/medium_xxx.jpg
 */
public final class ImageVariantHelper {

    private ImageVariantHelper() {}

    /**
     * Bitta THUMB path'dan barcha variant URL'larni qaytaradi.
     */
    public static Map<ImageVariant, String> toVariantPaths(String thumbPath) {
        if (thumbPath == null || thumbPath.isBlank()) {
            return Map.of();
        }
        int lastSlash = thumbPath.lastIndexOf('/');
        String dir = lastSlash >= 0 ? thumbPath.substring(0, lastSlash + 1) : "";
        String filename = lastSlash >= 0 ? thumbPath.substring(lastSlash + 1) : thumbPath;

        String baseName = filename;
        for (ImageVariant variant : ImageVariant.values()) {
            String prefix = variant.prefix() + "_";
            if (filename.startsWith(prefix)) {
                baseName = filename.substring(prefix.length());
                break;
            }
        }

        return Map.of(
                ImageVariant.ORIGINAL, dir + ImageVariant.ORIGINAL.prefix() + "_" + baseName,
                ImageVariant.THUMB, dir + ImageVariant.THUMB.prefix() + "_" + baseName,
                ImageVariant.MEDIUM, dir + ImageVariant.MEDIUM.prefix() + "_" + baseName
        );
    }

    public static List<Map<ImageVariant, String>> toAllVariantPaths(List<String> thumbPaths) {
        if (thumbPaths == null || thumbPaths.isEmpty()) {
            return List.of();
        }
        return thumbPaths.stream()
                .map(ImageVariantHelper::toVariantPaths)
                .collect(Collectors.toList());
    }

    public static String toMedium(String thumbPath) {
        Map<ImageVariant, String> variants = toVariantPaths(thumbPath);
        return variants.getOrDefault(ImageVariant.MEDIUM, thumbPath);
    }

    public static String toOriginal(String thumbPath) {
        Map<ImageVariant, String> variants = toVariantPaths(thumbPath);
        return variants.getOrDefault(ImageVariant.ORIGINAL, thumbPath);
    }
}
