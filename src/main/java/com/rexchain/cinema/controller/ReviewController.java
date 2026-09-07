package com.rexchain.cinema.controller;
import com.rexchain.cinema.entity.*; import com.rexchain.cinema.repository.*; import org.springframework.security.core.Authentication; import org.springframework.stereotype.Controller; import org.springframework.web.bind.annotation.*; import java.time.LocalDateTime;
@Controller
public class ReviewController{
 private final ReviewRepository reviews; private final MovieRepository movies; private final UserRepository users;
 public ReviewController(ReviewRepository r,MovieRepository m,UserRepository u){reviews=r;movies=m;users=u;}
 @PostMapping("/movies/{movieId}/reviews") String save(@PathVariable Long movieId,@RequestParam int rating,@RequestParam String comment,Authentication a){if(rating<1||rating>5)throw new IllegalArgumentException("Điểm đánh giá phải từ 1 đến 5");if(comment==null||comment.isBlank())throw new IllegalArgumentException("Vui lòng nhập nhận xét");User user=users.findByEmailIgnoreCase(a.getName()).orElseThrow();Review r=reviews.findByMovieIdAndUserId(movieId,user.getId()).orElseGet(Review::new);if(r.getId()==null){r.setUser(user);r.setMovie(movies.findById(movieId).orElseThrow());}else r.setUpdatedAt(LocalDateTime.now());r.setRating(rating);r.setComment(comment.trim());reviews.save(r);return "redirect:/movies/"+movieId+"?reviewed";}
}
