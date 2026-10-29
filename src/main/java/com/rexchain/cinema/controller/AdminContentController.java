package com.rexchain.cinema.controller;

import com.rexchain.cinema.entity.*;
import com.rexchain.cinema.repository.*;
import com.rexchain.cinema.service.CloudinaryService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.net.URI;

@Controller
@RequestMapping("/admin")
public class AdminContentController {
    private final EventRepository events;
    private final ArticleRepository articles;
    private final MediaItemRepository mediaItems;
    private final CloudinaryService cloudinary;
    private final AdminListSupport lists;

    public AdminContentController(EventRepository events, ArticleRepository articles,
                                  MediaItemRepository mediaItems, CloudinaryService cloudinary, AdminListSupport lists) {
        this.events = events;
        this.articles = articles;
        this.mediaItems = mediaItems;
        this.cloudinary = cloudinary;
        this.lists = lists;
    }

    @GetMapping("/events")
    String events(@RequestParam(required = false) Long edit, @RequestParam Map<String, String> params,
                  HttpServletRequest request, Model model) {
        var options = new AdminListSupport.Options("title,label", "active", "sortOrder", "createdAt", "title", "id")
                .withLabels("Hiển thị", "Tên hoặc nhãn sự kiện");
        var page = lists.query(events, params, options);
        model.addAttribute("events", page.getContent());
        lists.present(model, page, params, options, request, "true", "false");
        model.addAttribute("editEvent", edit == null ? new Event() : events.findById(edit).orElse(new Event()));
        return "admin/events";
    }

    @PostMapping("/events/save")
    String saveEvent(@RequestParam(required = false) Long id,
                     @RequestParam String title,
                     @RequestParam(required = false) String label,
                     @RequestParam(required = false) String description,
                     @RequestParam(required = false) String imageUrl,
                     @RequestParam(required = false) String linkUrl,
                     @RequestParam(defaultValue = "0") int sortOrder,
                     @RequestParam(required = false) MultipartFile imageFile) throws Exception {
        title = requiredText(title, "Tiêu đề sự kiện", 180);
        label = optionalText(label, "Nhãn sự kiện", 80);
        imageUrl = safeUrl(imageUrl, "Ảnh sự kiện", 600, true);
        linkUrl = safeUrl(linkUrl, "Liên kết sự kiện", 600, true);
        Event event = id == null ? new Event() : events.findById(id).orElseThrow();
        event.setTitle(title);
        event.setLabel(label);
        event.setDescription(description);
        event.setLinkUrl(linkUrl);
        event.setSortOrder(sortOrder);
        if (imageFile != null && !imageFile.isEmpty()) event.setImageUrl(cloudinary.uploadContentImage(imageFile));
        else if (imageUrl != null && !imageUrl.isBlank()) event.setImageUrl(imageUrl);
        events.save(event);
        return "redirect:/admin/events";
    }

    @PostMapping("/events/{id}/toggle")
    String toggleEvent(@PathVariable Long id) {
        Event event = events.findById(id).orElseThrow();
        event.setActive(!event.isActive());
        events.save(event);
        return "redirect:/admin/events";
    }

    @PostMapping("/events/{id}/delete")
    String deleteEvent(@PathVariable Long id) {
        events.deleteById(id);
        return "redirect:/admin/events";
    }

    @GetMapping("/articles")
    String articles(@RequestParam(required = false) Long edit, @RequestParam Map<String, String> params,
                    HttpServletRequest request, Model model) {
        var options = new AdminListSupport.Options("title,category", "active", "createdAt", "publishDate", "title", "id")
                .withLabels("Hiển thị", "Tiêu đề hoặc chuyên mục");
        var page = lists.query(articles, params, options);
        model.addAttribute("articles", page.getContent());
        lists.present(model, page, params, options, request, "true", "false");
        model.addAttribute("editArticle", edit == null ? new Article() : articles.findById(edit).orElse(new Article()));
        return "admin/articles";
    }

    @PostMapping("/articles/save")
    String saveArticle(@RequestParam(required = false) Long id,
                       @RequestParam String title,
                       @RequestParam(required = false) String category,
                       @RequestParam(required = false) String summary,
                       @RequestParam(required = false) String content,
                       @RequestParam(required = false) String imageUrl,
                       @RequestParam(required = false) String linkUrl,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate publishDate,
                       @RequestParam(defaultValue = "0") int sortOrder,
                       @RequestParam(required = false) MultipartFile imageFile) throws Exception {
        title = requiredText(title, "Tiêu đề bài viết", 220);
        category = optionalText(category, "Chuyên mục", 80);
        summary = optionalText(summary, "Tóm tắt", 500);
        imageUrl = safeUrl(imageUrl, "Ảnh bài viết", 600, true);
        linkUrl = safeUrl(linkUrl, "Liên kết bài viết", 600, true);
        Article article = id == null ? new Article() : articles.findById(id).orElseThrow();
        article.setTitle(title);
        article.setCategory(category);
        article.setSummary(summary);
        article.setContent(content);
        article.setLinkUrl(linkUrl);
        article.setPublishDate(publishDate == null ? LocalDate.now() : publishDate);
        article.setSortOrder(sortOrder);
        if (imageFile != null && !imageFile.isEmpty()) article.setImageUrl(cloudinary.uploadContentImage(imageFile));
        else if (imageUrl != null && !imageUrl.isBlank()) article.setImageUrl(imageUrl);
        articles.save(article);
        return "redirect:/admin/articles";
    }

