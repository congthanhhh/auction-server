package com.thanh.auction_server.mapper;

import com.thanh.auction_server.dto.request.CategoryRequest;
import com.thanh.auction_server.dto.response.CategoryResponse;
import com.thanh.auction_server.entity.Category;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    Category toCategory(CategoryRequest request);

    CategoryResponse toCategoryResponse(Category category);

    void updateCategory(@MappingTarget Category category, CategoryRequest request);
}
