package com.stream.app.exceptions;

public class VideoNotFoundException extends RuntimeException {

    public VideoNotFoundException(String message) {
        super(message);
    }
    
}
