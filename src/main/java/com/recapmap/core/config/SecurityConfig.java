package com.recapmap.core.config;

import com.recapmap.core.CoreApplication;
import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.util.StringUtils;

import java.util.UUID;

@EnableWebSecurity
@Configuration
public class SecurityConfig {

    @Value("${admin.password:}")
    private String configuredAdminPassword;
    
    @Value("${user.password:}")
    private String configuredUserPassword;

    // Generate UUID passwords for admin and user (fallback if not configured)
    public static final String GENERATED_ADMIN_PASSWORD = UUID.randomUUID().toString();
    public static final String GENERATED_USER_PASSWORD = UUID.randomUUID().toString();

    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        // Use configured password if available, otherwise use generated UUID
        String adminPassword = StringUtils.hasText(configuredAdminPassword) ? 
            configuredAdminPassword : GENERATED_ADMIN_PASSWORD;
        String userPassword = StringUtils.hasText(configuredUserPassword) ? 
            configuredUserPassword : GENERATED_USER_PASSWORD;
            
        // Log the passwords for development purposes
        if (StringUtils.hasText(configuredAdminPassword)) {
            System.out.println("=== ADMIN LOGIN CREDENTIALS ===");
            System.out.println("Username: admin");
            System.out.println("Password: " + adminPassword + " (configured)");
            System.out.println("================================");
        } else {
            System.out.println("=== ADMIN LOGIN CREDENTIALS ===");
            System.out.println("Username: admin");
            System.out.println("Password: " + adminPassword + " (auto-generated)");
            System.out.println("================================");
        }
        
        if (StringUtils.hasText(configuredUserPassword)) {
            System.out.println("=== USER LOGIN CREDENTIALS ===");
            System.out.println("Username: user");
            System.out.println("Password: " + userPassword + " (configured)");
            System.out.println("===============================");
        } else {
            System.out.println("=== USER LOGIN CREDENTIALS ===");
            System.out.println("Username: user");
            System.out.println("Password: " + userPassword + " (auto-generated)");
            System.out.println("===============================");
        }

        UserDetails admin = User.withUsername("admin")
                .password(passwordEncoder.encode(adminPassword))
                .roles("ADMIN")
                .build();
        UserDetails user = User.withUsername("user")
                .password(passwordEncoder.encode(userPassword))
                .roles("USER")
                .build();
        return new InMemoryUserDetailsManager(admin, user);
    }@Bean
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
