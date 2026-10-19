package com.rexchain.cinema.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;

@Service
public class CloudinaryService {
    private final Cloudinary cloudinary;
    private final String cloudName;

    public CloudinaryService(Cloudinary cloudinary, @Value("${cloudinary.cloud-name:}") String cloudName) {
        this.cloudinary = cloudinary; this.cloudName = cloudName;
    }
    public boolean configured() { return cloudName != null && !cloudName.isBlank(); }
    public String uploadMoviePoster(MultipartFile file) throws IOException { return upload(file, "rex-chain/movies"); }
    public String uploadAvatar(MultipartFile file) throws IOException { validateAvatar(file); return upload(file, "rex-chain/avatars"); }
    public String uploadComboImage(MultipartFile file) throws IOException { return upload(file, "rex-chain/combos"); }
    public String uploadContentImage(MultipartFile file) throws IOException { return upload(file, "rex-chain/content"); }
    private String upload(MultipartFile file, String folder) throws IOException {
        if (!configured()) throw new IllegalStateException("Chưa cấu hình Cloudinary");
        Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap("folder", folder, "resource_type", "image"));
        return String.valueOf(result.get("secure_url"));
    }
    private void validateAvatar(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("Hãy chọn ảnh đại diện");
        if (file.getSize() > 5L * 1024 * 1024) throw new IllegalArgumentException("Ảnh đại diện tối đa 5 MB");
        String type = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        byte[] content = file.getBytes();
        boolean jpg = content.length >= 3 && (content[0] & 0xff) == 0xff && (content[1] & 0xff) == 0xd8 && (content[2] & 0xff) == 0xff;
        boolean png = content.length >= 8 && (content[0] & 0xff) == 0x89 && content[1] == 0x50 && content[2] == 0x4e && content[3] == 0x47;
        boolean webp = content.length >= 12 && content[0] == 'R' && content[1] == 'I' && content[2] == 'F' && content[3] == 'F' && content[8] == 'W' && content[9] == 'E' && content[10] == 'B' && content[11] == 'P';
        boolean acceptedType = type.equals("image/jpeg") || type.equals("image/png") || type.equals("image/webp");
        if (!acceptedType || !(jpg || png || webp)) throw new IllegalArgumentException("Chỉ hỗ trợ ảnh JPG, PNG hoặc WebP hợp lệ");
        if (!configured()) throw new IllegalStateException("Chưa cấu hình Cloudinary để tải ảnh đại diện");
    }
}
