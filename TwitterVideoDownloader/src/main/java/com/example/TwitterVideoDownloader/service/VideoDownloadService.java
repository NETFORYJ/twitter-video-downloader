package com.example.TwitterVideoDownloader.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class VideoDownloadService {
   private String getCookieFile(String videoUrl) {
      if (!videoUrl.contains("youtube.com") && !videoUrl.contains("youtu.be")) {
         if (videoUrl.contains("facebook.com")) {
            return "cookies/facebook_cookies.txt";
         } else if (!videoUrl.contains("twitter.com") && !videoUrl.contains("x.com")) {
            return videoUrl.contains("instagram.com") ? "cookies/instagram_cookies.txt" : null;
         } else {
            return "cookies/twitter_cookies.txt";
         }
      } else {
         return "cookies/youtube_cookies.txt";
      }
   }

   public String fetchVideoDetails(String videoUrl) throws Exception {
      System.out.println("Fetching video details for: " + videoUrl);
      List<String> command = new ArrayList();
      command.add("yt-dlp");
      command.add("--skip-download");
      command.add("--print-json");
      command.add("--no-check-certificate");
      command.add(videoUrl);
      String cookieFile = this.getCookieFile(videoUrl);
      if (cookieFile != null) {
         command.add("--cookies");
         command.add(cookieFile);
      }

      ProcessBuilder processBuilder = new ProcessBuilder(command);
      processBuilder.redirectErrorStream(true);
      Process process = processBuilder.start();
      StringBuilder output = new StringBuilder();
      BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

      String line;
      try {
         while((line = reader.readLine()) != null) {
            output.append(line).append("\n");
         }
      } catch (Throwable var14) {
         try {
            reader.close();
         } catch (Throwable var13) {
            var14.addSuppressed(var13);
         }

         throw var14;
      }

      reader.close();
      int var15 = process.waitFor();
      if (var15 != 0) {
         throw new RuntimeException("Failed to fetch video details.");
      } else {
         ObjectMapper objectMapper = new ObjectMapper();
         Map<String, Object> jsonResponse = (Map)objectMapper.readValue(output.toString(), Map.class);
         String thumbnailUrl = jsonResponse.getOrDefault("thumbnail", "").toString();
         String title = jsonResponse.getOrDefault("title", "Unknown Title").toString();
         Map<String, String> response = new HashMap();
         response.put("thumbnail", thumbnailUrl);
         response.put("title", title);
         return objectMapper.writeValueAsString(response);
      }
   }

   public String downloadVideo(String videoUrl) throws Exception {
      System.out.println("Downloading: " + videoUrl);
      String uniqueFileName = UUID.randomUUID() + ".mp4";
      String filePath = "downloads/" + uniqueFileName;
      (new File("downloads")).mkdirs();
      List<String> command = new ArrayList();
      command.add("yt-dlp");
      command.add("-f");
      command.add("best");
      command.add("--no-check-certificate");
      command.add("-o");
      command.add(filePath);
      command.add(videoUrl);
      String cookieFile = this.getCookieFile(videoUrl);
      if (cookieFile != null) {
         command.add("--cookies");
         command.add(cookieFile);
      }

      ProcessBuilder processBuilder = new ProcessBuilder(command);
      processBuilder.redirectErrorStream(true);
      Process process = processBuilder.start();
      process.waitFor();
      File downloadedFile = new File(filePath);
      if (!downloadedFile.exists()) {
         throw new RuntimeException("Downloaded file not found: " + filePath);
      } else {
         return uniqueFileName;
      }
   }

   public String getThumbnail(String videoUrl) throws Exception {
      System.out.println("Fetching thumbnail for: " + videoUrl);
      List<String> command = new ArrayList();
      command.add("yt-dlp");
      command.add("--skip-download");
      command.add("--print-json");
      command.add("--no-check-certificate");
      command.add(videoUrl);
      String cookieFile = this.getCookieFile(videoUrl);
      if (cookieFile != null) {
         command.add("--cookies");
         command.add(cookieFile);
      }

      ProcessBuilder processBuilder = new ProcessBuilder(command);
      processBuilder.redirectErrorStream(true);
      Process process = processBuilder.start();
      StringBuilder output = new StringBuilder();
      BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

      String line;
      try {
         while((line = reader.readLine()) != null) {
            output.append(line).append("\n");
         }
      } catch (Throwable var12) {
         try {
            reader.close();
         } catch (Throwable var11) {
            var12.addSuppressed(var11);
         }

         throw var12;
      }

      reader.close();
      int var13 = process.waitFor();
      if (var13 != 0) {
         throw new RuntimeException("Failed to fetch thumbnail.");
      } else {
         ObjectMapper objectMapper = new ObjectMapper();
         Map<String, Object> jsonResponse = (Map)objectMapper.readValue(output.toString(), Map.class);
         String thumbnailUrl = jsonResponse.getOrDefault("thumbnail", "").toString();
         if (thumbnailUrl.isEmpty()) {
            thumbnailUrl = "https://via.placeholder.com/350x200?text=No+Thumbnail";
         }

         return thumbnailUrl;
      }
   }
}