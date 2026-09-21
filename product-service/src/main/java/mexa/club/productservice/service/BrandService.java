package mexa.club.productservice.service;

import mexa.club.productservice.dto.BrandRequest;
import mexa.club.productservice.dto.BrandResponse;
import mexa.club.productservice.entity.Brand;
import mexa.club.productservice.exception.ResourceNotFoundException;
import mexa.club.productservice.repository.BrandLogoRepository;
import mexa.club.productservice.repository.BrandRepository;
import mexa.club.productservice.storage.StorageService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
public class BrandService {

    private final BrandRepository brandRepository;
    private final BrandLogoRepository brandLogoRepository;
    private final StorageService storageService;

    public BrandService(BrandRepository brandRepository,
                        BrandLogoRepository brandLogoRepository,
                        StorageService storageService) {
        this.brandRepository = brandRepository;
        this.brandLogoRepository = brandLogoRepository;
        this.storageService = storageService;
    }

    @Cacheable("brands")
    @Transactional(readOnly = true)
    public Page<BrandResponse> list(String name, Boolean active, Pageable pageable) {
        boolean hasName = name != null && !name.isBlank();
        Page<Brand> page;
        if (hasName && active != null) {
            page = brandRepository.findByNameContainingIgnoreCaseAndActiveOrderByNameAsc(name.trim(), active, pageable);
        } else if (hasName) {
            page = brandRepository.findByNameContainingIgnoreCaseOrderByNameAsc(name.trim(), pageable);
        } else if (active != null) {
            page = brandRepository.findByActiveOrderByNameAsc(active, pageable);
        } else {
            page = brandRepository.findAllByOrderByNameAsc(pageable);
        }
        return page.map(this::toResponse);
    }

    @Cacheable("brands")
    @Transactional(readOnly = true)
    public BrandResponse get(UUID id) {
        return toResponse(require(id));
    }

    @CacheEvict(cacheNames = "brands", allEntries = true)
    @Transactional
    public BrandResponse create(BrandRequest req) {
        if (brandRepository.existsByNameIgnoreCase(req.name().trim())) {
            throw new IllegalArgumentException("Bu nomli brend allaqachon mavjud: " + req.name());
        }
        Brand brand = new Brand();
        brand.setName(req.name().trim());
        brand.setDescription(req.description());
        if (req.nameTranslations() != null) brand.setNameTranslations(req.nameTranslations());
        if (req.descriptionTranslations() != null) brand.setDescriptionTranslations(req.descriptionTranslations());
        brand.setActive(req.active() != null ? req.active() : true);
        return toResponse(brandRepository.save(brand));
    }

    @CacheEvict(cacheNames = "brands", allEntries = true)
    @Transactional
    public BrandResponse update(UUID id, BrandRequest req) {
        Brand brand = require(id);
        if (!brand.getName().equalsIgnoreCase(req.name().trim())
                && brandRepository.existsByNameIgnoreCase(req.name().trim())) {
            throw new IllegalArgumentException("Bu nomli brend allaqachon mavjud: " + req.name());
        }
        brand.setName(req.name().trim());
        brand.setDescription(req.description());
        if (req.nameTranslations() != null) brand.setNameTranslations(req.nameTranslations());
        if (req.descriptionTranslations() != null) brand.setDescriptionTranslations(req.descriptionTranslations());
        if (req.active() != null) brand.setActive(req.active());
        return toResponse(brandRepository.save(brand));
    }

    @CacheEvict(cacheNames = "brands", allEntries = true)
    @Transactional
    public void delete(UUID id) {
        if (!brandRepository.existsById(id)) {
            throw new ResourceNotFoundException("Brand", id.toString());
        }
        brandRepository.deleteById(id);
    }

    private Brand require(UUID id) {
        return brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Brand", id.toString()));
    }

    private BrandResponse toResponse(Brand b) {
        String logoUrl = brandLogoRepository.findById(b.getId())
                .map(logo -> storageService.toPublicUrl(logo.getPath()))
                .orElse(null);
        return new BrandResponse(
                b.getId(),
                b.getName(),
                b.getDescription(),
                b.getNameTranslations(),
                b.getDescriptionTranslations(),
                b.isActive(),
                logoUrl,
                b.getCreatedAt(),
                b.getUpdatedAt()
        );
    }
}
