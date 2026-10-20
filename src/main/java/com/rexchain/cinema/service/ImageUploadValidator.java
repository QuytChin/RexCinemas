package com.rexchain.cinema.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;

/** Validates untrusted image uploads before any external upload is attempted. */
@Service
public class ImageUploadValidator {
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private final long maxBytes;
    private final long maxPixels;
    private final int maxWidth;
    private final int maxHeight;

    public ImageUploadValidator(
            @Value("${app.upload.image.max-bytes:5242880}") long maxBytes,
            @Value("${app.upload.image.max-pixels:25000000}") long maxPixels,
            @Value("${app.upload.image.max-width:10000}") int maxWidth,
            @Value("${app.upload.image.max-height:10000}") int maxHeight) {
        this.maxBytes = maxBytes;
        this.maxPixels = maxPixels;
        this.maxWidth = maxWidth;
        this.maxHeight = maxHeight;
        ImageIO.scanForPlugins();
    }

    public ValidatedImage validate(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty() || file.getSize() <= 0) {
            throw new IllegalArgumentException("Hãy chọn một ảnh JPG, PNG hoặc WebP");
        }
        if (file.getSize() > maxBytes) {
            throw new IllegalArgumentException("Ảnh tối đa 5 MB");
        }

        String filename = requireSafeFilename(file.getOriginalFilename());
        byte[] bytes = file.getBytes();
        if (bytes.length == 0 || bytes.length > maxBytes) {
            throw new IllegalArgumentException("Ảnh tối đa 5 MB");
        }

        ImageFormat format = ImageFormat.detect(bytes);
        if (format == null) {
            throw new IllegalArgumentException("Tệp không phải là ảnh JPG, PNG hoặc WebP hợp lệ");
        }
        String declaredType = normalizeMimeType(file.getContentType());
        if (!format.mimeType.equals(declaredType)) {
            throw new IllegalArgumentException("Kiểu tệp khai báo không khớp với dữ liệu ảnh");
        }
        if (!format.acceptsFilename(filename)) {
            throw new IllegalArgumentException("Phần mở rộng tệp không khớp với dữ liệu ảnh");
        }

        decodeAndCheckBounds(bytes);
        return new ValidatedImage(bytes, format.mimeType);
    }

    private String requireSafeFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new IllegalArgumentException("Tên tệp ảnh không hợp lệ");
        }
        String filename = originalFilename.trim();
        if (filename.length() > 255 || filename.indexOf('\0') >= 0
                || filename.contains("/") || filename.contains("\\") || filename.contains("..")) {
            throw new IllegalArgumentException("Tên tệp ảnh không hợp lệ");
        }
        return filename;
    }

    private String normalizeMimeType(String contentType) {
        if (contentType == null) {
            throw new IllegalArgumentException("Kiểu tệp ảnh không hợp lệ");
        }
        String normalized = contentType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
        if (!ALLOWED_MIME_TYPES.contains(normalized)) {
            throw new IllegalArgumentException("Chỉ hỗ trợ ảnh JPG, PNG hoặc WebP");
        }
        return normalized;
    }

    private void decodeAndCheckBounds(byte[] bytes) throws IOException {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            if (input == null) {
                throw new IllegalArgumentException("Không thể đọc dữ liệu ảnh");
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw new IllegalArgumentException("Không thể giải mã dữ liệu ảnh");
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width < 1 || height < 1 || width > maxWidth || height > maxHeight
                        || ((long) width * height) > maxPixels) {
                    throw new IllegalArgumentException("Kích thước ảnh vượt quá giới hạn cho phép");
                }
                BufferedImage image = reader.read(0);
                if (image == null || image.getWidth() != width || image.getHeight() != height) {
                    throw new IllegalArgumentException("Không thể giải mã dữ liệu ảnh");
                }
            } finally {
                reader.dispose();
            }
        }
    }

    public record ValidatedImage(byte[] bytes, String contentType) { }

    private enum ImageFormat {
        JPEG("image/jpeg", Set.of("jpg", "jpeg")),
        PNG("image/png", Set.of("png")),
        WEBP("image/webp", Set.of("webp"));

        private final String mimeType;
        private final Set<String> extensions;

        ImageFormat(String mimeType, Set<String> extensions) {
            this.mimeType = mimeType;
            this.extensions = extensions;
        }

        static ImageFormat detect(byte[] bytes) {
            if (bytes.length >= 3 && (bytes[0] & 0xff) == 0xff && (bytes[1] & 0xff) == 0xd8 && (bytes[2] & 0xff) == 0xff) {
                return JPEG;
            }
            if (bytes.length >= 8 && (bytes[0] & 0xff) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4e && bytes[3] == 0x47
                    && bytes[4] == 0x0d && bytes[5] == 0x0a && bytes[6] == 0x1a && bytes[7] == 0x0a) {
                return PNG;
            }
            if (bytes.length >= 12 && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                    && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') {
                return WEBP;
            }
            return null;
        }

        boolean acceptsFilename(String filename) {
            int extensionStart = filename.lastIndexOf('.');
            return extensionStart > 0 && extensions.contains(filename.substring(extensionStart + 1).toLowerCase(Locale.ROOT));
        }
    }
}
