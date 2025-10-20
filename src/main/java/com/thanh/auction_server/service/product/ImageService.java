package com.thanh.auction_server.service.product;

import com.thanh.auction_server.entity.Image;
import com.thanh.auction_server.exception.ResourceNotFoundException;
import com.thanh.auction_server.repository.ImageRepository;
import com.thanh.auction_server.service.utils.CloudinaryService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Service
public class ImageService {
    ImageRepository imageRepository;
    CloudinaryService cloudinaryService;

    @Transactional
    public Image uploadAndSaveImage(MultipartFile file) throws IOException {
        Map uploadResult = cloudinaryService.upload(file);
        String imageUrl = (String) uploadResult.get("secure_url");
        String publicId = (String) uploadResult.get("public_id");

        if (imageUrl == null || publicId == null) {
            throw new IOException("Không thể upload ảnh lên Cloudinary.");
        }
        Image image = Image.builder()
                .url(imageUrl)
                .publicId(publicId)
                .build();
        Image savedImage = imageRepository.save(image);
        log.info("Image saved to DB with ID: {} and Public ID: {}", savedImage.getId(), savedImage.getPublicId());
        return savedImage;
    }

    @Transactional
    public void deleteImage(Integer imageId) throws IOException {
        Optional<Image> imageOptional = imageRepository.findById(imageId);
        if (imageOptional.isEmpty()) {
            throw new ResourceNotFoundException("Không tìm thấy ảnh với ID: " + imageId);
        }
        Image image = imageOptional.get();
        String publicId = image.getPublicId();

        if (publicId != null && !publicId.isEmpty()) {
            try {
                cloudinaryService.delete(publicId);
            } catch (IOException e) {
                log.error("Error deleting image from Cloudinary with Public ID {}: {}", publicId, e.getMessage());
                throw e;
            }
        } else {
            throw new ResourceNotFoundException("Không tìm thấy Public ID của ảnh với ID: " + imageId);
        }
        imageRepository.delete(image);
        log.info("Image with DB ID {} deleted successfully from DB.", imageId);
    }
}
