package com.livingframe.system.controller;

import com.livingframe.system.dto.AvatarResource;
import com.livingframe.system.service.MinioFileService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

/**
 * 头像公开读取入口。数据库只保存 MinIO 半路径，前端通过 VITE_AVATAR_BASE_URL 拼接。
 */
@RestController
@RequestMapping("/api/public/avatars")
@RequiredArgsConstructor
public class AvatarController {

    private static final String PUBLIC_PREFIX = "/api/public/avatars/";

    private final MinioFileService fileService;

    @GetMapping("/**")
    public ResponseEntity<byte[]> avatar(HttpServletRequest request) {
        String uri = request.getRequestURI();
        int index = uri.indexOf(PUBLIC_PREFIX);
        if (index < 0) {
            return ResponseEntity.notFound().build();
        }
        String objectName = uri.substring(index + PUBLIC_PREFIX.length());
        if (objectName.isBlank()) {
            return ResponseEntity.notFound().build();
        }
        try {
            AvatarResource resource = fileService.avatar(objectName);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(resource.contentType()))
                    .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePublic())
                    .body(resource.content());
        } catch (Exception ex) {
            return ResponseEntity.notFound().build();
        }
    }
}
