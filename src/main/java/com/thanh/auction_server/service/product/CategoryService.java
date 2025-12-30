package com.thanh.auction_server.service.product;

import com.thanh.auction_server.constants.ErrorMessage;
import com.thanh.auction_server.dto.request.CategoryRequest;
import com.thanh.auction_server.dto.response.CategoryResponse;
import com.thanh.auction_server.entity.Category;
import com.thanh.auction_server.exception.DataConflictException;
import com.thanh.auction_server.exception.ResourceNotFoundException;
import com.thanh.auction_server.mapper.CategoryMapper;
import com.thanh.auction_server.repository.CategoryRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Service
public class CategoryService {
    CategoryRepository categoryRepository;
    CategoryMapper categoryMapper;

    public CategoryResponse createCategory(CategoryRequest request) {
        if (categoryRepository.existsByName(request.getName())) {
            throw new DataConflictException(ErrorMessage.CATEGORY_ALREADY_EXIST);
        }
        Category category = categoryMapper.toCategory(request);
        return categoryMapper.toCategoryResponse(categoryRepository.save(category));
    }

    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll()
                .stream()
                .map(categoryMapper::toCategoryResponse)
                .collect(Collectors.toList());
    }

    public CategoryResponse getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessage.CATEGORY_NOT_FOUND + id));
        return categoryMapper.toCategoryResponse(category);
    }

    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        Category existingCategory = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessage.CATEGORY_NOT_FOUND + id));
        if (!existingCategory.getName().equals(request.getName())
                && categoryRepository.existsByName(request.getName())) {
            throw new DataConflictException(ErrorMessage.CATEGORY_ALREADY_EXIST);
        }
        categoryMapper.updateCategory(existingCategory, request);
        Category updatedCategory = categoryRepository.save(existingCategory);
        return categoryMapper.toCategoryResponse(updatedCategory);
    }

    // --- Xóa Category ---
    @Transactional
    public void deleteCategory(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new ResourceNotFoundException(ErrorMessage.CATEGORY_NOT_FOUND + id);
        }
        categoryRepository.deleteById(id);
    }

}
