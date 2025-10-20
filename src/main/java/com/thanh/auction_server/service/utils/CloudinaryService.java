package com.thanh.auction_server.service.utils;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Service
public class CloudinaryService {
    Cloudinary cloudinary;

    public Map upload(MultipartFile file) throws IOException {

        Map params = ObjectUtils.asMap(
                "folder", "auction_web"
        );
        Map uploadResult = cloudinary.uploader().upload(file.getBytes(), params);
        log.info("File uploaded successfully to folder 'auction-product-images': {}", uploadResult.get("secure_url"));
        return uploadResult;
    }

    public Map delete(String publicId) throws IOException {
        log.warn("Deleting image with public_id '{}' from Cloudinary", publicId);
        Map deleteResult = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        log.info("Image deletion result: {}", deleteResult);
        return deleteResult;
    }
}
