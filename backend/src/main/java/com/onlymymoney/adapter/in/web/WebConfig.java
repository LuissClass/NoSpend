package com.onlymymoney.adapter.in.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.web.servlet.config.annotation.*;

/**
 * Spring MVC configuration for the web layer.
 * <p>
 * Currently it only configures CORS so that a browser-based frontend served from a different
 * origin (for example, a development server) can call the API.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    /**
     * Allowed CORS origin(s), read from the {@code cors.allowed-origins} property.
     * Defaults to {@code http://localhost:5173} when the property is not defined.
     */
    @Value("${cors.allowed-origins:http://localhost:5173}")
    String origins;

    /**
     * Registers the CORS rules for all the endpoints under {@code /api/**}.
     * <p>
     * Requests from the configured origin(s) are accepted with any HTTP method and any header.
     *
     * @param r the registry where the CORS mappings are added
     */
    public void addCorsMappings(CorsRegistry r) {
        r.addMapping("/api/**").allowedOrigins(origins).allowedMethods("*").allowedHeaders("*");
    }
}