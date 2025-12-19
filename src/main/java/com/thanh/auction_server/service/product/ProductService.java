package com.thanh.auction_server.service.product;

import com.thanh.auction_server.constants.ErrorMessage;
import com.thanh.auction_server.dto.request.ProductRequest;
import com.thanh.auction_server.dto.request.ProductSearchRequest;
import com.thanh.auction_server.dto.request.ProductUpdateRequest;
import com.thanh.auction_server.dto.response.PageResponse;
import com.thanh.auction_server.dto.response.ProductResponse;
import com.thanh.auction_server.entity.Image;
import com.thanh.auction_server.entity.Product;
import com.thanh.auction_server.exception.ResourceNotFoundException;
import com.thanh.auction_server.exception.UserNotFoundException;
import com.thanh.auction_server.mapper.ProductMapper;
import com.thanh.auction_server.repository.CategoryRepository;
import com.thanh.auction_server.repository.ImageRepository;
import com.thanh.auction_server.repository.ProductRepository;
import com.thanh.auction_server.repository.UserRepository;
import com.thanh.auction_server.specification.ProductSpecification;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Service
public class ProductService {
    ProductRepository productRepository;
    CategoryRepository categoryRepository;
    ImageRepository imageRepository;
    ImageService imageService;
    UserRepository userRepository;
    ProductMapper productMapper;

    @Transactional
    public ProductResponse createProduct(ProductRequest request) {

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        var seller = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(ErrorMessage.USER_NOT_FOUND + username));

        var category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessage.CATEGORY_NOT_FOUND + request.getCategoryId()));
        var product = productMapper.toProduct(request);
        product.setSeller(seller);
        product.setCategory(category);
        product.setCreatedAt(LocalDateTime.now());
        product.setIsActive(true);
        product.setImages(new HashSet<>());
        Set<Image> imagesToAssociate = new HashSet<>();
        if (request.getImageIds() != null && !request.getImageIds().isEmpty()) {
            List<Image> foundImages = imageRepository.findAllById(request.getImageIds());
            if (foundImages.size() != request.getImageIds().size()) {
                log.warn("Mismatch in image IDs found. Requested: {}, Found: {}",
                        request.getImageIds(),
                        foundImages.stream().map(Image::getId).collect(Collectors.toList()));
            }
            imagesToAssociate.addAll(foundImages);
            product.getImages().addAll(imagesToAssociate);
        }


        var savedProduct = productRepository.save(product);
        return productMapper.toProductResponse(savedProduct);
    }

    public PageResponse<ProductResponse> getAllProducts(int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size);
        var pageData = productRepository.findAllByIsActiveTrue(pageable);
        return PageResponse.<ProductResponse>builder()
                .currentPage(page)
                .totalPages(pageData.getTotalPages())
                .pageSize(pageData.getSize())
                .totalPages(pageData.getTotalPages())
//                .data(pageData.map(productMapper::toProductResponse).getContent())
                .data(pageData.getContent().stream().map(productMapper::toProductResponse).toList())
                .build();
    }

    public ProductResponse getProductById(Long id) {
        var product = productRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessage.PRODUCT_NOT_FOUND + id));
        return productMapper.toProductResponse(product);
    }

