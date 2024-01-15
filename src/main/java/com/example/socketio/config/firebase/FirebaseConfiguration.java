package com.example.socketio.config.firebase;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;


@Configuration
public class FirebaseConfiguration {

	// https://www.baeldung.com/spring-fcm
	// https://firebase.google.com/docs/admin/setup?hl=es-419
	
	@Value("${custom.firebase.credentials.path}")
	Resource firebaseCredentials;
	
	@Bean
	GoogleCredentials googleCredentials() throws IOException {
		if (FirebaseApp.getApps().isEmpty()) {
			// para no usar variable de entorno
			try {
				// InputStream credentialsStream = new ByteArrayInputStream(firebaseCredentials.getBytes());
				return GoogleCredentials.fromStream(firebaseCredentials.getInputStream());	
			} catch (IOException exception) {
				return GoogleCredentials.getApplicationDefault();
			}
		} else {
			// Use standard credentials chain. Useful when running inside GKE
	        return GoogleCredentials.getApplicationDefault();
		}
	}
	
	// Firebase
	@Bean
	FirebaseApp firebaseApp(GoogleCredentials credentials) {
	    FirebaseOptions options = FirebaseOptions.builder()
	      .setCredentials(credentials)
	      .build();

	    return FirebaseApp.initializeApp(options);
	}
	
	// Firebase Messaging (FCM)
	@Bean
	FirebaseMessaging firebaseMessaging(FirebaseApp firebaseApp) {
	    return FirebaseMessaging.getInstance(firebaseApp);
	}
}
