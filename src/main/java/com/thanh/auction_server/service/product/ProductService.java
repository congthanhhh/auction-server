package com.thanh.auction_server.service.product;

import com.thanh.auction_server.constants.ErrorMessage;
import com.thanh.auction_server.constants.LogAction;
import com.thanh.auction_server.constants.ProductStatus;
import com.thanh.auction_server.dto.request.ProductRequest;
import com.thanh.auction_server.dto.request.ProductSearchRequest;
import com.thanh.auction_server.dto.request.ProductUpdateRequest;
import com.thanh.auction_server.dto.response.PageResponse;
import com.thanh.auction_server.dto.response.ProductResponse;
import com.thanh.auction_server.entity.Image;
import com.thanh.auction_server.entity.Product;
import com.thanh.auction_server.exception.DataConflictException;
import com.thanh.auction_server.exception.ResourceNotFoundException;
import com.thanh.auction_server.exception.UserNotFoundException;
import com.thanh.auction_server.mapper.ProductMapper;
import com.thanh.auction_server.repository.*;
import com.thanh.auction_server.service.admin.AuditLogService;
import com.thanh.auction_server.service.auction.NotificationService;
import com.thanh.auction_server.specification.ProductSpecification;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
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
    BidRepository bidRepository;
    ImageRepository imageRepository;
    ImageService imageService;
    UserRepository userRepository;
    ProductMapper productMapper;
    AuditLogService auditLogService;
    NotificationService notificationService;

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
        product.setStatus(ProductStatus.WAITING_FOR_APPROVAL);
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
        // Báo admin sản phầm chờ duyệt
        if (savedProduct.getStatus() == ProductStatus.WAITING_FOR_APPROVAL) {
            String adminMsg = String.format("SẢN PHẨM MỚI: User '%s' vừa đăng bán '%s'. Vui lòng kiểm duyệt.",
                    savedProduct.getSeller().getUsername(), savedProduct.getName());
            String adminLink = "/admin/products";
            notificationService.sendNotificationToAllAdmins(adminMsg, adminLink);
        }
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

    public List<ProductResponse> getProductsBySellerUsername() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        var products = productRepository.findAllBySeller_UsernameAndIsActiveTrueAndNotInAuctionSession(username);
        return products.stream()
                .map(productMapper::toProductResponse)
                .collect(Collectors.toList());
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
    public ProductResponse updateProduct(Long id, ProductUpdateRequest  request) {
        var existingProduct = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessage.PRODUCT_NOT_FOUND + id));

        // Kiểm tra quyền cập nhật (Chỉ seller)
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!existingProduct.getSeller().getUsername().equals(currentUsername)) {
            throw new AccessDeniedException("Bạn không có quyền chỉnh sửa sản phẩm này.");
        }
        // Cập nhật Category nếu ID thay đổi
        if (!existingProduct.getCategory().getId().equals(request.getCategoryId())) {
            var newCategory = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException(ErrorMessage.CATEGORY_NOT_FOUND+ request.getCategoryId()));
            existingProduct.setCategory(newCategory);
        }
        // Mapper này KHÔNG cập nhật images, category, seller
        productMapper.updateProduct(existingProduct, request);
        // Xử lý Gỡ bỏ Ảnh (imageIdsToRemove)
        if (request.getImageIdsToRemove() != null && !request.getImageIdsToRemove().isEmpty()) {
            Set<Image> imagesCurrentlyAssociated = existingProduct.getImages();
            // Lọc ra các ảnh thực sự cần xóa khỏi collection
            List<Image> imagesToRemove = imagesCurrentlyAssociated.stream()
                    .filter(img -> request.getImageIdsToRemove().contains(img.getId()))
                    .toList();
            if (!imagesToRemove.isEmpty()) {
                log.info("Removing {} images from product ID {}", imagesToRemove.size(), id);
                imagesToRemove.forEach(imagesCurrentlyAssociated::remove); // Xóa khỏi collection
//                imagesCurrentlyAssociated.removeAll(imagesToRemove);

                for (Image imgToRemove : imagesToRemove) {
                    try {
                        imageService.deleteImage(imgToRemove.getId());
                    } catch (Exception e) { // Bắt Exception chung
                        log.error("Error deleting image ID {} during product update: {}", imgToRemove.getId(), e.getMessage());
                    }
                }
            }
        }
        // Xử lý Thêm Ảnh Mới (imageIdsToAdd)
        if (request.getImageIdsToAdd() != null && !request.getImageIdsToAdd().isEmpty()) {
            // Kiểm tra giới hạn số lượng ảnh (nếu đã implement)
            int currentImageCount = existingProduct.getImages().size();
            int imagesToAddCount = request.getImageIdsToAdd().size();
            // if (currentImageCount + imagesToAddCount > maxImageCount) {
            //     throw new UserNotFoundException("Thêm ảnh mới sẽ vượt quá giới hạn " + maxImageCount);
            // }
            List<Image> imagesToAdd = imageRepository.findAllById(request.getImageIdsToAdd());
            if (imagesToAdd.size() != request.getImageIdsToAdd().size()) {
                log.warn("Some image IDs provided for addition were not found.");
            }
            if (!imagesToAdd.isEmpty()) {
                log.info("Adding {} new images to product ID {}", imagesToAdd.size(), id);
                existingProduct.getImages().addAll(imagesToAdd);
            }
        }
        // Lưu sản phẩm đã cập nhật
        var updatedProduct = productRepository.save(existingProduct);
        return productMapper.toProductResponse(updatedProduct);
    }

    public void deleteProduct(Long id) {
        var product = productRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessage.PRODUCT_NOT_FOUND + id));
        product.setIsActive(false);
        auditLogService.saveLog(LogAction.DISABLE_PRODUCT, id.toString(),
                "Sản phẩm '" + product.getName() + "' đã bị xóa (vô hiệu hóa).");
        productRepository.save(product);
    }

    public void enableProduct(Long id) {
        var product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessage.PRODUCT_NOT_FOUND + id));
        product.setIsActive(true);
        auditLogService.saveLog(LogAction.ENABLE_PRODUCT, id.toString(),
                "Sản phẩm '" + product.getName() + "' đã được kích hoạt lại.");
        productRepository.save(product);
    }

    // Hàm Search dành cho USER
    public PageResponse<ProductResponse> getAllProductsUser(ProductSearchRequest request, int page, int size) {
        request.setStatus(ProductStatus.ACTIVE);
        request.setIsActive(true);
        return executeSearch(request, page, size);
    }

    // Hàm Search dành cho ADMIN
    public PageResponse<ProductResponse> getProductsForAdmin(ProductSearchRequest request, int page, int size) {
        return executeSearch(request, page, size);
    }

    private PageResponse<ProductResponse> executeSearch(ProductSearchRequest request, int page, int size) {
        Sort sortObj = Sort.by("createdAt").descending();
        if (request.getSort() != null) {
            switch (request.getSort().toLowerCase()) {
                case "oldest":
                    sortObj = Sort.by("createdAt").ascending();
                    break;
                case "price_asc":
                    sortObj = Sort.by("startPrice").ascending();
                    break;
                case "price_desc":
                    sortObj = Sort.by("startPrice").descending();
                    break;
                case "newest":
                default:
                    sortObj = Sort.by("createdAt").descending();
                    break;
            }
        }
        Pageable pageable = PageRequest.of(page - 1, size, sortObj);
        Specification<Product> spec = ProductSpecification.getFilter(request);
        Page<Product> productPage = productRepository.findAll(spec, pageable);
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

    // ---METHOD CHO ADMIN ---
    public PageResponse<ProductResponse> getPendingProducts(int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size);
        Page<Product> pageData = productRepository.findByStatus(ProductStatus.WAITING_FOR_APPROVAL, pageable);

        return PageResponse.<ProductResponse>builder()
                .currentPage(page)
                .totalPages(pageData.getTotalPages())
                .pageSize(pageData.getSize())
                .totalElements(pageData.getTotalElements())
                .data(pageData.getContent().stream().map(productMapper::toProductResponse).toList())
                .build();
    }
    public void verifyProduct(Long productId, boolean isApproved) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessage.PRODUCT_NOT_FOUND + productId));

        if (isApproved) {
            product.setStatus(ProductStatus.ACTIVE);
        } else {
            product.setStatus(ProductStatus.REJECTED);
        }
        productRepository.save(product);
        String action = isApproved ? LogAction.VERIFY_PRODUCT : LogAction.REJECT_PRODUCT;
        auditLogService.saveLog(action, productId.toString(),
                "Sản phẩm '" + product.getName() + "' đã được " + (isApproved ? "phê duyệt." : "từ chối."));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ProductResponse updateProductByAdmin(Long id, ProductUpdateRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessage.PRODUCT_NOT_FOUND + id));
        if (bidRepository.existsBidsByProductId(id)) {
            throw new DataConflictException("Admin không thể chỉnh sửa sản phẩm đang đấu giá hoặc đã bán.");
        }
        Product updatedProduct = updateProductCommonLogic(product, request);
        if (auditLogService != null) {
            auditLogService.saveLog(LogAction.UPDATE_PRODUCT, id.toString(), "Admin updated product info");
        }

        return productMapper.toProductResponse(updatedProduct);
    }

    //=======================PRIVATE METHOD=======================
    private Product updateProductCommonLogic(Product product, ProductUpdateRequest request) {
        // 1. Update Category (Nếu có thay đổi)
        if (request.getCategoryId() != null && !request.getCategoryId().equals(product.getCategory().getId())) {
            var newCategory = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException(ErrorMessage.CATEGORY_NOT_FOUND + request.getCategoryId()));
            product.setCategory(newCategory);
        }

        // 2. Map các field cơ bản (Tên, Mô tả, StartPrice...)
        // Mapper đã có @MappingTarget và ignore images/category -> An toàn
        productMapper.updateProduct(product, request);

        // 3. Xử lý ảnh (Thêm/Xóa)
        handleProductImages(product, request);

        // 4. Update timestamp
        // product.setUpdatedAt(LocalDateTime.now()); // Nếu entity có field này

        return productRepository.save(product);
    }

    private void handleProductImages(Product product, ProductUpdateRequest request) {
        // A. Xử lý xóa ảnh (Remove)
        if (request.getImageIdsToRemove() != null && !request.getImageIdsToRemove().isEmpty()) {
            Set<Image> imagesCurrentlyAssociated = product.getImages();

            // Lọc ra các ảnh thực sự thuộc về product này và cần xóa
            List<Image> imagesToRemove = imagesCurrentlyAssociated.stream()
                    .filter(img -> request.getImageIdsToRemove().contains(img.getId()))
                    .toList();

            if (!imagesToRemove.isEmpty()) {
                log.info("Removing {} images from product ID {}", imagesToRemove.size(), product.getId());

                // Quan trọng: Xóa khỏi Set của Product trước để Hibernate không hiểu lầm
                imagesToRemove.forEach(imagesCurrentlyAssociated::remove);

                // Xóa vật lý (Cloudinary + DB)
                for (Image img : imagesToRemove) {
                    try {
                        imageService.deleteImage(img.getId());
                    } catch (Exception e) {
                        log.error("Error deleting image ID {}: {}", img.getId(), e.getMessage());
                    }
                }
            }
        }
        // B. Xử lý thêm ảnh (Add)
        if (request.getImageIdsToAdd() != null && !request.getImageIdsToAdd().isEmpty()) {
            List<Image> imagesToAdd = imageRepository.findAllById(request.getImageIdsToAdd());

            if (!imagesToAdd.isEmpty()) {
                log.info("Adding {} new images to product ID {}", imagesToAdd.size(), product.getId());
                // Gán Product cho Image (nếu quan hệ 2 chiều) hoặc add vào Set
                // Vì CascadeType.ALL, việc add vào Set sẽ tự động update FK
                product.getImages().addAll(imagesToAdd);
            }
        }
    }
}
