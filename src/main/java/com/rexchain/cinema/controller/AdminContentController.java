package com.rexchain.cinema.controller;

import com.rexchain.cinema.entity.*;
import com.rexchain.cinema.repository.*;
import com.rexchain.cinema.service.CloudinaryService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

@Controller
@RequestMapping("/admin")
public class AdminContentController {
    private final EventRepository events;
    private final ArticleRepository articles;
    private final MediaItemRepository mediaItems;
    private final CloudinaryService cloudinary;

    public AdminContentController(EventRepository events, ArticleRepository articles,
                                  MediaItemRepository mediaItems, CloudinaryService cloudinary) {
        this.events = events;
        this.articles = articles;
        this.mediaItems = mediaItems;
        this.cloudinary = cloudinary;
    }

    @GetMapping("/events")
    String events(@RequestParam(required = false) Long edit, Model model) {
        model.addAttribute("events", events.findAll());
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
    String articles(@RequestParam(required = false) Long edit, Model model) {
        model.addAttribute("articles", articles.findAll());
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
    String media(@RequestParam(required = false) Long edit, Model model) {
        model.addAttribute("mediaItems", mediaItems.findAll());
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
}
