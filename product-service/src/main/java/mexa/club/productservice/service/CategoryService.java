package mexa.club.productservice.service;

import mexa.club.productservice.dto.CategoryRequest;
import mexa.club.productservice.dto.CategoryResponse;
import mexa.club.productservice.entity.Category;
import mexa.club.productservice.exception.ResourceNotFoundException;
import mexa.club.productservice.repository.CategoryRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Cacheable("categories")
    @Transactional(readOnly = true)
    public Page<CategoryResponse> list(String name, Boolean active, Pageable pageable) {
        boolean hasName = name != null && !name.isBlank();
        Page<Category> page;
        if (hasName && active != null) {
            page = categoryRepository.findByNameContainingIgnoreCaseAndActiveOrderByNameAsc(name.trim(), active, pageable);
        } else if (hasName) {
            page = categoryRepository.findByNameContainingIgnoreCaseOrderByNameAsc(name.trim(), pageable);
        } else if (active != null) {
            page = categoryRepository.findByActiveOrderByNameAsc(active, pageable);
        } else {
            page = categoryRepository.findAllByOrderByNameAsc(pageable);
        }
        return page.map(this::toResponse);
    }

    @Cacheable("categories")
    @Transactional(readOnly = true)
    public CategoryResponse get(UUID id) {
        return toResponse(require(id));
    }

    @Cacheable("categories")
    @Transactional(readOnly = true)
    public List<CategoryResponse> listRoots() {
        return categoryRepository.findRootCategories()
                .stream().map(this::toResponse).toList();
    }

    @CacheEvict(cacheNames = "categories", allEntries = true)
    @Transactional
    public CategoryResponse create(CategoryRequest req) {
        if (categoryRepository.existsByNameIgnoreCase(req.getName().trim())) {
            throw new IllegalArgumentException("Bu nomli kategoriya allaqachon mavjud: " + req.getName());
        }
        if (req.getParentId() != null && !categoryRepository.existsById(req.getParentId())) {
            throw new IllegalArgumentException("Ota-kategoriya topilmadi: " + req.getParentId());
        }
        Category category = new Category();
        category.setName(req.getName().trim());
        category.setDescription(req.getDescription());
        category.setNameTranslations(req.getNameTranslations());
        category.setDescriptionTranslations(req.getDescriptionTranslations());
        category.setParentId(req.getParentId());
        category.setImageUrl(req.getImageUrl());
        category.setActive(req.getActive() != null ? req.getActive() : true);
        return toResponse(categoryRepository.save(category));
    }

    @CacheEvict(cacheNames = "categories", allEntries = true)
    @Transactional
    public CategoryResponse update(UUID id, CategoryRequest req) {
        Category category = require(id);
        if (!category.getName().equalsIgnoreCase(req.getName().trim())
                && categoryRepository.existsByNameIgnoreCase(req.getName().trim())) {
            throw new IllegalArgumentException("Bu nomli kategoriya allaqachon mavjud: " + req.getName());
        }
        if (req.getParentId() != null && req.getParentId().equals(id)) {
            throw new IllegalArgumentException("Kategoriya o'ziga ota-kategoriya bo'la olmaydi");
        }
        if (req.getParentId() != null && !categoryRepository.existsById(req.getParentId())) {
            throw new IllegalArgumentException("Ota-kategoriya topilmadi: " + req.getParentId());
        }
        category.setName(req.getName().trim());
        category.setDescription(req.getDescription());
        category.setNameTranslations(req.getNameTranslations());
        category.setDescriptionTranslations(req.getDescriptionTranslations());
        category.setParentId(req.getParentId());
        if (req.getImageUrl() != null) category.setImageUrl(req.getImageUrl());
        if (req.getActive() != null) category.setActive(req.getActive());
        return toResponse(categoryRepository.save(category));
    }

    @CacheEvict(cacheNames = "categories", allEntries = true)
    @Transactional
    public CategoryResponse updateImage(UUID id, String imageUrl) {
        Category category = require(id);
        category.setImageUrl(imageUrl);
        return toResponse(categoryRepository.save(category));
    }

    @CacheEvict(cacheNames = "categories", allEntries = true)
    @Transactional
    public void delete(UUID id) {
        if (!categoryRepository.existsById(id)) {
            throw new ResourceNotFoundException("Category", id.toString());
        }
        categoryRepository.deleteById(id);
    }

    private Category require(UUID id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id.toString()));
    }

    private CategoryResponse toResponse(Category c) {
        String parentName = null;
        if (c.getParentId() != null) {
            parentName = categoryRepository.findById(c.getParentId())
                    .map(Category::getName).orElse(null);
        }
        return new CategoryResponse(
                c.getId(),
                c.getName(),
                c.getDescription(),
                c.getNameTranslations(),
                c.getDescriptionTranslations(),
                c.getParentId(),
                parentName,
                c.getImageUrl(),
                c.isActive(),
                c.getCreatedAt(),
                c.getUpdatedAt()
        );
    }
}
