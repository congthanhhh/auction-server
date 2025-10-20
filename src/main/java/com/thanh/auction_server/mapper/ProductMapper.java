package com.thanh.auction_server.mapper;

import com.thanh.auction_server.dto.request.ProductRequest;
import com.thanh.auction_server.dto.request.ProductUpdateRequest;
import com.thanh.auction_server.dto.response.ProductResponse;
import com.thanh.auction_server.dto.response.SimpleUserResponse; // Import SimpleUserResponse
import com.thanh.auction_server.entity.Category;
import com.thanh.auction_server.entity.Product;
import com.thanh.auction_server.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = {CategoryMapper.class}) // uses = CategoryMapper để MapStruct biết cách map Category
public interface ProductMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "seller", ignore = true)
    @Mapping(target = "images", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Product toProduct(ProductRequest request);

    // Ánh xạ từ Entity sang Response
    // MapStruct sẽ tự động gọi CategoryMapper để map Category sang CategoryResponse
    // Chúng ta cần định nghĩa cách map User sang SimpleUserResponse
    @Mapping(source = "seller", target = "seller") // Định nghĩa ánh xạ cho seller
    ProductResponse toProductResponse(Product product);

    // Hàm ánh xạ User sang SimpleUserResponse (MapStruct sẽ dùng hàm này)
    SimpleUserResponse userToSimpleUserResponse(User user);

    // Cập nhật Entity từ Request (tương tự toProduct)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "seller", ignore = true)
    @Mapping(target = "images", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateProduct(@MappingTarget Product product, ProductUpdateRequest request);
}