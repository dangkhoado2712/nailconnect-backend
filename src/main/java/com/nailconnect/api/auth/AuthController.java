package com.nailconnect.api.auth;

import com.nailconnect.api.security.JwtService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;
import java.util.UUID;

@RestController @RequestMapping("/api/v1/auth")
public class AuthController {
  private final JdbcTemplate db; private final PasswordEncoder passwords; private final JwtService jwt;
  public AuthController(JdbcTemplate db,PasswordEncoder passwords,JwtService jwt){this.db=db;this.passwords=passwords;this.jwt=jwt;}
  @PostMapping("/register") @ResponseStatus(HttpStatus.CREATED)
  public AuthResponse register(@Valid @RequestBody RegisterRequest r){
    Integer count=db.queryForObject("select count(*) from users where lower(email)=lower(?)",Integer.class,r.email());if(count!=null&&count>0)throw new ResponseStatusException(HttpStatus.CONFLICT,"Email already registered");
    UUID id=UUID.randomUUID();db.update("insert into users(id,email,password_hash,role,display_name) values (?,?,?,cast(? as user_role),?)",id,r.email().toLowerCase(),passwords.encode(r.password()),r.role(),r.displayName());return new AuthResponse(jwt.issue(id,r.email(),r.role()),new UserView(id,r.email(),r.role(),r.displayName()));
  }
  @PostMapping("/login") public AuthResponse login(@Valid @RequestBody LoginRequest r){
    var rows=db.query("select id,email,password_hash,role::text,display_name from users where lower(email)=lower(?) and active=true",(rs,n)->Map.of("id",rs.getObject(1,UUID.class),"email",rs.getString(2),"hash",rs.getString(3),"role",rs.getString(4),"name",rs.getString(5)),r.email());if(rows.isEmpty()||!passwords.matches(r.password(),(String)rows.getFirst().get("hash")))throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Invalid credentials");var u=rows.getFirst();UUID id=(UUID)u.get("id");String email=(String)u.get("email"),role=(String)u.get("role"),name=(String)u.get("name");return new AuthResponse(jwt.issue(id,email,role),new UserView(id,email,role,name));
  }
  public record RegisterRequest(@Email @NotBlank String email,@Size(min=8,max=72) String password,@Pattern(regexp="TECHNICIAN|SALON_OWNER") String role,@NotBlank @Size(max=120) String displayName){}
  public record LoginRequest(@Email @NotBlank String email,@NotBlank String password){}
  public record AuthResponse(String accessToken,UserView user){}
  public record UserView(UUID id,String email,String role,String displayName){}
}
