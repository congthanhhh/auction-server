package com.thanh.auction_server.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductUpdateRequest {

    @NotBlank(message = "Tên sản phẩm không được để trống")
    String name;

    String description;

    @NotNull(message = "Giá khởi điểm không được để trống")
    @DecimalMin(value = "0.0", inclusive = false, message = "Giá khởi điểm phải lớn hơn 0")
    BigDecimal startPrice;

    @NotNull(message = "Danh mục không được để trống")
    Long categoryId;

    String attributes;

    List<Integer> imageIdsToAdd;
    List<Integer> imageIdsToRemove;
}
