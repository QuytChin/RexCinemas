package com.rexchain.cinema.config;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/** Resolves legacy demo-art references without changing stored or user-uploaded URLs. */
@Component("cinemaArtwork")
public class CinemaArtwork {
    private static final Map<String, String> LEGACY_ARTWORK = Map.ofEntries(
            Map.entry("/images/poster-1.svg", "/images/movies/hanh-trinh-anh-sang.png"),
            Map.entry("/images/poster-2.svg", "/images/movies/mua-he-khong-ten.png"),
            Map.entry("/images/poster-3.svg", "/images/movies/ke-gac-dem.png"),
            Map.entry("/images/poster-4.svg", "/images/movies/nha-co-hai-nguoi.png"),
            Map.entry("/images/poster-5.svg", "/images/movies/thanh-pho-sau-mua.png"),
            Map.entry("/images/poster-6.svg", "/images/movies/mat-ma-dai-duong.png"),
            Map.entry("/images/event-member.svg", "/images/events/member-wednesday.png"),
            Map.entry("/images/event-combo.svg", "/images/events/weekend-combo.png"),
            Map.entry("/images/article-seat.svg", "/images/articles/chon-ghe-xem-phim.png"),
            Map.entry("/images/article-member.svg", "/images/articles/rex-member.png"),
            Map.entry("/images/article-realtime.svg", "/images/articles/booking-realtime.png"),
            Map.entry("/images/media-trailer.svg", "/images/media/trailer-tuyen-chon.png"),
            Map.entry("/images/media-clip.svg", "/images/media/trai-nghiem-rex.png")
    );

    private static final Set<String> DEMO_MOVIES = Set.of(
            "hanh-trinh-anh-sang", "mua-he-khong-ten", "ke-gac-dem",
            "nha-co-hai-nguoi", "thanh-pho-sau-mua", "mat-ma-dai-duong");

    private static final Set<String> CATEGORIES = Set.of(
            "movie", "article", "event", "combo", "media", "avatar");

    public String imageUrl(String storedUrl, String category) {
        String kind = CATEGORIES.contains(category) ? category : "movie";
        if (storedUrl == null || storedUrl.isBlank()) {
            return "/images/fallbacks/" + kind + ".jpg";
        }
        return LEGACY_ARTWORK.getOrDefault(storedUrl, storedUrl);
    }

    public String heroBackdrop(String posterUrl) {
        String resolvedPoster = imageUrl(posterUrl, "movie");
        for (String slug : DEMO_MOVIES) {
            if (resolvedPoster.equals("/images/movies/" + slug + ".png")) {
                return "/images/hero/" + slug + ".jpg";
            }
        }
        return resolvedPoster;
    }
}
