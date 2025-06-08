package com.recapmap.core.config;

import com.recapmap.core.CoreApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import java.util.UUID;

@EnableWebSecurity
@Configuration
public class SecurityConfig {

    // Generate UUID passwords for admin and user
    public static final String ADMIN_PASSWORD = UUID.randomUUID().toString();
    public static final String USER_PASSWORD = UUID.randomUUID().toString();

    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        UserDetails admin = User.withUsername("admin")
                .password(passwordEncoder.encode(ADMIN_PASSWORD))
                .roles("ADMIN")
                .build();
        UserDetails user = User.withUsername("user")
                .password(passwordEncoder.encode(USER_PASSWORD))
                .roles("USER")
                .build();
        return new InMemoryUserDetailsManager(admin, user);
    }    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        if (CoreApplication.BYPASS_LOGIN) {
            http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .csrf(csrf -> csrf.disable());
            return http.build();
        }
        
        http            
            .authorizeHttpRequests(auth -> auth
                // Public static resources
                .requestMatchers("/css/**", "/js/**", "/static/**", "/jquery-3.7.1.min.js").permitAll()
                
                // Admin frontend - publicly accessible (React handles auth)
                .requestMatchers("/admin", "/admin/**").permitAll()
                
                // API authentication endpoints
                .requestMatchers("/api/auth/login").permitAll()
                .requestMatchers("/api/auth/logout", "/api/auth/status").authenticated()
                
                // Protected API endpoints - require ADMIN role
                .requestMatchers("/api/docs/**").hasRole("ADMIN")
                
                // Spring Security default login (for non-admin access)
                .requestMatchers("/login").permitAll()
                
                // All other requests require authentication via Spring Security
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/", true)
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(org.springframework.security.config.http.SessionCreationPolicy.IF_REQUIRED)
                .maximumSessions(1)
                .maxSessionsPreventsLogin(false)
            )
            .csrf(csrf -> csrf.disable());
            
        return http.build();
    }
}
