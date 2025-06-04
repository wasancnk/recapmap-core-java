package com.recapmap.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import jakarta.servlet.MultipartConfigElement;
import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.util.unit.DataSize;

import com.recapmap.core.config.SecurityConfig;
import com.recapmap.core.service.PdfService;

@SpringBootApplication
public class CoreApplication {

	private static final Logger logger = LoggerFactory.getLogger(CoreApplication.class);

	public static void main(String[] args) {
		logger.info("=== Starting RecapMap Core Application ===");
		logger.info("Java version: {}", System.getProperty("java.version"));
		logger.info("Spring Boot version: {}", org.springframework.boot.SpringBootVersion.getVersion());
		logger.info("Working directory: {}", System.getProperty("user.dir"));
		
		try {
			SpringApplication.run(CoreApplication.class, args);
			logger.info("=== RecapMap Core Application Started Successfully ===");
		} catch (Exception e) {
			logger.error("=== Failed to start RecapMap Core Application ===", e);
			throw e;
		}
	}
	@Bean
	public CommandLineRunner printPasswords() {
		return args -> {
			logger.info("==============================");
			logger.info("APPLICATION CREDENTIALS:");
			logger.info("Admin login: admin");
			logger.info("Admin password: {}", SecurityConfig.ADMIN_PASSWORD);
			logger.info("User login: user");
			logger.info("User password: {}", SecurityConfig.USER_PASSWORD);
			logger.info("Application URL: http://localhost:8080");
			logger.info("==============================");
			
			System.out.println("\n==============================");
			System.out.println("Admin login: admin");
			System.out.println("Admin password: " + SecurityConfig.ADMIN_PASSWORD);
			System.out.println("User login: user");
			System.out.println("User password: " + SecurityConfig.USER_PASSWORD);
			System.out.println("Application URL: http://localhost:8080");
			System.out.println("==============================\n");
		};
	}@Bean
	public MultipartConfigElement multipartConfigElement() {
		MultipartConfigFactory factory = new MultipartConfigFactory();
		DataSize maxSize = DataSize.ofMegabytes(PdfService.MAX_UPLOAD_SIZE_MB);
		factory.setMaxFileSize(maxSize);
		factory.setMaxRequestSize(maxSize);
		return factory.createMultipartConfig();
	}

	// Toggle this to true to bypass login for development
	public static final boolean BYPASS_LOGIN = false;
}
