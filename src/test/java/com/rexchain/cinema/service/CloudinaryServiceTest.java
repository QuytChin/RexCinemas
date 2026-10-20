package com.rexchain.cinema.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.*;

class CloudinaryServiceTest {
    private final ImageUploadValidator validator = new ImageUploadValidator(5 * 1024 * 1024, 25_000_000, 10_000, 10_000);

    @Test
    void returnsOnlyVerifiedSecureUrlAfterSuccessfulUpload() throws Exception {
        Cloudinary cloudinary = mock(Cloudinary.class);
        Uploader uploader = mock(Uploader.class);
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), anyMap()))
                .thenReturn(Map.of("secure_url", "https://res.cloudinary.com/test/image/upload/rex.jpg"));
        CloudinaryService service = new CloudinaryService(cloudinary, "test", "key", "secret", validator);

        String url = service.uploadMoviePoster(validPng());

        assertThat(url).startsWith("https://");
        verify(uploader).upload(any(byte[].class), anyMap());
    }

    @Test
    void cloudinaryFailureDoesNotReturnPlaceholderUrl() throws Exception {
        Cloudinary cloudinary = mock(Cloudinary.class);
        when(cloudinary.uploader()).thenThrow(new RuntimeException("offline"));
        CloudinaryService service = new CloudinaryService(cloudinary, "test", "key", "secret", validator);

        assertThatThrownBy(() -> service.uploadContentImage(validPng()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Không thể tải ảnh");
    }

    @Test
    void invalidImageNeverCallsCloudinary() {
        Cloudinary cloudinary = mock(Cloudinary.class);
        CloudinaryService service = new CloudinaryService(cloudinary, "test", "key", "secret", validator);
        MockMultipartFile invalid = new MockMultipartFile(
                "imageFile", "fake.png", "image/png", "not an image".getBytes());

        assertThatThrownBy(() -> service.uploadAvatar(invalid))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(cloudinary);
    }

    private MockMultipartFile validPng() throws Exception {
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        assertThat(ImageIO.write(image, "png", output)).isTrue();
        return new MockMultipartFile("imageFile", "poster.png", "image/png", output.toByteArray());
    }
}
