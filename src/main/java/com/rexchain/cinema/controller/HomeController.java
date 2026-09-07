package com.rexchain.cinema.controller;

import com.rexchain.cinema.entity.*;
import com.rexchain.cinema.repository.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.*;
import java.util.*;

@Controller
public class HomeController {
    private final MovieRepository movies;
    private final ShowtimeRepository showtimes;
    private final ReviewRepository reviews;
    private final UserRepository users;
    private final CinemaRepository cinemas;
    private final EventRepository events;
    private final ArticleRepository articles;
    private final MediaItemRepository mediaItems;
    private final ShowtimeSeatRepository showtimeSeats;

    public HomeController(MovieRepository movies, ShowtimeRepository showtimes, ReviewRepository reviews,
                          UserRepository users, CinemaRepository cinemas, EventRepository events,
                          ArticleRepository articles, MediaItemRepository mediaItems, ShowtimeSeatRepository showtimeSeats) {
        this.movies = movies;
        this.showtimes = showtimes;
        this.reviews = reviews;
        this.users = users;
        this.cinemas = cinemas;
        this.events = events;
        this.articles = articles;
        this.mediaItems = mediaItems;
        this.showtimeSeats = showtimeSeats;
    }

    @GetMapping("/")
    String home(Model model) {
        LocalDate today = LocalDate.now();
        List<Movie> nowShowing = movies.findByActiveTrueAndReleaseDateLessThanEqualOrderByReleaseDateDesc(today);
        List<Movie> comingSoon = movies.findByActiveTrueAndReleaseDateGreaterThanOrderByReleaseDateAsc(today);
        List<Movie> heroMovies = nowShowing.stream().limit(4).toList();

        model.addAttribute("heroMovies", heroMovies);
        model.addAttribute("nowShowing", nowShowing);
        model.addAttribute("comingSoon", comingSoon);
        model.addAttribute("cinemas", cinemas.findByActiveTrueOrderByCityAscNameAsc());
        model.addAttribute("events", events.findByActiveTrueOrderBySortOrderAscCreatedAtDesc().stream().limit(4).toList());
        model.addAttribute("articles", articles.findByActiveTrueOrderBySortOrderAscPublishDateDesc().stream().limit(6).toList());
        model.addAttribute("mediaItems", mediaItems.findByActiveTrueOrderBySortOrderAscCreatedAtDesc().stream().limit(8).toList());
        List<Showtime> quickShowtimes = showtimes.findByActiveTrueAndStartTimeAfterOrderByStartTimeAsc(LocalDateTime.now())
                .stream().filter(s -> s.getMovie().isActive() && s.getAuditorium().getCinema().isActive()).limit(8).toList();
        model.addAttribute("quickShowtimes", quickShowtimes);
        populateAvailability(model, quickShowtimes);
        model.addAttribute("dates", dateOptions(today, 7));
        return "index";
    }

    @GetMapping("/movies")
    String movieCatalog(@RequestParam(required = false, defaultValue = "") String q,
                        @RequestParam(required = false, defaultValue = "all") String status,
                        Model model) {
        LocalDate today = LocalDate.now();
        List<Movie> result;
        if (q != null && !q.isBlank()) result = movies.search(q.trim());
        else if ("coming".equalsIgnoreCase(status)) result = movies.findByActiveTrueAndReleaseDateGreaterThanOrderByReleaseDateAsc(today);
        else if ("now".equalsIgnoreCase(status)) result = movies.findByActiveTrueAndReleaseDateLessThanEqualOrderByReleaseDateDesc(today);
        else result = movies.findByActiveTrueOrderByReleaseDateDesc();
        model.addAttribute("movies", result);
        model.addAttribute("q", q);
        model.addAttribute("status", status);
        return "movies";
    }

