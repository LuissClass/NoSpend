package com.onlymymoney.adapter.in.web;
import org.springframework.beans.factory.annotation.Value; import org.springframework.context.annotation.*; import org.springframework.web.servlet.config.annotation.*;
@Configuration public class WebConfig implements WebMvcConfigurer {
 @Value("${cors.allowed-origins:http://localhost:5173}") String origins;
 public void addCorsMappings(CorsRegistry r){r.addMapping("/api/**").allowedOrigins(origins).allowedMethods("*").allowedHeaders("*");}
}
