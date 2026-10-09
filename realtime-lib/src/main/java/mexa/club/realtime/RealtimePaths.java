package mexa.club.realtime;

/**
 * Realtime event payload'lari uchun path normalizatsiyasi.
 * ID segmentlaridan qutilish uchun: {@code /api/products/<uuid>/images}
 * → {@code /api/products/{id}/images}.
 * Shu orqali client'lar barqaror {@code resource} prefiksi bo'yicha filtrlaydi.
 */
public final class RealtimePaths {

    private static final String ID_PLACEHOLDER = "{id}";
    private static final java.util.regex.Pattern UUID_PATTERN = java.util.regex.Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
    private static final java.util.regex.Pattern HEX_ID_PATTERN = java.util.regex.Pattern.compile(
            "^[0-9a-fA-F]{16,}$");

    private RealtimePaths() {
    }

    /** Path'dagi UUID / uzoq hex / raqamli segmentlarni {@code {id}} bilan almashtiradi. */
    public static String normalize(String path) {
        if (path == null || path.isEmpty()) {
            return path;
        }
        String[] segments = path.split("/", -1);
        for (int i = 0; i < segments.length; i++) {
            if (isIdSegment(segments[i])) {
                segments[i] = ID_PLACEHOLDER;
            }
        }
        return String.join("/", segments);
    }

    private static boolean isIdSegment(String segment) {
        if (segment.isEmpty()) {
            return false;
        }
        if (UUID_PATTERN.matcher(segment).matches()) {
            return true;
        }
        if (HEX_ID_PATTERN.matcher(segment).matches()) {
            return true;
        }
        // Faqat raqamlardan iborat segment (snowflake id va hokazo)
        for (int i = 0; i < segment.length(); i++) {
            if (!Character.isDigit(segment.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}
