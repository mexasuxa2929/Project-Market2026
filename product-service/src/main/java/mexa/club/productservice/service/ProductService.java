package mexa.club.productservice.service;

import mexa.club.productservice.client.OrderUsageClient;
import mexa.club.productservice.client.ShopReviewClient;
import mexa.club.productservice.client.WarehouseVariantStockClient;
import mexa.club.productservice.dto.ProductColorSiblingResponse;
import mexa.club.productservice.dto.ProductFullResponse;
import mexa.club.productservice.dto.ProductRequest;
import mexa.club.productservice.dto.ProductResponse;
import mexa.club.productservice.dto.ProductImportResult;
import mexa.club.productservice.dto.ProductPriceRequest;
import mexa.club.productservice.dto.ProductPriceResponse;
import mexa.club.productservice.dto.PriceResolveResponse;
import mexa.club.productservice.dto.StockSummaryResponse;
import mexa.club.productservice.entity.Product;
import mexa.club.productservice.entity.ProductPrice;
import mexa.club.productservice.entity.ProductPriceTier;
import mexa.club.productservice.entity.ProductTag;
import mexa.club.productservice.exception.ProductDeletionException;
import mexa.club.productservice.exception.ProductNotFoundException;
import mexa.club.productservice.exception.ResourceNotFoundException;
import mexa.club.productservice.client.WarehouseStockClient;
import mexa.club.productservice.repository.ProductPriceRepository;
import mexa.club.productservice.repository.ProductPriceTierRepository;
import mexa.club.productservice.repository.ProductRepository;
import mexa.club.productservice.repository.ProductTagRepository;
import mexa.club.productservice.repository.ProductSpecification;
import mexa.club.productservice.storage.StorageService;
import mexa.club.productservice.realtime.RealtimeEventPublisher;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository productRepository;
    private final ProductPriceRepository productPriceRepository;
    private final ProductPriceTierRepository productPriceTierRepository;
    private final ProductTagRepository productTagRepository;
    private final StorageService storageService;
    private final ReferenceDataService referenceDataService;
    private final WarehouseStockClient warehouseStockClient;
    private final WarehouseVariantStockClient warehouseProductStockClient;
    private final OrderUsageClient orderUsageClient;
    private final ShopReviewClient shopReviewClient;
    private final RealtimeEventPublisher realtimeEventPublisher;

    public ProductService(
            ProductRepository productRepository,
            ProductPriceRepository productPriceRepository,
            ProductPriceTierRepository productPriceTierRepository,
            ProductTagRepository productTagRepository,
            StorageService storageService,
            ReferenceDataService referenceDataService,
            WarehouseStockClient warehouseStockClient,
            WarehouseVariantStockClient warehouseProductStockClient,
            OrderUsageClient orderUsageClient,
            ShopReviewClient shopReviewClient,
            RealtimeEventPublisher realtimeEventPublisher
    ) {
        this.productRepository = productRepository;
        this.productPriceRepository = productPriceRepository;
        this.productPriceTierRepository = productPriceTierRepository;
        this.productTagRepository = productTagRepository;
        this.storageService = storageService;
        this.referenceDataService = referenceDataService;
        this.warehouseStockClient = warehouseStockClient;
        this.warehouseProductStockClient = warehouseProductStockClient;
        this.orderUsageClient = orderUsageClient;
        this.shopReviewClient = shopReviewClient;
        this.realtimeEventPublisher = realtimeEventPublisher;
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> findProducts(
            Pageable pageable,
            UUID afterId,
            String name,
            String barcode,
            String categoryIdParam,
            String brandIdParam,
            String manufacturerIdParam,
            Boolean active,
            String tag,
            String status,
            Boolean featured,
            String lang
    ) {
        UUID categoryId = parseOptionalUuid(categoryIdParam);
        UUID brandId = parseOptionalUuid(brandIdParam);
        UUID manufacturerId = parseOptionalUuid(manufacturerIdParam);
        // Keyset: cursor berilganda tartib majburan id (ASC) bo'ladi — OFFSET ishlatilmaydi
        if (afterId != null) {
            pageable = PageRequest.of(0, pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "id"));
        }
        Page<Product> page = productRepository.findAll(
                ProductSpecification.combined(name, barcode, categoryId, brandId, manufacturerId, active, tag, status, featured)
                        .and(ProductSpecification.afterId(afterId))
                        .and(ProductSpecification.primaryOnly()),
                pageable
        );
        List<Product> content = page.getContent();
        // N+1 oldini olish: sibling colors, category/brand/manufacturer nomlari — hammasi batch
        Map<UUID, List<ProductColorSiblingResponse>> siblingMap = buildSiblingColorsBatch(content);
        Map<UUID, String> categoryNameMap = referenceDataService.resolveCategoryNames(
                content.stream().map(Product::getCategoryId).filter(Objects::nonNull).collect(Collectors.toSet()));
        Map<UUID, String> brandNameMap = referenceDataService.resolveBrandNames(
                content.stream().map(Product::getBrandId).filter(Objects::nonNull).collect(Collectors.toSet()));
        Map<UUID, String> manufacturerNameMap = referenceDataService.resolveManufacturerNames(
                content.stream().map(Product::getManufacturerId).filter(Objects::nonNull).collect(Collectors.toSet()));
        List<ProductResponse> items = enrichWithPricing(content.stream()
                .map(p -> toResponse(p, siblingMap, categoryNameMap, brandNameMap, manufacturerNameMap))
                .toList());
        if (lang != null && !lang.isBlank()) {
            // Mobile uchun: lokalizatsiya + yengil payload (translations/descriptions olib tashlanadi)
            items = items.stream().map(r -> slimLocalized(r, lang)).toList();
        }
        return new PageImpl<>(items, pageable, page.getTotalElements());
    }

    /** Ro'yxat/detail javoblarini narx, stock va reyting bilan boyitadi (shop/mobile uchun). */
    private List<ProductResponse> enrichWithPricing(List<ProductResponse> items) {
        if (items == null || items.isEmpty()) {
            return items;
        }
        List<UUID> ids = items.stream().map(ProductResponse::getId).toList();
        Map<UUID, Integer> stockMap = warehouseProductStockClient.fetchTotalStockBatch(ids);
        Map<UUID, double[]> ratingMap = shopReviewClient.fetchRatingBatch(ids);
        // BATCH narx: N+1 o'rniga bitta query — har mahsulot uchun eng so'nggi (effectiveDate DESC) narx.
        // Chegirma o'qish vaqtida hisoblanadi: sanalar oralig'ida bo'lsa faol, o'tgan bo'lsa avtomatik o'chadi.
        Map<UUID, ProductPrice> priceMap = new LinkedHashMap<>();
        for (ProductPrice pp : productPriceRepository.findByProductIdInOrderByEffectiveDateDesc(ids)) {
            priceMap.putIfAbsent(pp.getProductId(), pp);
        }
        // Liniyalar (miqdorga bog'liq narxlar). Chegirma faol bo'lsa liniyalar ishlamaydi —
        // qancha dona bo'lsa ham 1-liniya narxiga chegirma qo'llanadi.
        Map<UUID, List<ProductPriceTier>> tierMap = new LinkedHashMap<>();
        for (ProductPriceTier t : productPriceTierRepository.findByProductIdInOrderByMinQtyAsc(ids)) {
            tierMap.computeIfAbsent(t.getProductId(), k -> new ArrayList<>()).add(t);
        }
        LocalDateTime now = LocalDateTime.now();
        for (ProductResponse item : items) {
            ProductPrice pp = priceMap.get(item.getId());
            List<ProductPriceTier> tiers = tierMap.get(item.getId());
            int pct = activeDiscountPercent(pp, now);
            if (pct > 0) {
                // Chegirma faol — liniyalar o'chadi: istalgan miqdor 1-liniya narxida chegirma bilan.
                BigDecimal ref = (tiers != null && !tiers.isEmpty())
                        ? tiers.get(0).getPrice()
                        : (pp != null ? pp.getSalePrice() : null);
                item.setBasePrice(discounted(ref, pp, now));
                item.setDiscountPercent(pct);
            } else {
                item.setDiscountPercent(0);
                if (tiers != null && !tiers.isEmpty()) {
                    // Chegirma yo'q — boshlang'ich narx 1-liniya narxi.
                    item.setBasePrice(tiers.get(0).getPrice());
                } else {
                    item.setBasePrice(currentPrice(pp, now));
                }
            }
            Integer totalStock = stockMap.getOrDefault(item.getId(), null);
            item.setTotalStock(totalStock);
            item.setInStock(totalStock != null && totalStock > 0);
            double[] rating = ratingMap.get(item.getId());
            if (rating != null) {
                item.setAvgRating(rating[0]);
                item.setReviewCount((long) rating[1]);
            } else {
                item.setAvgRating(0.0);
                item.setReviewCount(0L);
            }
        }
        return items;
    }

    /** Joriy narx: chegirma faol bo'lsa hisoblangan, aks holda asl (salePrice). */
    private static BigDecimal currentPrice(ProductPrice pp, LocalDateTime now) {
        if (pp == null) {
            return null;
        }
        return discounted(pp.getSalePrice(), pp, now);
    }

    /** Asosiy narx ustidan chegirma: faol bo'lsa hisoblangan, aks holda asl narx. */
    private static BigDecimal discounted(BigDecimal base, ProductPrice pp, LocalDateTime now) {
        if (base == null) {
            return BigDecimal.ZERO;
        }
        if (pp == null || !ProductPriceResponse.isDiscountActive(pp, now)) {
            return base;
        }
        return base.multiply(BigDecimal.valueOf(100 - pp.getDiscountPercent()))
                .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
    }

    /** Aktiv chegirma foizi — muddat o'tgan bo'lsa 0 qaytaradi (narx avtomatik eski holatga qaytadi). */
    private static int activeDiscountPercent(ProductPrice pp, LocalDateTime now) {
        if (pp == null || !ProductPriceResponse.isDiscountActive(pp, now)) {
            return 0;
        }
        return pp.getDiscountPercent();
    }

    /**
     * Yagona narx resolve qilish (shop/order cart va buyurtma liniyalari uchun).
     * Qoidalar:
     *  - Chegirma faol bo'lsa: liniyalar ishlamaydi — qancha dona bo'lsa ham 1-liniya narxiga chegirma qo'llanadi;
     *  - Chegirma faol bo'lmasa: miqdor bo'yicha liniya narxi (2+ liniya) yoki asosiy narx (0–1 liniya).
     */
    @Transactional(readOnly = true)
    public PriceResolveResponse resolvePrice(UUID productId, int qty) {
        requireProduct(productId);
        int safeQty = Math.max(1, qty);
        List<ProductPriceTier> tiers = productPriceTierRepository.findByProductIdOrderByMinQtyAsc(productId);
        ProductPrice pp = productPriceRepository.findByProductIdOrderByEffectiveDateDesc(productId)
                .stream().findFirst().orElse(null);
        LocalDateTime now = LocalDateTime.now();
        int pct = activeDiscountPercent(pp, now);
        if (pct > 0) {
            // Chegirma faol — liniyalar ishlamaydi: istalgan miqdor 1-liniya narxida chegirma bilan hisoblanadi.
            BigDecimal ref = tiers.isEmpty()
                    ? (pp != null ? pp.getSalePrice() : null)
                    : tiers.get(0).getPrice();
            return new PriceResolveResponse(
                    productId, safeQty, discounted(ref, pp, now), pct, false, tiers.size());
        }
        if (tiers.size() >= 2) {
            ProductPriceTier match = null;
            for (ProductPriceTier t : tiers) { // minQty ASC — oxirgi mos kelgani eng "katta" liniya
                if (t.getMinQty() <= safeQty && (t.getMaxQty() == null || t.getMaxQty() >= safeQty)) {
                    match = t;
                }
            }
            BigDecimal price = match != null ? match.getPrice() : tiers.get(0).getPrice();
            return new PriceResolveResponse(productId, safeQty, price, 0, match != null, tiers.size());
        }
        BigDecimal base = tiers.isEmpty()
                ? (pp != null ? pp.getSalePrice() : null)
                : tiers.get(0).getPrice();
        return new PriceResolveResponse(
                productId, safeQty, discounted(base, pp, now), 0, false, tiers.size());
    }

    private ProductResponse enrichWithPricing(ProductResponse item) {
        if (item == null) {
            return null;
        }
        List<ProductResponse> enriched = enrichWithPricing(List.of(item));
        return enriched.isEmpty() ? item : enriched.get(0);
    }

    @Cacheable("products")
    @Transactional(readOnly = true)
    public ProductResponse getProductById(UUID id, String lang) {
        ProductResponse response = enrichWithPricing(toResponse(requireProduct(id)));
        return localizedDetail(response, lang);
    }

    @CacheEvict(cacheNames = "products", allEntries = true)
    @Transactional
    public ProductResponse createProduct(ProductRequest dto) {
        return createProduct(dto, List.of());
    }

    @CacheEvict(cacheNames = "products", allEntries = true)
    @Transactional
    public ProductResponse createProduct(ProductRequest dto, List<MultipartFile> images) {
        requireUniqueBarcode(dto.getBarcode(), null);
        Product product = new Product();
        product.setImagePaths(new ArrayList<>());
        product.setTags(new LinkedHashSet<>());
        applyRequest(dto, product, true);
        applyTags(product, dto.getTags());
        // Guruhga qo'shilayotgan rang nusxasi (dto.groupId to'ldirilgan) vakil emas — u
        // ro'yxatda alohida qator bo'lib ko'rinmaydi. Guruhsiz (standalone) mahsulot vakil bo'ladi.
        product.setPrimaryVariant(dto.getGroupId() == null);
        Product saved = productRepository.save(product);
        appendUploadedImages(saved, images);
        if (images != null && !images.isEmpty()) {
            saved = productRepository.save(saved);
        }
        log.info("Product created id={} barcode={} color={} groupId={}", saved.getId(), saved.getBarcode(), saved.getColor(), saved.getGroupId());
        realtimeEventPublisher.productCreated(saved.getId());
        return toResponse(saved);
    }

    /**
     * Mavjud mahsulotning yangi rangini (nusxasini) yaratadi: bir xil guruhga ({@code groupId})
     * bog'langan, lekin o'z barcode/narx/sklad qoldig'iga ega bo'lgan mustaqil Product.
     * Agar manba mahsulotda hali groupId bo'lmasa, avval o'ziga groupId beriladi.
     */
    @CacheEvict(cacheNames = "products", allEntries = true)
    @Transactional
    public ProductResponse createColorSibling(UUID sourceProductId, ProductRequest dto) {
        Product source = requireProduct(sourceProductId);
        if (source.getGroupId() == null) {
            source.setGroupId(UUID.randomUUID());
            productRepository.save(source);
        }
        dto.setGroupId(source.getGroupId());
        return createProduct(dto, List.of());
    }

    @CacheEvict(cacheNames = "products", allEntries = true)
    @Transactional
    public ProductResponse updateProduct(UUID id, ProductRequest dto) {
        return updateProduct(id, dto, List.of());
    }

    @CacheEvict(cacheNames = "products", allEntries = true)
    @Transactional
    public ProductResponse updateProduct(UUID id, ProductRequest dto, List<MultipartFile> newImages) {
        Product product = requireProduct(id);
        requireUniqueBarcode(dto.getBarcode(), id);
        applyRequest(dto, product, false);
        applyTags(product, dto.getTags());
        appendUploadedImages(product, newImages);
        Product saved = productRepository.save(product);
        log.info("Product updated id={}", id);
        realtimeEventPublisher.productUpdated(id);
        return toResponse(saved);
    }

    private void requireUniqueBarcode(String barcode, UUID excludingId) {
        if (barcode == null || barcode.isBlank()) {
            return;
        }
        productRepository.findByBarcode(barcode.trim()).ifPresent(existing -> {
            if (excludingId == null || !existing.getId().equals(excludingId)) {
                throw new IllegalArgumentException("Barcode allaqachon mavjud: " + barcode);
            }
        });
    }

    @CacheEvict(cacheNames = "products", allEntries = true)
    @Transactional
    public ProductResponse addProductImages(UUID id, List<MultipartFile> images) {
        Product product = requireProduct(id);
        appendUploadedImages(product, images);
        return toResponse(productRepository.save(product));
    }

    @Transactional(readOnly = true)
    public List<String> listProductImages(UUID id) {
        Product product = requireProduct(id);
        List<String> paths = resolveImagePaths(product);
        return paths.stream()
                .filter(p -> p != null && p.contains("/thumb_"))
                .map(storageService::toPublicUrl)
                .filter(u -> u != null && !u.isBlank())
                .toList();
    }

    @CacheEvict(cacheNames = "products", allEntries = true)
    @Transactional
    public ProductResponse removeProductImage(UUID productId, String filename) {
        Product product = requireProduct(productId);
        String relative = product.getImagePaths().stream()
                .filter(p -> p != null && p.endsWith("/" + filename))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("ProductImage", productId + "/" + filename));

        // Asl rasm nomini topish (thumb_, medium_, large_ prefikslarini olib tashlash)
        String baseFilename = filename;
        for (mexa.club.productservice.image.ImageVariant v : mexa.club.productservice.image.ImageVariant.values()) {
            String prefix = v.prefix() + "_";
            if (filename.startsWith(prefix)) {
                baseFilename = filename.substring(prefix.length());
                break;
            }
        }

        // Barcha variantlarni topish va o'chirish
        String finalBaseFilename = baseFilename;
        List<String> toRemove = product.getImagePaths().stream()
                .filter(p -> p != null && p.contains(productId + "/") && p.contains(finalBaseFilename))
                .toList();

        for (String path : toRemove) {
            try {
                storageService.deleteRelativePath(path);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to delete image file", e);
            }
            product.getImagePaths().remove(path);
        }

        return toResponse(productRepository.save(product));
    }

    /**
     * Rasmlar tartibini o'zgartirish.
     * {@code orderedPublicUrls} — mijoz tomonidagi yangi tartibda public URL lar ro'yxati.
     * Faqat product ga tegishli rasmlar qabul qilinadi.
     */
    @CacheEvict(cacheNames = "products", allEntries = true)
    @Transactional
    public ProductResponse reorderProductImages(UUID productId, List<String> orderedPublicUrls) {
        Product product = requireProduct(productId);
        List<String> currentPaths = product.getImagePaths();
        if (currentPaths == null || currentPaths.isEmpty()) return toResponse(product);

        // public URL → relative path xaritasi
        java.util.Map<String, String> urlToPath = new java.util.LinkedHashMap<>();
        for (String path : currentPaths) {
            String pub = storageService.toPublicUrl(path);
            if (pub != null) urlToPath.put(pub, path);
        }

        List<String> reordered = orderedPublicUrls.stream()
                .map(urlToPath::get)
                .filter(p -> p != null)
                .collect(java.util.stream.Collectors.toList());

        // Tartiblanmagan qolgan rasmlarni oxiriga qo'shamiz
        for (String path : currentPaths) {
            if (!reordered.contains(path)) reordered.add(path);
        }

        currentPaths.clear();
        currentPaths.addAll(reordered);
        return toResponse(productRepository.save(product));
    }

    /**
     * {@code active == null} bo'lsa — toggle ({@code !product.isActive()}).
     */
    @CacheEvict(cacheNames = "products", allEntries = true)
    @Transactional
    public ProductResponse setProductActive(UUID id, Boolean active) {
        Product product = requireProduct(id);
        boolean next = active != null ? active : !product.isActive();
        product.setActive(next);
        Product saved = productRepository.save(product);
        log.info("Product active flag changed id={} active={}", id, next);
        return toResponse(saved);
    }

    @CacheEvict(cacheNames = "products", allEntries = true)
    @Transactional
    public void deleteProduct(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", String.valueOf(id)));

        // 1. "Featured" mahsulotni to'g'ridan-to'g'ri o'chirish taqiqlangan — avval belgini olib tashlang.
        if (product.isFeatured()) {
            throw new ProductDeletionException("PRODUCT_FEATURED",
                    "Mahsulot 'featured' belgilangan. O'chirishdan oldin featured holatini olib tashlang.");
        }

        // 2. Orderlarga bog'liq mahsulotni o'chirib bo'lmaydi (tarixiy ma'lumotlar saqlanishi kerak).
        long orderCount = orderUsageClient.countOrdersByProduct(id);
        if (orderCount > 0) {
            throw new ProductDeletionException("PRODUCT_IN_ORDERS",
                    "Mahsulot " + orderCount + " ta orderga bog'liq. O'chirib bo'lmaydi — o'rniga 'active=false' qilib o'chiring.");
        }

        // 3. Omborda faol qoldiq borligini tekshirish (qoldiq bo'lsa ham, sklad yozuvlarini
        //    tozalashga ruxsat beramiz, lekin foydalanuvchini ogohlantiramiz).
        try {
            int totalStock = warehouseProductStockClient.fetchTotalStock(id);
            if (totalStock > 0) {
                log.warn("Deleting product {} which still has {} units in stock", id, totalStock);
            }
        } catch (Exception e) {
            log.warn("Could not verify stock for product {} before deletion: {}", id, e.getMessage());
        }

        // Agar o'chirilayotgan yozuv rang guruhining vakili bo'lsa, guruh ro'yxatdan g'oyib
        // bo'lmasligi uchun guruhdagi boshqa bir siblingni yangi vakil qilib belgilaymiz.
        if (product.isPrimaryVariant() && product.getGroupId() != null) {
            productRepository.findByGroupIdOrderByColorAsc(product.getGroupId()).stream()
                    .filter(sibling -> !sibling.getId().equals(id))
                    .findFirst()
                    .ifPresent(sibling -> {
                        sibling.setPrimaryVariant(true);
                        productRepository.save(sibling);
                        log.info("Promoted sibling id={} to primary variant for group {}", sibling.getId(), product.getGroupId());
                    });
        }
        try {
            storageService.deleteAllForProduct(id);
        } catch (IOException e) {
            log.warn("Failed to delete image folder for product {}", id, e);
        }
        productPriceRepository.deleteByProductId(id);
        warehouseStockClient.deleteStockLinesByProductId(id);
        shopReviewClient.deleteReviewsByProductId(id);
        productRepository.deleteById(id);
        log.info("Product deleted id={}", id);

        // Real-time event: mahsulot o'chirildi
        realtimeEventPublisher.productDeleted(id);
    }

    @Transactional(readOnly = true)
    public ProductResponse getByBarcode(String barcode) {
        Product product = productRepository.findByBarcode(barcode)
                .orElseThrow(() -> new ProductNotFoundException("Product not found by barcode: " + barcode));
        return enrichWithPricing(toResponse(product));
    }

    @Transactional(readOnly = true)
    public List<String> listTags() {
        return productTagRepository.findAllByOrderByNameAsc().stream()
                .map(ProductTag::getName)
                .toList();
    }

    /** Bir guruhdagi (group_id) barcha ranglarni qaytaradi (shu jumladan mavjudligi tekshirilgan productId ham). */
    @Transactional(readOnly = true)
    public List<ProductResponse> listColorGroup(UUID productId) {
        Product product = requireProduct(productId);
        if (product.getGroupId() == null) {
            return enrichWithPricing(List.of(toResponse(product)));
        }
        return enrichWithPricing(productRepository.findByGroupIdOrderByColorAsc(product.getGroupId()).stream()
                .map(this::toResponse)
                .toList());
    }

    @Transactional(readOnly = true)
    public ProductFullResponse getProductFull(UUID productId) {
        Product product = requireProduct(productId);

        List<ProductPriceTier> tiers = productPriceTierRepository.findByProductIdOrderByMinQtyAsc(productId);
        ProductPrice latestPrice = productPriceRepository
                .findByProductIdOrderByEffectiveDateDesc(productId)
                .stream().findFirst().orElse(null);
        LocalDateTime now = LocalDateTime.now();
        int pct = activeDiscountPercent(latestPrice, now);
        BigDecimal basePrice;
        if (pct > 0) {
            // Chegirma faol — liniyalar ishlamaydi: 1-liniya narxiga chegirma qo'llanadi.
            BigDecimal ref = tiers.isEmpty()
                    ? (latestPrice != null ? latestPrice.getSalePrice() : null)
                    : tiers.get(0).getPrice();
            basePrice = discounted(ref, latestPrice, now);
        } else if (!tiers.isEmpty()) {
            // Chegirma yo'q — boshlang'ich narx 1-liniya narxi.
            basePrice = tiers.get(0).getPrice();
        } else {
            basePrice = currentPrice(latestPrice, now);
        }

        int totalStock = warehouseProductStockClient.fetchTotalStock(productId);

        List<ProductColorSiblingResponse> siblingColors = buildSiblingColors(product);

        List<String> imageUrls = resolveImagePaths(product).stream()
                .map(storageService::toPublicUrl)
                .filter(u -> u != null && !u.isBlank())
                .toList();

        // Guruhlangan rasm variantlari (original/thumb/medium)
        List<mexa.club.productservice.dto.ProductImageVariants> images = buildImageVariants(product);

        String categoryName = product.getCategoryId() == null ? null : referenceDataService.resolveCategoryName(product.getCategoryId());
        String brandName = product.getBrandId() == null ? null : referenceDataService.resolveBrandName(product.getBrandId());
        String manufacturerName = product.getManufacturerId() != null
                ? referenceDataService.resolveManufacturerName(product.getManufacturerId())
                : product.getManufacturerName();
        Set<String> tags = product.getTags() == null ? Set.of() : product.getTags().stream()
                .map(mexa.club.productservice.entity.ProductTag::getName)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        return ProductFullResponse.fromEntity(
                product, basePrice, latestPrice, tiers.size(), totalStock, imageUrls, images, categoryName, brandName, manufacturerName,
                tags, siblingColors
        );
    }

    /**
     * imagePaths list'dan guruhlangan rasm variantlarini yaratadi.
     * original_xxx.jpg, thumb_xxx.jpg, medium_xxx.jpg → bitta ProductImageVariants
     */
    private List<mexa.club.productservice.dto.ProductImageVariants> buildImageVariants(Product product) {
        List<String> paths = resolveImagePaths(product);
        if (paths.isEmpty()) return List.of();

        // original_xxx.jpg dan "xxx.jpg" (baseName) topish
        Map<String, String> baseNameToOriginal = new LinkedHashMap<>();
        Map<String, String> baseNameToThumb = new LinkedHashMap<>();
        Map<String, String> baseNameToMedium = new LinkedHashMap<>();

        for (String path : paths) {
            if (path == null || path.isBlank()) continue;
            String url = storageService.toPublicUrl(path);
            if (url == null || url.isBlank()) continue;

            int lastSlash = path.lastIndexOf('/');
            String filename = lastSlash >= 0 ? path.substring(lastSlash + 1) : path;

            String baseName = filename;
            String prefix = null;
            for (mexa.club.productservice.image.ImageVariant v : mexa.club.productservice.image.ImageVariant.values()) {
                String p = v.prefix() + "_";
                if (filename.startsWith(p)) {
                    prefix = v.prefix();
                    baseName = filename.substring(p.length());
                    break;
                }
            }

            if ("original".equals(prefix)) baseNameToOriginal.put(baseName, url);
            else if ("thumb".equals(prefix)) baseNameToThumb.put(baseName, url);
            else if ("medium".equals(prefix)) baseNameToMedium.put(baseName, url);
        }

        Set<String> allBaseNames = new LinkedHashSet<>();
        allBaseNames.addAll(baseNameToOriginal.keySet());
        allBaseNames.addAll(baseNameToThumb.keySet());
        allBaseNames.addAll(baseNameToMedium.keySet());

        List<mexa.club.productservice.dto.ProductImageVariants> result = new ArrayList<>();
        for (String bn : allBaseNames) {
            result.add(mexa.club.productservice.dto.ProductImageVariants.builder()
                    .original(baseNameToOriginal.get(bn))
                    .thumb(baseNameToThumb.get(bn))
                    .medium(baseNameToMedium.get(bn))
                    .build());
        }
        return result;
    }

    /**
     * Mahsulotning rasm yo'llari. O'zida rasm bo'lmasa va rang guruhiga kirsa —
     * guruh ichidan rasmi bor variantniki qaytadi (avval primary, keyin istalgan).
     * Variantlar bitta umumiy rasmni bo'lishganda detail/cart/order'da hamma joyda
     * bir xil rasm chiqadi (savatda 📦 ko'rinmaydi).
     */
    private List<String> resolveImagePaths(Product p) {
        if (p.getImagePaths() != null && !p.getImagePaths().isEmpty()) {
            return p.getImagePaths();
        }
        if (p.getGroupId() == null) {
            return List.of();
        }
        return productRepository.findByGroupIdOrderByColorAsc(p.getGroupId()).stream()
                .filter(s -> !s.getId().equals(p.getId()))
                .sorted(Comparator.comparing(s -> !s.isPrimaryVariant()))
                .map(Product::getImagePaths)
                .filter(l -> l != null && !l.isEmpty())
                .findFirst()
                .orElse(List.of());
    }

    /** Bitta asosiy rasm URL (ro'yxat/sibling ko'rinishlar uchun, guruh fallback bilan). */
    private String resolveFirstImageUrl(Product p) {
        List<String> paths = resolveImagePaths(p);
        return paths.isEmpty() ? null : storageService.toPublicUrl(paths.get(0));
    }

    /** Shu mahsulot bilan bir guruhdagi boshqa ranglarni qisqa ko'rinishda qaytaradi (o'zini hisobga olmasdan). */
    private List<ProductColorSiblingResponse> buildSiblingColors(Product product) {
        if (product.getGroupId() == null) {
            return List.of();
        }
        return productRepository.findByGroupIdOrderByColorAsc(product.getGroupId()).stream()
                .filter(p -> !p.getId().equals(product.getId()))
                .map(p -> toSiblingResponse(p, false))
                .toList();
    }

    /**
     * BATCH: ro'yxatdagi barcha mahsulotlar uchun sibling colors'ni bitta query bilan
     * hisoblaydi (N+1 o'rniga) — natija: productId -> sibling response list.
     */
    private Map<UUID, List<ProductColorSiblingResponse>> buildSiblingColorsBatch(List<Product> products) {
        Map<UUID, List<ProductColorSiblingResponse>> result = new HashMap<>();
        if (products == null || products.isEmpty()) {
            return result;
        }
        Set<UUID> groupIds = products.stream()
                .map(Product::getGroupId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (groupIds.isEmpty()) {
            products.forEach(p -> result.put(p.getId(), List.of()));
            return result;
        }
        Map<UUID, List<Product>> byGroup = productRepository.findByGroupIdIn(groupIds).stream()
                .collect(Collectors.groupingBy(Product::getGroupId));
        for (Product p : products) {
            List<ProductColorSiblingResponse> siblings = p.getGroupId() == null ? List.of()
                    : byGroup.getOrDefault(p.getGroupId(), List.of()).stream()
                            .filter(s -> !s.getId().equals(p.getId()))
                            .map(s -> toSiblingResponse(s, false))
                            .toList();
            result.put(p.getId(), siblings);
        }
        return result;
    }

    private ProductColorSiblingResponse toSiblingResponse(Product p, boolean isCurrent) {
        return ProductColorSiblingResponse.builder()
                .id(p.getId())
                .color(p.getColor())
                .colorCode(p.getColorCode())
                .barcode(p.getBarcode())
                .imageUrl(resolveFirstImageUrl(p))
                .active(p.isActive())
                .isCurrent(isCurrent)
                .build();
    }

    /** Desktop app uchun: joriy mahsulot va uning barcha rang guruhidoshlarini qaytaradi. */
    @Transactional(readOnly = true)
    public List<ProductColorSiblingResponse> listColorSiblings(UUID productId) {
        Product product = requireProduct(productId);
        if (product.getGroupId() == null) {
            return List.of(ProductColorSiblingResponse.builder()
                    .id(product.getId())
                    .color(product.getColor())
                    .colorCode(product.getColorCode())
                    .barcode(product.getBarcode())
                    .imageUrl(resolveFirstImageUrl(product))
                    .active(product.isActive())
                    .isCurrent(true)
                    .build());
        }
        return productRepository.findByGroupIdOrderByColorAsc(product.getGroupId()).stream()
                .map(p -> ProductColorSiblingResponse.builder()
                        .id(p.getId())
                        .color(p.getColor())
                        .colorCode(p.getColorCode())
                        .barcode(p.getBarcode())
                        .imageUrl(resolveFirstImageUrl(p))
                        .active(p.isActive())
                        .isCurrent(p.getId().equals(productId))
                        .build())
                .toList();
    }

    @Transactional
    public ProductImportResult importProducts(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Import file is required");
        }
        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        ProductImportResult result = ProductImportResult.builder().imported(0).skipped(0).errors(new ArrayList<>()).build();
        try {
            if (filename.endsWith(".csv")) {
                importCsv(file.getBytes(), result);
            } else if (filename.endsWith(".xlsx")) {
                importXlsx(file.getBytes(), result);
            } else {
                throw new IllegalArgumentException("Only .csv or .xlsx files are supported");
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read import file", ex);
        }
        return result;
    }

    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> exportProducts(String categoryIdParam, Boolean active) {
        UUID categoryId = parseOptionalUuid(categoryIdParam);
        Page<Product> page = productRepository.findAll(
                ProductSpecification.combined(null, null, categoryId, null, null, active, null, null, null),
                Pageable.unpaged()
        );
        byte[] bytes = buildExportWorkbook(page.getContent());
        String filename = "products-" + LocalDate.now() + ".xlsx";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
        return ResponseEntity.ok().headers(headers).body(bytes);
    }

    @Transactional(readOnly = true)
    public StockSummaryResponse stockSummary(UUID productId) {
        Product product = requireProduct(productId);
        List<Map<String, Object>> warehouses = referenceDataService.listWarehousesSafe();
        List<StockSummaryResponse.WarehouseQuantity> rows = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (Map<String, Object> warehouse : warehouses) {
            UUID warehouseId = safeUuid(warehouse.get("id"));
            if (warehouseId == null) {
                continue;
            }
            referenceDataService.fetchWarehouseStockLine(warehouseId, productId).ifPresent(snapshot -> {
                String warehouseName = warehouse.get("name") instanceof String n ? n : null;
                rows.add(StockSummaryResponse.WarehouseQuantity.builder()
                        .warehouseId(warehouseId)
                        .warehouseName(warehouseName)
                        .quantity(snapshot.quantity())
                        .reservedQuantity(snapshot.reservedQuantity())
                        .availableQuantity(snapshot.availableQuantity())
                        .build());
            });
        }
        for (StockSummaryResponse.WarehouseQuantity row : rows) {
            total = total.add(row.getQuantity() != null ? row.getQuantity() : BigDecimal.ZERO);
        }
        return StockSummaryResponse.builder()
                .productId(productId)
                .productName(product.getName())
                .totalQuantity(total)
                .warehouses(rows)
                .build();
    }

    private Product requireProduct(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", String.valueOf(id)));
    }

    private ProductResponse toResponse(Product p) {
        List<String> paths = resolveImagePaths(p);
        List<String> urls = paths.stream()
                .filter(p2 -> p2 != null && p2.contains("/thumb_"))
                .map(storageService::toPublicUrl)
                .filter(u -> u != null && !u.isBlank())
                .toList();
        String categoryName = p.getCategoryId() == null ? null : referenceDataService.resolveCategoryName(p.getCategoryId());
        String brandName = p.getBrandId() == null ? null : referenceDataService.resolveBrandName(p.getBrandId());
        String manufacturerName = p.getManufacturerId() != null
                ? referenceDataService.resolveManufacturerName(p.getManufacturerId())
                : p.getManufacturerName();
        List<ProductColorSiblingResponse> siblingColors = buildSiblingColors(p);
        Set<String> tags = p.getTags() == null ? Set.of() : p.getTags().stream()
                .map(ProductTag::getName)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return ProductResponse.fromEntity(p, urls, categoryName, brandName, manufacturerName, siblingColors, tags);
    }

    /** BATCH versiya: sibling colors va nomlar map'lar orqali beriladi — N+1'siz (list endpointlari uchun). */
    private ProductResponse toResponse(
            Product p,
            Map<UUID, List<ProductColorSiblingResponse>> siblingMap,
            Map<UUID, String> categoryNameMap,
            Map<UUID, String> brandNameMap,
            Map<UUID, String> manufacturerNameMap
    ) {
        List<String> paths = resolveImagePaths(p);
        List<String> urls = paths.stream()
                .filter(p2 -> p2 != null && p2.contains("/thumb_"))
                .map(storageService::toPublicUrl)
                .filter(u -> u != null && !u.isBlank())
                .toList();
        String categoryName = p.getCategoryId() == null ? null : categoryNameMap.get(p.getCategoryId());
        String brandName = p.getBrandId() == null ? null : brandNameMap.get(p.getBrandId());
        String manufacturerName = p.getManufacturerId() != null
                ? manufacturerNameMap.get(p.getManufacturerId())
                : p.getManufacturerName();
        List<ProductColorSiblingResponse> siblingColors = siblingMap.getOrDefault(p.getId(), List.of());
        Set<String> tags = p.getTags() == null ? Set.of() : p.getTags().stream()
                .map(ProductTag::getName)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return ProductResponse.fromEntity(p, urls, categoryName, brandName, manufacturerName, siblingColors, tags);
    }

    /** BATCH: id ro'yxati bo'yicha mahsulotlarni bitta so'rovda qaytaradi (cart/order uchun). */
    @Transactional(readOnly = true)
    public List<ProductResponse> getProductsByIds(Collection<UUID> ids, String lang) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<Product> products = productRepository.findAllById(ids);
        Map<UUID, List<ProductColorSiblingResponse>> siblingMap = buildSiblingColorsBatch(products);
        Map<UUID, String> categoryNameMap = referenceDataService.resolveCategoryNames(
                products.stream().map(Product::getCategoryId).filter(Objects::nonNull).collect(Collectors.toSet()));
        Map<UUID, String> brandNameMap = referenceDataService.resolveBrandNames(
                products.stream().map(Product::getBrandId).filter(Objects::nonNull).collect(Collectors.toSet()));
        Map<UUID, String> manufacturerNameMap = referenceDataService.resolveManufacturerNames(
                products.stream().map(Product::getManufacturerId).filter(Objects::nonNull).collect(Collectors.toSet()));
        List<ProductResponse> result = enrichWithPricing(products.stream()
                .map(p -> toResponse(p, siblingMap, categoryNameMap, brandNameMap, manufacturerNameMap))
                .toList());
        if (lang != null && !lang.isBlank()) {
            result = result.stream().map(r -> slimLocalized(r, lang)).toList();
        }
        return result;
    }

    // ── Lokalizatsiya + yengil (slim) payload ──

    private static final List<String> SUPPORTED_LANGS = List.of("uz", "ru");

    private static String normalizeLang(String lang) {
        if (lang == null) {
            return null;
        }
        String code = lang.trim().toLowerCase(Locale.ROOT);
        return SUPPORTED_LANGS.contains(code) ? code : null;
    }

    private static String localize(Map<String, String> translations, String lang, String fallback) {
        if (translations == null || lang == null) {
            return fallback;
        }
        for (Map.Entry<String, String> e : translations.entrySet()) {
            if (lang.equalsIgnoreCase(e.getKey())) {
                String value = e.getValue();
                return (value != null && !value.isBlank()) ? value : fallback;
            }
        }
        return fallback;
    }

    private static void applyLocalizedFields(ProductResponse r, String lang, boolean includeDescription) {
        r.setName(localize(r.getNameTranslations(), lang, r.getName()));
        r.setShortDescription(localize(r.getShortDescriptionTranslations(), lang, r.getShortDescription()));
        r.setMaterial(localize(r.getMaterialTranslations(), lang, r.getMaterial()));
        r.setCountryOfOrigin(localize(r.getCountryOfOriginTranslations(), lang, r.getCountryOfOrigin()));
        r.setManufacturerName(localize(r.getManufacturerNameTranslations(), lang, r.getManufacturerName()));
        if (includeDescription) {
            r.setDescription(localize(r.getDescriptionTranslations(), lang, r.getDescription()));
        }
    }

    private static void stripTranslations(ProductResponse r) {
        r.setNameTranslations(Map.of());
        r.setDescriptionTranslations(Map.of());
        r.setShortDescriptionTranslations(Map.of());
        r.setMaterialTranslations(Map.of());
        r.setCountryOfOriginTranslations(Map.of());
        r.setManufacturerNameTranslations(Map.of());
    }

    /**
     * Mobile ro'yxat (list) uchun: til bo'yicha lokalizatsiya + og'ir maydonlarni olib
     * tashlash — payload hajmi sezilarli kichrayadi, Gson parse tezlashadi.
     */
    private ProductResponse slimLocalized(ProductResponse r, String lang) {
        String code = normalizeLang(lang);
        if (code == null) {
            return r;
        }
        applyLocalizedFields(r, code, false);
        stripTranslations(r);
        r.setDescription(null);
        r.setMetaDescription(null);
        r.setSiblingColors(List.of());
        r.setTags(Set.of());
        return r;
    }

    /** Detail (mobile) uchun: lokalizatsiya + translation map'larni olib tashlash (boshqa hamma maydon saqlanadi). */
    private ProductResponse localizedDetail(ProductResponse r, String lang) {
        String code = normalizeLang(lang);
        if (code == null) {
            return r;
        }
        applyLocalizedFields(r, code, true);
        stripTranslations(r);
        return r;
    }

    private void appendUploadedImages(Product product, List<MultipartFile> images) {
        if (images == null || images.isEmpty()) {
            return;
        }
        for (MultipartFile file : images) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            try {
                // 3 xil variant (ORIGINAL, THUMB, MEDIUM) yaratiladi
                Map<mexa.club.productservice.image.ImageVariant, String> variants =
                        storageService.saveWithVariants(product.getId(), file);
                // Barcha variantlarni DB'ga saqlaymiz
                for (mexa.club.productservice.image.ImageVariant variant : mexa.club.productservice.image.ImageVariant.values()) {
                    String path = variants.get(variant);
                    if (path != null) {
                        product.getImagePaths().add(path);
                    }
                }
            } catch (IOException e) {
                throw new IllegalStateException("Failed to store product image", e);
            }
        }
    }

    private static void applyRequest(ProductRequest dto, Product product, boolean applyActiveFromDto) {
        product.setName(dto.getName());
        product.setBarcode(dto.getBarcode());
        product.setColor(dto.getColor());
        product.setColorCode(dto.getColorCode());
        if (dto.getGroupId() != null) {
            product.setGroupId(dto.getGroupId());
        }
        product.setCategoryId(dto.getCategoryId());
        product.setBrandId(dto.getBrandId());
        product.setManufacturerId(dto.getManufacturerId());
        product.setManufacturerName(dto.getManufacturerName());
        product.setUnit(dto.getUnit());
        product.setLeadTimeDays(dto.getLeadTimeDays());
        if (dto.getDeliveryDaysMin() != null && dto.getDeliveryDaysMax() != null
                && dto.getDeliveryDaysMin() > dto.getDeliveryDaysMax()) {
            throw new IllegalArgumentException("deliveryDaysMin must be <= deliveryDaysMax");
        }
        product.setDeliveryDaysMin(dto.getDeliveryDaysMin());
        product.setDeliveryDaysMax(dto.getDeliveryDaysMax());
        product.setMinStock(dto.getMinStock());
        product.setWeight(dto.getWeight());
        product.setLength(dto.getLength());
        product.setWidth(dto.getWidth());
        product.setHeight(dto.getHeight());
        product.setPackageType(dto.getPackageType());
        product.setFragile(dto.isFragile());
        product.setFeatured(dto.isFeatured());
        product.setDigital(dto.isDigital());
        if (dto.getStatus() != null) {
            product.setStatus(dto.getStatus());
        }
        product.setDescription(dto.getDescription());
        product.setShortDescription(dto.getShortDescription());
        product.setSku(dto.getSku());
        product.setSlug(dto.getSlug());
        product.setMetaDescription(dto.getMetaDescription());
        product.setMaterial(dto.getMaterial());
        product.setCountryOfOrigin(dto.getCountryOfOrigin());
        if (dto.getNameTranslations() != null) product.setNameTranslations(dto.getNameTranslations());
        if (dto.getDescriptionTranslations() != null) product.setDescriptionTranslations(dto.getDescriptionTranslations());
        if (dto.getShortDescriptionTranslations() != null) product.setShortDescriptionTranslations(dto.getShortDescriptionTranslations());
        if (dto.getMaterialTranslations() != null) product.setMaterialTranslations(dto.getMaterialTranslations());
        if (dto.getCountryOfOriginTranslations() != null) product.setCountryOfOriginTranslations(dto.getCountryOfOriginTranslations());
        if (dto.getManufacturerNameTranslations() != null) product.setManufacturerNameTranslations(dto.getManufacturerNameTranslations());
        product.setWarrantyMonths(dto.getWarrantyMonths());
        if (applyActiveFromDto) {
            product.setActive(dto.isActive());
        }
    }

    private void applyTags(Product product, List<String> tags) {
        if (product.getTags() == null) {
            product.setTags(new LinkedHashSet<>());
        }
        product.getTags().clear();
        if (tags == null || tags.isEmpty()) {
            return;
        }
        for (String raw : tags) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String name = raw.trim().toLowerCase(Locale.ROOT);
            ProductTag tag = productTagRepository.findByName(name).orElseGet(() -> {
                ProductTag t = new ProductTag();
                t.setName(name);
                return productTagRepository.save(t);
            });
            product.getTags().add(tag);
        }
    }

    private static UUID parseOptionalUuid(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(s.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid UUID: " + s);
        }
    }

    private void importCsv(byte[] bytes, ProductImportResult result) {
        List<String> lines = Arrays.stream(new String(bytes, StandardCharsets.UTF_8).split("\\R"))
                .filter(l -> l != null && !l.isBlank())
                .toList();
        if (lines.isEmpty()) {
            return;
        }
        for (int i = 1; i < lines.size(); i++) {
            String[] cols = lines.get(i).split(",", -1);
            processImportRow(i + 1, cols, result);
        }
    }

    private void importXlsx(byte[] bytes, ProductImportResult result) {
        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = workbook.getSheetAt(0);
            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) {
                    continue;
                }
                String[] cols = new String[11];
                for (int c = 0; c < cols.length; c++) {
                    Cell cell = row.getCell(c);
                    cols[c] = cell == null ? "" : String.valueOf(cell).trim();
                }
                processImportRow(r + 1, cols, result);
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to parse xlsx", ex);
        }
    }

    private void processImportRow(int rowNum, String[] cols, ProductImportResult result) {
        try {
            String name = value(cols, 0);
            String barcode = value(cols, 1);
            String unit = value(cols, 2);
            if (name.isBlank() || barcode.isBlank() || unit.isBlank()) {
                throw new IllegalArgumentException("name, barcode and unit are required");
            }
            if (productRepository.findByBarcode(barcode).isPresent()) {
                result.setSkipped(result.getSkipped() + 1);
                result.getErrors().add(new ProductImportResult.RowError(rowNum, "barcode already exists: " + barcode));
                return;
            }
            ProductRequest req = new ProductRequest();
            req.setName(name);
            req.setBarcode(barcode);
            req.setUnit(unit);
            req.setCategoryId(referenceDataService.resolveOrCreateCategoryByName(value(cols, 3)));
            req.setBrandId(referenceDataService.resolveOrCreateBrandByName(value(cols, 4)));
            req.setManufacturerId(referenceDataService.resolveOrCreateManufacturerByName(value(cols, 5)));
            req.setWeight(parseDouble(value(cols, 6)));
            req.setActive(parseBoolean(value(cols, 9), true));
            ProductResponse created = createProduct(req);
            BigDecimal salePrice = parseBigDecimal(value(cols, 7));
            if (salePrice.compareTo(BigDecimal.ZERO) > 0) {
                ProductPrice price = new ProductPrice();
                price.setProductId(created.getId());
                price.setSalePrice(salePrice);
                price.setEffectiveDate(LocalDateTime.now());
                productPriceRepository.save(price);
            }
            result.setImported(result.getImported() + 1);
        } catch (Exception ex) {
            result.setSkipped(result.getSkipped() + 1);
            result.getErrors().add(new ProductImportResult.RowError(rowNum, ex.getMessage()));
        }
    }

    private byte[] buildExportWorkbook(List<Product> products) {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("products");
            Row h = sheet.createRow(0);
            String[] headers = {"ID","Name","Barcode","Unit","Category","Brand","Manufacturer","Weight","SalePrice","Active","CreatedAt"};
            for (int i = 0; i < headers.length; i++) {
                h.createCell(i).setCellValue(headers[i]);
            }
            int rowNum = 1;
            for (Product p : products) {
                Row row = sheet.createRow(rowNum++);
                ProductPrice price = productPriceRepository.findByProductIdOrderByEffectiveDateDesc(p.getId()).stream().findFirst().orElse(null);
                row.createCell(0).setCellValue(String.valueOf(p.getId()));
                row.createCell(1).setCellValue(p.getName());
                row.createCell(2).setCellValue(p.getBarcode());
                row.createCell(3).setCellValue(p.getUnit());
                row.createCell(4).setCellValue(referenceDataService.resolveCategoryName(p.getCategoryId()));
                row.createCell(5).setCellValue(referenceDataService.resolveBrandName(p.getBrandId()));
                row.createCell(6).setCellValue(referenceDataService.resolveManufacturerName(p.getManufacturerId()));
                row.createCell(7).setCellValue(p.getWeight());
                row.createCell(8).setCellValue(price != null && price.getSalePrice() != null ? price.getSalePrice().toPlainString() : "");
                row.createCell(9).setCellValue(p.isActive());
                row.createCell(10).setCellValue(p.getCreatedAt() != null ? p.getCreatedAt().toString() : "");
            }
            wb.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to build export xlsx", ex);
        }
    }

    private static String value(String[] cols, int idx) {
        return idx < cols.length && cols[idx] != null ? cols[idx].trim() : "";
    }

    private static BigDecimal parseBigDecimal(String value) {
        if (value == null || value.isBlank()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (Exception ex) {
            return BigDecimal.ZERO;
        }
    }

    private static double parseDouble(String value) {
        if (value == null || value.isBlank()) {
            return 0.0d;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (Exception ex) {
            return 0.0d;
        }
    }

    private static boolean parseBoolean(String value, boolean defaultVal) {
        if (value == null || value.isBlank()) {
            return defaultVal;
        }
        return "true".equalsIgnoreCase(value) || "1".equals(value.trim()) || "yes".equalsIgnoreCase(value.trim());
    }

    private static UUID safeUuid(Object value) {
        if (!(value instanceof String s) || s.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(s);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}

