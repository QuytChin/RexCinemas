package com.rexchain.cinema.service;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImageUploadValidatorTest {
    private final ImageUploadValidator validator = new ImageUploadValidator(5 * 1024 * 1024, 25_000_000, 10_000, 10_000);

    @Test
    void acceptsDecodedJpegPngAndWebpWithMatchingMetadata() throws Exception {
        ImageUploadValidator.ValidatedImage jpeg = validator.validate(image("poster.jpg", "image/jpeg", "jpg"));
        ImageUploadValidator.ValidatedImage png = validator.validate(image("poster.png", "image/png", "png"));
        ImageUploadValidator.ValidatedImage webp = validator.validate(new MockMultipartFile(
                "imageFile", "poster.webp", "image/webp", Base64.getDecoder().decode(
                "UklGRiIAAABXRUJQVlA4IBYAAAAwAQCdASoBAAEADsD+JaQAA3AAAAAA")));

        assertThat(jpeg.contentType()).isEqualTo("image/jpeg");
        assertThat(png.contentType()).isEqualTo("image/png");
        assertThat(webp.contentType()).isEqualTo("image/webp");
    }

    @Test
    void rejectsFakeExtensionMimeAndNonImageBytes() throws Exception {
        MockMultipartFile pngWithJpegExtension = new MockMultipartFile(
                "imageFile", "poster.jpg", "image/png", imageBytes("png"));
        MockMultipartFile pngWithFakeMime = new MockMultipartFile(
                "imageFile", "poster.png", "image/jpeg", imageBytes("png"));
        MockMultipartFile fakeImage = new MockMultipartFile(
                "imageFile", "poster.png", "image/png", "not an image".getBytes());

        assertThatThrownBy(() -> validator.validate(pngWithJpegExtension))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Phần mở rộng");
        assertThatThrownBy(() -> validator.validate(pngWithFakeMime))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Kiểu tệp");
        assertThatThrownBy(() -> validator.validate(fakeImage))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("không phải");
    }

    @Test
    void rejectsEmptyOversizedAndDangerousFilenames() throws Exception {
        MockMultipartFile empty = new MockMultipartFile("imageFile", "empty.png", "image/png", new byte[0]);
        MockMultipartFile oversized = new MockMultipartFile(
                "imageFile", "large.png", "image/png", new byte[5 * 1024 * 1024 + 1]);
        MockMultipartFile traversal = new MockMultipartFile(
                "imageFile", "../poster.png", "image/png", imageBytes("png"));

        assertThatThrownBy(() -> validator.validate(empty)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> validator.validate(oversized))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("5 MB");
        assertThatThrownBy(() -> validator.validate(traversal))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Tên tệp");
    }

    private MockMultipartFile image(String filename, String contentType, String format) throws Exception {
        return new MockMultipartFile("imageFile", filename, contentType, imageBytes(format));
    }

    private byte[] imageBytes(String format) throws Exception {
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        image.setRGB(0, 0, 0xff336699);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        assertThat(ImageIO.write(image, format, output)).isTrue();
        return output.toByteArray();
    }
}
