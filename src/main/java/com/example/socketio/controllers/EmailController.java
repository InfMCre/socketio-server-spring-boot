package com.example.socketio.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.socketio.services.email.EmailService;


@RestController
@RequestMapping("/api/email")
public class EmailController {
	
	@Autowired
	EmailService emailService;
	
    @GetMapping("/send-demo")
    public String sendMessage() {
        // Envia un mensaje a todos los clientes conectados
    	// TODO cambiar el toEmail
    	String toEmail = "destinatario@dominio.com";
    	String subject = "Título prueba";
    	String message = "Prueba Email!";
    	emailService.sendEmail(toEmail, subject, message);
    	
        return "Mensaje enviado";
    }
}
