package com.nailconnect.api.config;

import com.nailconnect.api.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.List;

@Configuration @EnableMethodSecurity
public class SecurityConfig {
  @Bean PasswordEncoder passwordEncoder(){return new BCryptPasswordEncoder();}
  @Bean SecurityFilterChain filterChain(HttpSecurity http,JwtAuthenticationFilter jwt,@Value("${app.frontend-origin}") String origin)throws Exception{
    var cors=new CorsConfiguration();cors.setAllowedOrigins(List.of(origin));cors.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));cors.setAllowedHeaders(List.of("Authorization","Content-Type"));cors.setAllowCredentials(true);var source=new UrlBasedCorsConfigurationSource();source.registerCorsConfiguration("/**",cors);
    return http.csrf(c->c.disable()).cors(c->c.configurationSource(source)).sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS)).authorizeHttpRequests(a->a.requestMatchers("/api/v1/auth/**","/actuator/health").permitAll().requestMatchers(HttpMethod.GET,"/api/v1/jobs/**").permitAll().anyRequest().authenticated()).addFilterBefore(jwt,UsernamePasswordAuthenticationFilter.class).build();
  }
}
