package com.example.hibernatedemo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Custom external asset mapping removed — static assets are served from
 * classpath:/static (src/main/resources/static). This class is kept as a
 * placeholder to avoid accidental re-introduction of file-system based mappings.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Serve static assets from classpath:/static/assets/ at the /assets/** path
        registry.addResourceHandler("/assets/**")
                .addResourceLocations("classpath:/static/assets/")
                .setCachePeriod(0);
    }
}
