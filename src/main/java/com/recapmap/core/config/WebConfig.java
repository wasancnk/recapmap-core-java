package com.recapmap.core.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Serve React static files: map /admin/static/** to the actual location classpath:/static/static/
        registry.addResourceHandler("/admin/static/**")
                .addResourceLocations("classpath:/static/static/");
        
        // Serve favicon and other root-level assets
        registry.addResourceHandler("/admin/favicon.ico")
                .addResourceLocations("classpath:/static/favicon.ico");
        
        // Serve any other assets directly from static root
        registry.addResourceHandler("/admin/asset-manifest.json")
                .addResourceLocations("classpath:/static/asset-manifest.json");
                
        // Default static resources for other parts of the app
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/");
                
        // Additional CSS/JS resources
        registry.addResourceHandler("/css/**", "/js/**")
                .addResourceLocations("classpath:/static/css/", "classpath:/static/js/");
    }
}
