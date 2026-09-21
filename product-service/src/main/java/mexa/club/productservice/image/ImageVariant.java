package mexa.club.productservice.image;

/**
 * Rasm variantlari — har bir mahsulot rasmi uchun 3 xil o'lcham yaratiladi.
 *
 * ORIGINAL — Asl rasm (o'lcham va sifat o'zgarmaydi)
 * THUMB    — Ro'yxat cardlari uchun (300x300 px, 75% sifat)
 * MEDIUM   — Detal sahifasidagi slayder uchun (800x800 px, 85% sifat)
 */
public enum ImageVariant {
    ORIGINAL(-1, -1, 1.0),
    THUMB(300, 300, 0.75),
    MEDIUM(800, 800, 0.85);

    public final int width;
    public final int height;
    public final double quality;

    ImageVariant(int width, int height, double quality) {
        this.width = width;
        this.height = height;
        this.quality = quality;
    }

    public boolean isOriginal() {
        return this == ORIGINAL;
    }

    /**
     * Fayl nomi uchun variant prefiksi (masalan: "thumb_xxx.webp")
     */
    public String prefix() {
        return name().toLowerCase();
    }
}
