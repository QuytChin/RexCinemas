package com.rexchain.cinema.security;
import com.rexchain.cinema.entity.User; import com.rexchain.cinema.repository.UserRepository; import org.springframework.security.core.userdetails.*; import org.springframework.stereotype.Service;
@Service
public class CustomUserDetailsService implements UserDetailsService{
 private final UserRepository users; public CustomUserDetailsService(UserRepository users){this.users=users;}
 @Override public UserDetails loadUserByUsername(String email)throws UsernameNotFoundException{User u=users.findByEmailIgnoreCase(email).orElseThrow(()->new UsernameNotFoundException("Không tìm thấy tài khoản")); return org.springframework.security.core.userdetails.User.withUsername(u.getEmail()).password(u.getPassword()).roles(u.getRole().name()).disabled(!u.isEnabled()).build();}
}
