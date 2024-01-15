package com.example.socketio.services.firebase;

import java.util.List;

public interface FirebaseMessagingOperationsService {
	void sendMulticastNotification(List deviceTokens);
}
