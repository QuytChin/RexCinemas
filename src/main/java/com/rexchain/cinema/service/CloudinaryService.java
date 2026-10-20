package com.rexchain.cinema.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public class CloudinaryService {
    private final Cloudinary cloudinary;
    private final String cloudName;
    private final String apiKey;
    private final String apiSecret;
    private final ImageUploadValidator imageUploadValidator;

    public CloudinaryService(Cloudinary cloudinary, @Value("${cloudinary.cloud-name:}") String cloudName,
                             @Value("${cloudinary.api-key:}") String apiKey,
                             @Value("${cloudinary.api-secret:}") String apiSecret,
                             ImageUploadValidator imageUploadValidator) {
        this.cloudinary = cloudinary;
        this.cloudName = cloudName;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.imageUploadValidator = imageUploadValidator;
    }

    public boolean configured() {
        return cloudName != null && !cloudName.isBlank()
                && apiKey != null && !apiKey.isBlank()
                && apiSecret != null && !apiSecret.isBlank();
    }

    public String uploadMoviePoster(MultipartFile file) throws IOException {
        return upload(file, "rex-chain/movies");
    }

    public String uploadAvatar(MultipartFile file) throws IOException {
        return upload(file, "rex-chain/avatars");
    }

    public String uploadComboImage(MultipartFile file) throws IOException {
        return upload(file, "rex-chain/combos");
    }

    public String uploadContentImage(MultipartFile file) throws IOException {
        return upload(file, "rex-chain/content");
    }

    private String upload(MultipartFile file, String folder) throws IOException {
        ImageUploadValidator.ValidatedImage image;
        try {
            image = imageUploadValidator.validate(file);
        } catch (IOException ex) {
            throw new IllegalArgumentException("Không thể giải mã dữ liệu ảnh");
        }
        if (!configured()) {
            throw new IllegalStateException("Chưa cấu hình Cloudinary để tải ảnh");
        }
        try {
            Map<?, ?> result = cloudinary.uploader().upload(image.bytes(), ObjectUtils.asMap(
                    "folder", folder,
                    "resource_type", "image",
                    "use_filename", false,
                    "unique_filename", true,
                    "overwrite", false));
            Object value = result.get("secure_url");
            if (!(value instanceof String url) || url.isBlank() || !url.startsWith("https://")) {
                throw new IllegalStateException("Cloudinary không trả về URL ảnh hợp lệ");
            }
            return url;
        } catch (IllegalArgumentException | IllegalStateException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("Không thể tải ảnh lên Cloudinary. Vui lòng thử lại.", ex);
        }
    }
}
