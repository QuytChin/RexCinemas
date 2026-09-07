package com.rexchain.cinema.controller;
import com.rexchain.cinema.entity.User; import com.rexchain.cinema.repository.UserRepository; import com.rexchain.cinema.service.CloudinaryService; import org.springframework.security.core.Authentication; import org.springframework.security.crypto.password.PasswordEncoder; import org.springframework.stereotype.Controller; import org.springframework.ui.Model; import org.springframework.web.bind.annotation.*; import org.springframework.web.multipart.MultipartFile;
@Controller
public class ProfileController{
 private final UserRepository users; private final CloudinaryService cloudinary; private final PasswordEncoder encoder;
 public ProfileController(UserRepository users,CloudinaryService cloudinary,PasswordEncoder encoder){this.users=users;this.cloudinary=cloudinary;this.encoder=encoder;}
 @GetMapping("/profile") String profile(Authentication a,Model m){m.addAttribute("user",current(a));return "profile";}
 @PostMapping("/profile") String update(Authentication a,@RequestParam String fullName,@RequestParam(required=false)String phone,@RequestParam(required=false)String address,@RequestParam(required=false)String newPassword,@RequestParam(required=false)MultipartFile avatarFile,Model m){User u=current(a);try{u.setFullName(fullName.trim());u.setPhone(phone);u.setAddress(address);if(newPassword!=null&&!newPassword.isBlank()){if(newPassword.length()<6)throw new IllegalArgumentException("Mật khẩu mới phải có ít nhất 6 ký tự");u.setPassword(encoder.encode(newPassword));}if(avatarFile!=null&&!avatarFile.isEmpty())u.setAvatarUrl(cloudinary.uploadAvatar(avatarFile));users.save(u);return "redirect:/profile?success";}catch(Exception e){m.addAttribute("user",u);m.addAttribute("error",e.getMessage());return "profile";}}
 private User current(Authentication a){return users.findByEmailIgnoreCase(a.getName()).orElseThrow();}
}
