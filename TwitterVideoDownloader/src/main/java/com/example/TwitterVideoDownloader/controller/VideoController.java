package com.example.TwitterVideoDownloader.controller;

import com.example.TwitterVideoDownloader.service.VideoDownloadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@RestController
@RequestMapping("/api")
public class VideoController {

    @Autowired
    private VideoDownloadService videoDownloadService;

    private static final Logger logger = Logger.getLogger(VideoController.class.getName());

    @PostMapping("/fetch-details")
    public ResponseEntity<String> fetchVideoDetails(@RequestParam String videoUrl) {
        try {
            logger.info("Fetching video details for: " + videoUrl);
            String jsonResponse = videoDownloadService.fetchVideoDetails(videoUrl);
            return ResponseEntity.ok(jsonResponse);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error fetching video details: " + e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"Error fetching video details.\"}");
        }
    }

    @PostMapping("/download")
    public ResponseEntity<Resource> downloadVideo(@RequestParam String videoUrl) {
        if (!videoUrl.matches("^(https?://)?(www\\.)?(twitter\\.com|x\\.com)/[a-zA-Z0-9_]+/status/\\d+(\\?.*)?$")) {
            logger.warning("Blocked non-Twitter URL: " + videoUrl);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }

        try {
            logger.info("Downloading video from: " + videoUrl);
            
            // First ensure downloads directory exists
            Path downloadsDir = Paths.get("downloads");
            if (!Files.exists(downloadsDir)) {
                Files.createDirectories(downloadsDir);
            }
            
            String filename = videoDownloadService.downloadVideo(videoUrl);
            Path path = downloadsDir.resolve(filename).normalize();
            
            // Additional safety checks
            if (!path.startsWith(downloadsDir)) {
                throw new SecurityException("Attempt to access file outside downloads directory");
            }

            Resource fileResource = new UrlResource(path.toUri());
            if (fileResource.exists() && fileResource.isReadable()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                        .body(fileResource);
            } else {
                throw new RuntimeException("Failed to read video file");
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error downloading video: " + e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/thumbnail")
    public ResponseEntity<?> getVideoThumbnail(@RequestParam String videoUrl) {
        logger.info("Fetching thumbnail for: " + videoUrl);

        try {
            String thumbnailPath = videoDownloadService.getThumbnail(videoUrl);
            
            if (thumbnailPath == null || thumbnailPath.trim().isEmpty()) {
                logger.warning("Thumbnail not found.");
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Collections.singletonMap("error", "Thumbnail not found"));
            }

            // If it's a URL, return it directly
            if (thumbnailPath.startsWith("http")) {
                return ResponseEntity.ok(Collections.singletonMap("thumbnailUrl", thumbnailPath));
            }

            // For local files, serve them through the controller
            Path file = Paths.get(thumbnailPath);
            if (!Files.exists(file)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Collections.singletonMap("error", "Thumbnail file not found"));
            }

            Resource resource = new FileSystemResource(file);
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG)
                    .body(resource);

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error fetching thumbnail: " + e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "Error fetching thumbnail"));
        }
    }
}