package com.thanh.auction_server.Controller;

import com.thanh.auction_server.dto.request.ProductRequest;
import com.thanh.auction_server.dto.request.ProductSearchRequest;
import com.thanh.auction_server.dto.request.ProductUpdateRequest;
import com.thanh.auction_server.dto.response.PageResponse;
import com.thanh.auction_server.dto.response.ProductResponse;
import com.thanh.auction_server.service.product.ProductService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/products")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProductController {
    ProductService productService;

    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(@RequestBody ProductRequest request) {
        return ResponseEntity.ok(productService.createProduct(request));
    }

    @GetMapping
    public ResponseEntity<PageResponse<ProductResponse>> getAllProducts(
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "size", required = false, defaultValue = "10") int size) {
        return ResponseEntity.<PageResponse<ProductResponse>>ok(productService.getAllProducts(page, size));
    }

    @GetMapping("/my-products")
    public ResponseEntity<List<ProductResponse>> getMyProducts() {
        return ResponseEntity.ok(productService.getProductsBySellerUsername());
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> updateProduct(@PathVariable Long id, @RequestBody ProductUpdateRequest request) {
        ProductResponse updatedProduct = productService.updateProduct(id, request);
        return ResponseEntity.ok(updatedProduct);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable Long id) {
        ProductResponse product = productService.getProductById(id);
        return ResponseEntity.ok(product);
    }

    @GetMapping("/search")
    public ResponseEntity<PageResponse<ProductResponse>> searchProducts(
            @ModelAttribute ProductSearchRequest request,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(productService.getAllProductsUser(request, page, size));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
    @PatchMapping("/{id}/restore")
    public ResponseEntity<Void> restoreProduct(@PathVariable Long id) {
        productService.enableProduct(id);
        return ResponseEntity.noContent().build();
    }
    //===============Admin chức năng==================
    @GetMapping("/admin/pending")
    public ResponseEntity<PageResponse<ProductResponse>> getPendingProducts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(productService.getPendingProducts(page, size));
    }
    // Duyệt hoặc từ chối
    @PatchMapping("/admin/{id}/verify")
    public ResponseEntity<String> verifyProduct(
            @PathVariable Long id,
            @RequestParam Boolean isApproved) {
        productService.verifyProduct(id, isApproved);
        return ResponseEntity.ok(isApproved ? "Đã duyệt sản phẩm" : "Đã từ chối sản phẩm");
    }
    @GetMapping("/admin/search")
    public ResponseEntity<PageResponse<ProductResponse>> searchProductsAdmin(
            @ModelAttribute ProductSearchRequest request,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(productService.getProductsForAdmin(request, page, size));
    }
}
