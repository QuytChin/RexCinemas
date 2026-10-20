package com.rexchain.cinema.config;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Resolves legacy artwork references and requests optimized image delivery. */
@Component("cinemaArtwork")
public class CinemaArtwork {
    private static final Map<String, String> LEGACY_ARTWORK = Map.ofEntries(
            Map.entry("/images/poster-1.svg", "/images/movies/hanh-trinh-anh-sang.webp"),
            Map.entry("/images/poster-2.svg", "/images/movies/mua-he-khong-ten.webp"),
            Map.entry("/images/poster-3.svg", "/images/movies/ke-gac-dem.webp"),
            Map.entry("/images/poster-4.svg", "/images/movies/nha-co-hai-nguoi.webp"),
            Map.entry("/images/poster-5.svg", "/images/movies/thanh-pho-sau-mua.webp"),
            Map.entry("/images/poster-6.svg", "/images/movies/mat-ma-dai-duong.webp"),
            Map.entry("/images/event-member.svg", "/images/events/member-wednesday.webp"),
            Map.entry("/images/event-combo.svg", "/images/events/weekend-combo.webp"),
            Map.entry("/images/article-seat.svg", "/images/articles/chon-ghe-xem-phim.webp"),
            Map.entry("/images/article-member.svg", "/images/articles/rex-member.webp"),
            Map.entry("/images/article-realtime.svg", "/images/articles/booking-realtime.webp"),
            Map.entry("/images/media-trailer.svg", "/images/media/trailer-tuyen-chon.webp"),
            Map.entry("/images/media-clip.svg", "/images/media/trai-nghiem-rex.webp")
    );

    private static final Set<String> DEMO_MOVIES = Set.of(
            "hanh-trinh-anh-sang", "mua-he-khong-ten", "ke-gac-dem",
            "nha-co-hai-nguoi", "thanh-pho-sau-mua", "mat-ma-dai-duong");

    private static final Set<String> CATEGORIES = Set.of(
            "movie", "article", "event", "combo", "media", "avatar");

    public String imageUrl(String storedUrl, String category) {
        String kind = CATEGORIES.contains(category) ? category : "movie";
        if (storedUrl == null || storedUrl.isBlank()) {
            return "/images/fallbacks/" + kind + ".webp";
        }
        String resolved = LEGACY_ARTWORK.getOrDefault(storedUrl, storedUrl);
        if (resolved.startsWith("/images/")) {
            return resolved.replaceAll("(?i)\\.(?:png|jpe?g)$", ".webp");
        }
        String cloudinaryMarker = "/image/upload/";
        if (resolved.startsWith("https://res.cloudinary.com/") && resolved.contains(cloudinaryMarker)
                && !resolved.toLowerCase(Locale.ROOT).contains("f_webp")) {
            return resolved.replace(cloudinaryMarker, cloudinaryMarker + "f_webp,q_auto/");
        }
        return resolved;
    }

    public String heroBackdrop(String posterUrl) {
        String resolvedPoster = imageUrl(posterUrl, "movie");
        for (String slug : DEMO_MOVIES) {
            if (resolvedPoster.equals("/images/movies/" + slug + ".webp")) {
                return "/images/hero/" + slug + ".webp";
            }
        }
        return resolvedPoster;
    }
}
