package com.stream.app.services.impl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.stream.app.entities.Video;
import com.stream.app.exceptions.VideoProcessingException;
import com.stream.app.exceptions.VideoNotFoundException;
import com.stream.app.repositories.VideoRepository;
import com.stream.app.services.VideoService;

import jakarta.annotation.PostConstruct;

@Service
public class VideoServiceImpl implements VideoService {

    private static final String VIDEO_CODEC = "libx264";
    private static final String AUDIO_CODEC = "aac";
    private static final String HLS_FORMAT = "hls";
    private static final String SEGMENT_PATTERN = "segment_%03d.ts";
    private static final String MASTER_PLAYLIST = "master.m3u8";

    private static final Logger logger = LoggerFactory.getLogger(VideoServiceImpl.class);

    @Value("${files.video}")
    private String DIR;

    @Value("${file.video.hls}")
    private String HLS_DIR;

    private VideoRepository videoRepository;

    public VideoServiceImpl(VideoRepository videoRepository) {
        this.videoRepository = videoRepository;
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(Path.of(DIR)); // Ensure video directory exists
            Files.createDirectories(Path.of(HLS_DIR)); // Ensure HLS directory exists
            logger.info("Directories initialized successfully.");
        } catch (IOException e) {
            logger.error("Error initializing directories: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public Video save(Video video, MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            String contentType = file.getContentType();
            video.setContentType(contentType);

            String filename = file.getOriginalFilename();
            if (filename == null) {
                throw new IllegalArgumentException("File name cannot be null");
            }

            String cleanFileName = StringUtils.cleanPath(filename);
            String cleanFolder = StringUtils.cleanPath(DIR);
            Path path = Path.of(cleanFolder, cleanFileName);
            video.setFilePath(path.toString());

            // Copy the file to the desired location
            Files.copy(inputStream, path, StandardCopyOption.REPLACE_EXISTING);

            Video savedVideo = videoRepository.save(video);
            logger.info("Video saved with ID: {}", savedVideo.getVideoId());

            processVideoAsync(savedVideo.getVideoId());

            return savedVideo;
        } catch (IOException | IllegalArgumentException e) {
            logger.error("Error saving video: {}", e.getMessage(), e);
            throw new VideoProcessingException("Error while saving video", e);
        }
    }

    @Override
    public Video get(String videoId) {
        Video video = videoRepository.findById(videoId).orElseThrow(
                () -> new VideoNotFoundException("Video with ID " + videoId + " not found"));

        return video;
    }

    @Override
    public Video getByTitle(String title) {
        return null;
    }

    @Override
    public List<Video> getAll() {
        return videoRepository.findAll();
    }

    // Helper method to log output and errors from the process
    private void logProcessOutput(Process process) throws IOException {
        try (InputStream errorStream = process.getErrorStream();
                InputStream outputStream = process.getInputStream()) {

            String errorOutput = new String(errorStream.readAllBytes());
            String standardOutput = new String(outputStream.readAllBytes());

            if (!errorOutput.isEmpty()) {
                logger.error("FFmpeg error output: {}", errorOutput);
            }

            if (!standardOutput.isEmpty()) {
                logger.info("FFmpeg standard output: {}", standardOutput);
            }
        }
    }

    @Async
    @Override
    public String processVideoAsync(String videoId) {
        Video video = get(videoId);
        String filePath = video.getFilePath();

        // path where data will be stored
        Path videoPath = Path.of(filePath);
        // path where HLS segments and playlist will be stored
        Path outputPath = Path.of(HLS_DIR, videoId);

        try {
            Files.createDirectories(outputPath);

            // ffmpeg command
            String ffmpegCmd = String.format(
                    "ffmpeg -i \"%s\" -c:v %s -c:a %s -strict -2 -f %s -hls_time 10 -hls_list_size 0 -hls_segment_filename \"%s/%s\"  \"%s/%s\"",
                    videoPath, VIDEO_CODEC, AUDIO_CODEC, HLS_FORMAT, outputPath, SEGMENT_PATTERN, outputPath,
                    MASTER_PLAYLIST);

            // execute the command
            ProcessBuilder processBuilder = new ProcessBuilder("cmd.exe", "/c", ffmpegCmd);
            processBuilder.inheritIO();
            Process process = processBuilder.start();
            int exit = process.waitFor();

            logProcessOutput(process);

            if (exit != 0) {
                throw new VideoProcessingException("Video processing failed for video ID: " + videoId);
            }

            // Delete the original video file after successful processing
            Files.deleteIfExists(videoPath);
            logger.info("Original video file deleted: {}", videoPath);

            // Update the database to store the HLS metadata (path to master playlist)
            video.setFilePath(outputPath.toString());  // Store the directory of HLS files
            video.setContentType("application/vnd.apple.mpegurl");  // Set the content type for HLS
            videoRepository.save(video);

            logger.info("Video processing completed for video ID: {}", videoId);
            return videoId;

        } catch (IOException | InterruptedException e) {
            logger.error("Error processing video: {}", e.getMessage(), e);
            Thread.currentThread().interrupt();
            throw new VideoProcessingException("Error processing video with ID: " + videoId, e);
        }
    }

}
