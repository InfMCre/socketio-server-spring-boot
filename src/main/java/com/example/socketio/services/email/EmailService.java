package com.example.socketio.services.email;

public interface EmailService {
	void sendEmail(String toEmail, String subject, String message);
}
