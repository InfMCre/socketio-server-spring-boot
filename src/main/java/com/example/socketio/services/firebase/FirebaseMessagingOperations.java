package com.example.socketio.services.firebase;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;

@Service
public class FirebaseMessagingOperations implements FirebaseMessagingOperationsService {
	
	private final FirebaseMessaging fcm;
	@Autowired
	public FirebaseMessagingOperations(FirebaseMessaging fcm) {
    	this.fcm = fcm;
    }
	
	// TODO falta modificar los parametros de entrada para no hardcodear
	@Override
    public void sendMulticastNotification(List deviceTokens) {
    	
    	// mandamos notificacion a los clientes de la lista. ESTO ESTA HARDCODEADO!
    	
    	
    	// para un usuario
    	/*
    	String deviceToken = "";
    	Message msg = Message.builder()
    			  .setToken(deviceToken)
    			  .putData("body", "some data")
    			  .build();
    	*/
    	// para muchos usuarios
    	
    	Notification notification = Notification.builder()
    		.setTitle("titulo")
    		.setBody("Cuerpo")
    		.build();
    	
    	@SuppressWarnings("unchecked")
		MulticastMessage msg = MulticastMessage.builder()
			.addAllTokens(deviceTokens)
			.setNotification(notification)
			.putData("body", "some data")
			.build();
    	try {
			fcm.sendMulticast(msg);
		} catch (FirebaseMessagingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	// The returned BatchResponse contains the generated message identifiers and any errors associated with the delivery for a given client.
    	// BatchResponse response = fcm.sendMulticast(msg);
	}

}
