package com.rexchain.cinema.controller;

import com.rexchain.cinema.entity.User;
import com.rexchain.cinema.repository.UserRepository;
import com.rexchain.cinema.service.CloudinaryService;
import com.rexchain.cinema.service.ProfileService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class ProfileController {
    private final UserRepository users;
    private final ProfileService profiles;
    private final CloudinaryService cloudinary;

    public ProfileController(UserRepository users, ProfileService profiles, CloudinaryService cloudinary) {
        this.users = users; this.profiles = profiles; this.cloudinary = cloudinary;
    }

    @GetMapping("/profile")
    String profile(Authentication authentication, Model model) {
        model.addAttribute("user", current(authentication));
        model.addAttribute("cloudinaryConfigured", cloudinary.configured());
        return "profile";
    }

    @PostMapping("/profile")
    String update(Authentication authentication, @RequestParam String fullName, @RequestParam(required = false) String phone,
                  @RequestParam(required = false) String address) {
        profiles.update(current(authentication), fullName, phone, address);
        return "redirect:/profile?profileUpdated";
    }

    @PostMapping("/profile/avatar")
    String avatar(Authentication authentication, @RequestParam("avatarFile") MultipartFile avatarFile) throws Exception {
        User user = current(authentication);
        user.setAvatarUrl(cloudinary.uploadAvatar(avatarFile));
        users.save(user);
        return "redirect:/profile?avatarUpdated";
    }

    @PostMapping("/profile/password")
    String password(Authentication authentication, @RequestParam String currentPassword, @RequestParam String newPassword,
                    @RequestParam String confirmPassword, Model model) {
        try {
            profiles.changePassword(current(authentication), currentPassword, newPassword, confirmPassword);
            return "redirect:/profile?passwordUpdated";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("user", current(authentication));
            model.addAttribute("cloudinaryConfigured", cloudinary.configured());
            model.addAttribute("passwordError", ex.getMessage());
            return "profile";
        }
    }

    private User current(Authentication authentication) {
        if (authentication == null) throw new IllegalStateException("Bạn cần đăng nhập");
        return users.findByEmailIgnoreCase(authentication.getName()).orElseThrow();
    }
}
