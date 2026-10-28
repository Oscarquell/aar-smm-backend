package kg.azem.portfolio.config;

import java.util.Arrays;
import java.util.List;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final List<String> allowedOrigins;
    private final Path mediaStoragePath;

    public WebConfig(
            @Value("${app.cors.allowed-origins:http://localhost:3000,http://localhost:3001}")
            String configuredOrigins,
            @Value("${app.media.storage-path:storage/uploads}") String mediaStoragePath
    ) {
        this.allowedOrigins = Arrays.stream(configuredOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList();
        this.mediaStoragePath = Path.of(mediaStoragePath).toAbsolutePath().normalize();
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins.toArray(String[]::new))
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("Content-Type", "Authorization");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(mediaStoragePath.toUri().toString(), "classpath:/static/uploads/");
    }
}
