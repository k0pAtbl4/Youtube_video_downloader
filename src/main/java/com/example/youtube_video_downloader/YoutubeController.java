package com.example.youtube_video_downloader;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.io.FileUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@RestController
public class YoutubeController {

    @PostMapping("/download")
    public ResponseEntity<byte[]> downloadVideo(@RequestBody ObjectNode json) {
        String url = json.get("url").asText();
        String quality = json.get("quality").asText();
        try {
            String command = "yt-dlp -f " + quality + " " + url;
            Process process = Runtime.getRuntime().exec(command);
            boolean finished = process.waitFor(600, TimeUnit.SECONDS);

            if (!finished) {
                process.destroy();
                return ResponseEntity.status(500).body("Timeout while downloading video".getBytes());
            }

            File folder = new File(".");
            File[] listOfFiles = folder.listFiles();
            File downloadedFile = null;

            for (File file : listOfFiles) {
                if (file.isFile() && (file.getName().endsWith(".mp4") || file.getName().endsWith(".mkv"))) {
                    downloadedFile = file;
                    break;
                }
            }

            if (downloadedFile != null) {
                byte[] fileContent = FileUtils.readFileToByteArray(downloadedFile);
                String encodedFileName = URLEncoder.encode(downloadedFile.getName(), StandardCharsets.UTF_8.toString());
                HttpHeaders headers = new HttpHeaders();
                headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFileName + "\"");

                File finalDownloadedFile = downloadedFile;
                new Thread(() -> {
                    try {
                        Thread.sleep(5000);
                        FileUtils.forceDelete(finalDownloadedFile);
                    } catch (IOException | InterruptedException e) {
                        e.printStackTrace();
                    }
                }).start();

                return ResponseEntity.ok().headers(headers).body(fileContent);
            } else {
                return ResponseEntity.status(500).body("Failed to download video".getBytes());
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(("An error occurred: " + e.getMessage()).getBytes());
        }
    }
}