    @GetMapping("/schedule")
    String schedule(@RequestParam(required = false) Long cinemaId,
                    @RequestParam(required = false) Long movieId,
                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                    Model model) {
        LocalDate selectedDate = date == null ? LocalDate.now() : date;
        LocalDateTime fromTime = selectedDate.equals(LocalDate.now()) ? LocalDateTime.now() : selectedDate.atStartOfDay();
        List<Showtime> list = showtimes.findSchedule(fromTime, selectedDate.plusDays(1).atStartOfDay(), cinemaId, movieId);

        Map<Cinema, Map<Movie, List<Showtime>>> schedule = new LinkedHashMap<>();
        for (Showtime showtime : list) {
            Cinema cinema = showtime.getAuditorium().getCinema();
            schedule.computeIfAbsent(cinema, k -> new LinkedHashMap<>())
                    .computeIfAbsent(showtime.getMovie(), k -> new ArrayList<>())
                    .add(showtime);
        }

        model.addAttribute("schedule", schedule);
        model.addAttribute("cinemas", cinemas.findByActiveTrueOrderByCityAscNameAsc());
        model.addAttribute("movies", movies.findByActiveTrueOrderByReleaseDateDesc());
        model.addAttribute("selectedCinemaId", cinemaId);
        model.addAttribute("selectedMovieId", movieId);
        model.addAttribute("selectedDate", selectedDate);
        model.addAttribute("dates", dateOptions(LocalDate.now(), 7));
        populateAvailability(model, list);
        return "schedule";
    }

    @GetMapping("/movies/{id}")
    String detail(@PathVariable Long id, Model model, Authentication auth) {
        Movie movie = movies.findById(id).orElseThrow();
        List<Showtime> upcoming = showtimes.findByMovieIdAndActiveTrueAndStartTimeAfterOrderByStartTimeAsc(id, LocalDateTime.now());
        Map<Cinema, List<Showtime>> byCinema = new LinkedHashMap<>();
        for (Showtime showtime : upcoming) {
            byCinema.computeIfAbsent(showtime.getAuditorium().getCinema(), k -> new ArrayList<>()).add(showtime);
        }

        model.addAttribute("movie", movie);
        model.addAttribute("showtimesByCinema", byCinema);
        model.addAttribute("reviews", reviews.findByMovieIdOrderByCreatedAtDesc(id));
        model.addAttribute("averageRating", reviews.averageRating(id));
        model.addAttribute("reviewCount", reviews.countByMovieId(id));
        populateAvailability(model, upcoming);
        if (auth != null) {
            User user = users.findByEmailIgnoreCase(auth.getName()).orElse(null);
            if (user != null) model.addAttribute("myReview", reviews.findByMovieIdAndUserId(id, user.getId()).orElse(null));
        }
        return "movie-detail";
    }

    @GetMapping("/articles/{id}")
    String article(@PathVariable Long id, Model model) {
        Article article = articles.findById(id).orElseThrow();
        if (!article.isActive()) throw new IllegalStateException("Bài viết đang ẩn");
        model.addAttribute("article", article);
        model.addAttribute("latestArticles", articles.findByActiveTrueOrderBySortOrderAscPublishDateDesc().stream()
                .filter(a -> !a.getId().equals(id)).limit(4).toList());
        return "article-detail";
    }

    private void populateAvailability(Model model, List<Showtime> list) {
        Map<Long, Long> availableSeats = new HashMap<>();
        Map<Long, Long> totalSeats = new HashMap<>();
        for (Showtime showtime : list) {
            availableSeats.put(showtime.getId(), showtimeSeats.countByShowtimeIdAndStatus(showtime.getId(), SeatStatus.AVAILABLE));
            totalSeats.put(showtime.getId(), showtimeSeats.countByShowtimeId(showtime.getId()));
        }
        model.addAttribute("availableSeats", availableSeats);
        model.addAttribute("totalSeats", totalSeats);
    }

    private List<LocalDate> dateOptions(LocalDate start, int days) {
        List<LocalDate> list = new ArrayList<>();
        for (int i = 0; i < days; i++) list.add(start.plusDays(i));
        return list;
    }
}
