package com.rexchain.cinema.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CinemaArtworkTest {
    private final CinemaArtwork artwork = new CinemaArtwork();

    @Test
    void convertsLegacyLocalRasterUrlsAndUsesWebpFallbacks() {
        assertEquals("/images/movies/ke-gac-dem.webp",
                artwork.imageUrl("/images/movies/ke-gac-dem.png", "movie"));
        assertEquals("/images/fallbacks/avatar.webp", artwork.imageUrl(null, "avatar"));
    }

    @Test
    void resolvesLegacyArtworkAndHeroBackdropToWebp() {
        assertEquals("/images/articles/chon-ghe-xem-phim.webp",
                artwork.imageUrl("/images/article-seat.svg", "article"));
        assertEquals("/images/hero/ke-gac-dem.webp", artwork.heroBackdrop("/images/movies/ke-gac-dem.png"));
    }

    @Test
    void requestsWebpDeliveryForCloudinaryWithoutChangingStoredUrl() {
        assertEquals("https://res.cloudinary.com/rex/image/upload/f_webp,q_auto/v1/avatar.jpg",
                artwork.imageUrl("https://res.cloudinary.com/rex/image/upload/v1/avatar.jpg", "avatar"));
        assertEquals("https://res.cloudinary.com/rex/image/upload/f_webp,q_auto/v1/avatar.jpg",
                artwork.imageUrl("https://res.cloudinary.com/rex/image/upload/f_webp,q_auto/v1/avatar.jpg", "avatar"));
    }
}
