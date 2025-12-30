package com.thanh.auction_server.Controller;

import com.thanh.auction_server.entity.Image;
import com.thanh.auction_server.service.product.ImageService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RequiredArgsConstructor
@RestController
@RequestMapping("/images")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ImageController {
    ImageService imageService;

    @PostMapping("/upload")
        public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("Vui lòng chọn file để upload.");
        }
        try {
            Image savedImage = imageService.uploadAndSaveImage(file);
            return ResponseEntity.ok(savedImage);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Upload ảnh thất bại: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteImage(@PathVariable("id") Integer imageId) {
        try {
            imageService.deleteImage(imageId);
            return ResponseEntity.noContent().build();
        } catch (IOException e) {
            // Lỗi khi giao tiếp với Cloudinary
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (RuntimeException e) {
            // Lỗi không tìm thấy ảnh trong DB
            return ResponseEntity.notFound().build();
        }
    }
}