//    public ProductResponse updateProduct1(Long id, ProductRequest request) {
//        var existingProduct = productRepository.findByIdAndIsActiveTrue(id)
//                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessage.PRODUCT_NOT_FOUND + id));
//
//        if (!existingProduct.getCategory().getId().equals(request.getCategoryId())) {
//            var category = categoryRepository.findById(request.getCategoryId())
//                    .orElseThrow(() -> new ResourceNotFoundException(ErrorMessage.CATEGORY_NOT_FOUND + request.getCategoryId()));
//            existingProduct.setCategory(category);
//        }
//        productMapper.updateProduct(existingProduct, request);
//
//
//        var updatedProduct = productRepository.save(existingProduct);
//        return productMapper.toProductResponse(updatedProduct);
//    }

    @Transactional
    public ProductResponse updateProduct(Long id, ProductUpdateRequest   request) {
        var existingProduct = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessage.PRODUCT_NOT_FOUND + id));

        // Kiểm tra quyền cập nhật (Chỉ seller)
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!existingProduct.getSeller().getUsername().equals(currentUsername)) {
            throw new AccessDeniedException("Bạn không có quyền chỉnh sửa sản phẩm này.");
        }

        // Cập nhật Category nếu ID thay đổi
        if (!existingProduct.getCategory().getId().equals(request.getCategoryId())) {
            log.debug("Category change detected for product ID {}. New category ID: {}", id, request.getCategoryId());
            var newCategory = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException(ErrorMessage.CATEGORY_NOT_FOUND+ request.getCategoryId()));
            existingProduct.setCategory(newCategory);
        }
        // 5. Dùng mapper để cập nhật các trường cơ bản (name, description, price, attributes)
        // Mapper này KHÔNG cập nhật images, category, seller
        productMapper.updateProduct(existingProduct, request);

        // 2. Xử lý Gỡ bỏ Ảnh (imageIdsToRemove)
        if (request.getImageIdsToRemove() != null && !request.getImageIdsToRemove().isEmpty()) {
            log.debug("Processing images to remove: {}", request.getImageIdsToRemove());
            Set<Image> imagesCurrentlyAssociated = existingProduct.getImages();

            // Lọc ra các ảnh thực sự cần xóa khỏi collection
            List<Image> imagesToRemove = imagesCurrentlyAssociated.stream()
                    .filter(img -> request.getImageIdsToRemove().contains(img.getId()))
                    .toList();

            if (!imagesToRemove.isEmpty()) {
                log.info("Removing {} images from product ID {}", imagesToRemove.size(), id);
                imagesToRemove.forEach(imagesCurrentlyAssociated::remove); // Xóa khỏi collection
//                imagesCurrentlyAssociated.removeAll(imagesToRemove);

                // QUAN TRỌNG: Xóa ảnh khỏi Cloudinary và DB Image (nếu cần)
                // Vì ảnh bị gỡ khỏi Product, nó có thể trở thành mồ côi
                for (Image imgToRemove : imagesToRemove) {
                    try {
                        // Gọi ImageService để xử lý xóa cả Cloudinary và DB
                        imageService.deleteImage(imgToRemove.getId());
                    } catch (Exception e) { // Bắt Exception chung
                        // Ghi log lỗi nhưng vẫn tiếp tục cập nhật Product
                        log.error("Error deleting image ID {} during product update: {}", imgToRemove.getId(), e.getMessage());
                    }
                }
            }
        }

        // 3. Xử lý Thêm Ảnh Mới (imageIdsToAdd)
        if (request.getImageIdsToAdd() != null && !request.getImageIdsToAdd().isEmpty()) {
            log.debug("Processing images to add: {}", request.getImageIdsToAdd());

            // Kiểm tra giới hạn số lượng ảnh (nếu đã implement)
            int currentImageCount = existingProduct.getImages().size();
            int imagesToAddCount = request.getImageIdsToAdd().size();
            // if (currentImageCount + imagesToAddCount > maxImageCount) {
            //     throw new UserNotFoundException("Thêm ảnh mới sẽ vượt quá giới hạn " + maxImageCount);
            // }

            List<Image> imagesToAdd = imageRepository.findAllById(request.getImageIdsToAdd());
            if (imagesToAdd.size() != request.getImageIdsToAdd().size()) {
                log.warn("Some image IDs provided for addition were not found.");
                // Quyết định: Ném lỗi hay chỉ thêm ảnh tìm thấy?
            }

            if (!imagesToAdd.isEmpty()) {
                log.info("Adding {} new images to product ID {}", imagesToAdd.size(), id);
                existingProduct.getImages().addAll(imagesToAdd); // Thêm vào collection
            }
        }

        // 4. Lưu Product cập nhật
        // JPA sẽ tự động xử lý việc thêm/xóa các bản ghi liên kết (nếu dùng bảng trung gian)
        // hoặc cập nhật khóa ngoại (nếu dùng @JoinColumn)
        var updatedProduct = productRepository.save(existingProduct);
        log.info("Product updated successfully with ID: {}", updatedProduct.getId());

        // 5. Map và trả về Response
        return productMapper.toProductResponse(updatedProduct);
    }

    public void deleteProduct(Long id) {
        var product = productRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessage.PRODUCT_NOT_FOUND + id));
        product.setIsActive(false);
        productRepository.save(product);
        log.info("Product with ID: {} has been deactivated", id);
    }

    public PageResponse<ProductResponse> searchProducts(ProductSearchRequest  request, int page, int size) {
        // 1. Tạo Pageable (có thể thêm sort ở đây nếu muốn)
        Pageable pageable = PageRequest.of(page - 1, size);

        // 2. Lấy Specification từ Class util
        Specification<Product> spec = ProductSpecification.getFilter(request);

        // 3. Gọi Repository (Hàm findAll này có sẵn nhờ JpaSpecificationExecutor)
        Page<Product> productPage = productRepository.findAll(spec, pageable);

        // 4. Map sang Response
        List<ProductResponse> responses = productPage.getContent().stream()
                .map(productMapper::toProductResponse)
                .collect(Collectors.toList());

        return PageResponse.<ProductResponse>builder()
                .currentPage(page)
                .totalPages(productPage.getTotalPages())
                .pageSize(productPage.getSize())
                .totalElements(productPage.getTotalElements())
                .data(responses)
                .build();
    }
}
