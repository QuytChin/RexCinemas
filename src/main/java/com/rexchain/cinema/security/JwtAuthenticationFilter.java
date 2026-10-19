package com.rexchain.cinema.security;
import com.rexchain.cinema.entity.User;
import com.rexchain.cinema.repository.UserRepository;
import com.rexchain.cinema.service.AccessTokenRevocationService;
import jakarta.servlet.*; import jakarta.servlet.http.*; import org.springframework.security.authentication.UsernamePasswordAuthenticationToken; import org.springframework.security.core.context.SecurityContextHolder; import org.springframework.security.core.userdetails.UserDetails; import org.springframework.stereotype.Component; import org.springframework.web.filter.OncePerRequestFilter; import java.io.IOException; import java.util.Arrays;
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter{
 private final JwtService jwt; private final CustomUserDetailsService uds; private final UserRepository users; private final AccessTokenRevocationService revocations;
 public JwtAuthenticationFilter(JwtService jwt,CustomUserDetailsService uds,UserRepository users,AccessTokenRevocationService revocations){this.jwt=jwt;this.uds=uds;this.users=users;this.revocations=revocations;}
 @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain)throws ServletException,IOException{
  String token=null; String auth=req.getHeader("Authorization"); if(auth!=null&&auth.startsWith("Bearer ")) token=auth.substring(7);
  if(token==null&&req.getCookies()!=null) token=Arrays.stream(req.getCookies()).filter(c->"access_token".equals(c.getName())).map(Cookie::getValue).findFirst().orElse(null);
  if(token!=null&&SecurityContextHolder.getContext().getAuthentication()==null&&jwt.valid(token)&&!revocations.isRevoked(token)){
   String email=jwt.extractEmail(token);
   User user=users.findByEmailIgnoreCase(email).orElse(null);
   if(user!=null&&user.isEnabled()&&user.getCredentialVersion()==jwt.extractCredentialVersion(token)){
    UserDetails ud=uds.loadUserByUsername(email);
    if(ud.isEnabled()&&ud.isAccountNonExpired()&&ud.isAccountNonLocked()&&ud.isCredentialsNonExpired()) SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(ud,null,ud.getAuthorities()));
   }
  }
  chain.doFilter(req,res);
 }
}
