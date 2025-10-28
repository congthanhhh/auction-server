package com.thanh.auction_server.mapper;

import com.thanh.auction_server.dto.request.ProductRequest;
import com.thanh.auction_server.dto.request.ProductUpdateRequest;
import com.thanh.auction_server.dto.response.ProductResponse;
import com.thanh.auction_server.dto.response.SimpleProductResponse;
import com.thanh.auction_server.entity.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

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

    // Cập nhật Entity từ Request (tương tự toProduct)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "seller", ignore = true)
    @Mapping(target = "images", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateProduct(@MappingTarget Product product, ProductUpdateRequest request);

    // Hàm trợ giúp map Product -> SimpleProductResponse
    @Named("productToSimpleProductResponse")
    SimpleProductResponse productToSimpleProductResponse(Product product);

}