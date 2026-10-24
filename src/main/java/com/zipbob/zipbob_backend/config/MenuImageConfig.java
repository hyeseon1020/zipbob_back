package com.zipbob.zipbob_backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Configuration
/*
이미지 경로 연결

새 업로드 이미지가 있는 외부 images/menus
기존 이미지가 있는 static/images/menus
*/
public class MenuImageConfig implements WebMvcConfigurer {

    private final Path imageDirectory;

    public MenuImageConfig(
            @Value("${app.menu-image-dir:./images/menus}") String imageDirectory
    ) throws IOException {
        this.imageDirectory = Path.of(imageDirectory)
                .toAbsolutePath()
                .normalize();

        Files.createDirectories(this.imageDirectory);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/images/menus/**")
                .addResourceLocations(
                        imageDirectory.toUri().toString(),
                        "classpath:/static/images/menus/"
                )
                .setCachePeriod(0);
    }
}