package com.stream.app.spring_stream_backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.stream.app.services.VideoService;

@SpringBootTest
class SpringStreamBackendApplicationTests {

	@Autowired
	VideoService videoService;

	@Test
	void contextLoads() {
		videoService.processVideoAsync("7941bd8c-fb34-4181-bf4e-395f620a05b7");
	}

}
