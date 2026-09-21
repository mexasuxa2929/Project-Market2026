package mexa.club.productservice.exception;

/**
 * Mahsulotni o'chirib bo'lmaydigan holatlar uchun (409 CONFLICT).
 * Masalan: mahsulot "featured" belgilangan, orderlarga kiritilgan,
 * wishlist/lapda mavjud yoki omborda faol qoldiq bor.
 */
public class ProductDeletionException extends RuntimeException {

    private final String reasonCode;

    public ProductDeletionException(String reasonCode, String message) {
        super(message);
        this.reasonCode = reasonCode;
    }

    public String getReasonCode() {
        return reasonCode;
    }
}
