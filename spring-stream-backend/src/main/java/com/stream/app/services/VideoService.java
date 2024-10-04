package com.stream.app.services;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.stream.app.entities.Video;
import com.stream.app.exceptions.VideoProcessingException;

public interface VideoService {

    Video save(Video video, MultipartFile file) throws VideoProcessingException;

    Video get(String videoId);

    Video getByTitle(String title);

    List<Video> getAll();

    String processVideoAsync(String videoId);

}
