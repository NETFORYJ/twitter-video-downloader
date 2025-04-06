 package com.example.TwitterVideoDownloader.WebConfig;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
   public void addResourceHandlers(ResourceHandlerRegistry registry) {
      registry.addResourceHandler(new String[]{"/downloads/**"}).addResourceLocations(new String[]{"file:downloads/"});
   }
}