    @PostMapping("/articles/{id}/toggle")
    String toggleArticle(@PathVariable Long id) {
        Article article = articles.findById(id).orElseThrow();
        article.setActive(!article.isActive());
        articles.save(article);
        return "redirect:/admin/articles";
    }

    @PostMapping("/articles/{id}/delete")
    String deleteArticle(@PathVariable Long id) {
        articles.deleteById(id);
        return "redirect:/admin/articles";
    }

    @GetMapping("/media")
    String media(@RequestParam(required = false) Long edit, @RequestParam Map<String, String> params,
                 HttpServletRequest request, Model model) {
        var options = new AdminListSupport.Options("title", "type", "sortOrder", "createdAt", "title", "id")
                .withLabels("Loại nội dung", "Tên video hoặc clip");
        var page = lists.query(mediaItems, params, options);
        model.addAttribute("mediaItems", page.getContent());
        lists.present(model, page, params, options, request, MediaType.values()[0].name(), MediaType.values()[1].name());
        model.addAttribute("mediaTypes", MediaType.values());
        model.addAttribute("editMedia", edit == null ? new MediaItem() : mediaItems.findById(edit).orElse(new MediaItem()));
        return "admin/media";
    }

    @PostMapping("/media/save")
    String saveMedia(@RequestParam(required = false) Long id,
                     @RequestParam String title,
                     @RequestParam MediaType type,
                     @RequestParam String videoUrl,
                     @RequestParam(required = false) String thumbnailUrl,
                     @RequestParam(defaultValue = "0") int sortOrder,
                     @RequestParam(required = false) MultipartFile imageFile) throws Exception {
        title = requiredText(title, "Tiêu đề media", 180);
        videoUrl = requiredText(videoUrl, "Đường dẫn video", 800);
        videoUrl = safeUrl(videoUrl, "Đường dẫn video", 800, false);
        thumbnailUrl = safeUrl(thumbnailUrl, "Ảnh thu nhỏ", 600, true);
        MediaItem media = id == null ? new MediaItem() : mediaItems.findById(id).orElseThrow();
        media.setTitle(title);
        media.setType(type);
        media.setVideoUrl(videoUrl);
        media.setSortOrder(sortOrder);
        if (imageFile != null && !imageFile.isEmpty()) media.setThumbnailUrl(cloudinary.uploadContentImage(imageFile));
        else if (thumbnailUrl != null && !thumbnailUrl.isBlank()) media.setThumbnailUrl(thumbnailUrl);
        mediaItems.save(media);
        return "redirect:/admin/media";
    }

    @PostMapping("/media/{id}/toggle")
    String toggleMedia(@PathVariable Long id) {
        MediaItem media = mediaItems.findById(id).orElseThrow();
        media.setActive(!media.isActive());
        mediaItems.save(media);
        return "redirect:/admin/media";
    }

    @PostMapping("/media/{id}/delete")
    String deleteMedia(@PathVariable Long id) {
        mediaItems.deleteById(id);
        return "redirect:/admin/media";
    }

    @ExceptionHandler({IllegalArgumentException.class, DataIntegrityViolationException.class})
    String invalidContent(Exception ex, HttpServletRequest request, RedirectAttributes redirect) {
        String path = request.getRequestURI();
        String page = path.startsWith("/admin/events") ? "/admin/events"
                : path.startsWith("/admin/articles") ? "/admin/articles" : "/admin/media";
        redirect.addFlashAttribute("error", ex instanceof DataIntegrityViolationException
                ? "Dữ liệu không hợp lệ hoặc trùng với nội dung đã có" : ex.getMessage());
        return "redirect:" + page;
    }

    private static String requiredText(String value, String label, int maxLength) {
        String text = optionalText(value, label, maxLength);
        if (text == null) throw new IllegalArgumentException(label + " không được để trống");
        return text;
    }

    private static String optionalText(String value, String label, int maxLength) {
        if (value == null || value.isBlank()) return null;
        String trimmed = value.trim();
        if (trimmed.length() > maxLength)
            throw new IllegalArgumentException(label + " không được vượt quá " + maxLength + " ký tự");
        return trimmed;
    }

    private static String safeUrl(String value, String label, int maxLength, boolean allowRelative) {
        String normalized = optionalText(value, label, maxLength);
        if (normalized == null) return null;
        if (allowRelative && normalized.startsWith("/") && !normalized.startsWith("//")) return normalized;
        try {
            URI uri = URI.create(normalized);
            if (("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    && uri.getHost() != null && !uri.getHost().isBlank()) return normalized;
        } catch (IllegalArgumentException invalid) {
            // The user gets the same safe, localized validation message below.
        }
        throw new IllegalArgumentException(label + " phải là URL http(s) hợp lệ"
                + (allowRelative ? " hoặc đường dẫn nội bộ bắt đầu bằng /" : ""));
    }
}
