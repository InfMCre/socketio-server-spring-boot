package com.example.socketio;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SocketioApplication {

	public static void main(String[] args) {
		// originalmente era:
		// SpringApplication.run(SocketioApplication.class, args);
		
		SpringApplication application = new SpringApplication(SocketioApplication.class);
		// incluir ssl para https
        application.setAdditionalProfiles("ssl");
        application.run(args);
	}

}
