package com.example.socketio.config.socketio;

import com.corundumstudio.socketio.*;
import com.corundumstudio.socketio.listener.*;
import com.example.socketio.model.MessageFromClient;
import com.example.socketio.model.MessageFromServer;
import com.example.socketio.model.MessageType;
import com.example.socketio.services.firebase.FirebaseMessagingOperationsService;

import io.netty.handler.codec.http.HttpHeaders;
import jakarta.annotation.PreDestroy;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

@Configuration
public class SocketIOConfig {
	
    @Value("${socket-server.host}")
    private String host;

    @Value("${socket-server.port}")
    private Integer port;
    
    @Value("${server.ssl.key-store-password}")
    private String keyStorePassword;
    
    @Value("${server.ssl.key-store}")
    private Resource keyStoreFile;
    
    
    private SocketIOServer server;
    
    public final static String CLIENT_USER_NAME_PARAM = "authorname";
    public final static String CLIENT_USER_ID_PARAM = "authorid";
    public final static String AUTHORIZATION_HEADER = "Authorization";

    
    @Autowired
    private FirebaseMessagingOperationsService firebaseMessagingOperationsService;
    

    @Bean
    public SocketIOServer socketIOServer() throws IOException {
    	com.corundumstudio.socketio.Configuration config = new com.corundumstudio.socketio.Configuration();
        config.setHostname(host);
        config.setPort(port);

        // vamos a permitir a una web que no este en el mismo host y port conectarse. Si no da error de Cross Domain
        config.setAllowHeaders("Authorization");
        config.setOrigin("https://localhost:8080");

        // nuevo: cargar el certificado
        config.setKeyStorePassword(keyStorePassword);
        InputStream stream = keyStoreFile.getInputStream();
        config.setKeyStore(stream);
        // fin cargar certificado en la configuracion

        server = new SocketIOServer(config);

        server.addConnectListener(new MyConnectListener(server));
        server.addDisconnectListener(new MyDisconnectListener());
        server.addEventListener(SocketEvents.ON_MESSAGE_RECEIVED.value, MessageFromClient.class, onSendMessage());
        server.start();

        return server;
    }
    
    

    private static class MyConnectListener implements ConnectListener {

        private SocketIOServer server;
        
    	MyConnectListener(SocketIOServer server) {
    		this.server = server;
    	}
    	
        @Override
        public void onConnect(SocketIOClient client) {
        	// ojo por que este codigo no esta bien en si
        	
        	// TODO el que no tenga autorization no deberia ni poder conectarse. gestionar
        	HttpHeaders headers = client.getHandshakeData().getHttpHeaders();
        	if (headers.get(AUTHORIZATION_HEADER) == null) {
        		// FUERA
        		System.out.println("Nuevo cliente no permitida la conexion: " + client.getSessionId());
        		client.disconnect();
        	} else {
        		loadClientData(headers, client);
        		System.out.printf("Nuevo cliente conectado: %s . Clientes conectados ahora mismo: %d \n", client.getSessionId(), this.server.getAllClients().size());
        		
        		// aqui incluso se podria notificar a todos o a salas de que se ha conectado...
            	// server.getBroadcastOperations().sendEvent("chat message", messageFromServer);
        	}
        }

		private void loadClientData(HttpHeaders headers, SocketIOClient client) {

			try {
				String authorization = headers.get(AUTHORIZATION_HEADER);
				String jwt = authorization.split(" ")[1];
				
	    		// TODO HAY QUE VALIDAR Y CARGAR ESTOS DATOS DEL JWT! y si no no dejar conectarle o desconectarle
				// si esta autenticado y puede, meterle en sus salas correspondientes...
				// Esto está hardcodeado
				// vamos a meter el userId y el userName en el socket, para futuras operaciones.
				
				String[] datos = jwt.split(":");
				String authorId = datos[1];
				String authorName = datos[2];

				client.set(CLIENT_USER_ID_PARAM, authorId);
				client.set(CLIENT_USER_NAME_PARAM, authorName);
				
				// TODO ejemplo de salas
				// ojo por que "Room1" no es la misma sala que "room1"
				client.joinRoom("default-room");
				client.joinRoom("Room1");
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
    }
    
    private static class MyDisconnectListener implements DisconnectListener {
    	@Override
		public void onDisconnect(SocketIOClient client) {
			client.getNamespace().getAllClients().stream().forEach(data-> {
				System.out.println("user disconnected "+ data.getSessionId().toString());
				// notificateDisconnectToUsers(client);
			});
		}

		// podemos notificar a los demas usuarios que ha salido. Ojo por que el broadcast envia a todos
    	private void notificateDisconnectToUsers(SocketIOClient client) {
			String room = null;
			String message = "el usuario se ha desconectado salido";
        	String authorIdS = client.get(CLIENT_USER_ID_PARAM);
        	Integer authorId = Integer.valueOf(authorIdS);
        	String authorName = client.get(CLIENT_USER_NAME_PARAM);
        	
        	MessageFromServer messageFromServer = new MessageFromServer(
        		MessageType.SERVER, 
        		room, 
        		message, 
        		authorName, 
        		authorId
        	);
			client.getNamespace().getBroadcastOperations().sendEvent(SocketEvents.ON_SEND_MESSAGE.value, messageFromServer);
    	}
    }
    
    private DataListener<MessageFromClient> onSendMessage() {
        return (senderClient, data, acknowledge) -> {
        	
        	String authorIdS = senderClient.get(CLIENT_USER_ID_PARAM);
        	Integer authorId = Integer.valueOf(authorIdS);
        	String authorName = senderClient.get(CLIENT_USER_NAME_PARAM);
        	
        	System.out.printf("Mensaje recibido de (%d) %s. El mensaje es el siguiente: %s \n", authorId, authorName, data.toString());
        	
        	// TODO comprobar si el usuario esta en la room a la que quiere enviar...
        	boolean isAllowedToSendToRoom = checkIfSendCanSendToRoom(senderClient, data.getRoom());
        	if (isAllowedToSendToRoom) {

            	MessageFromServer message = new MessageFromServer(
            		MessageType.CLIENT, 
            		data.getRoom(), 
            		data.getMessage(), 
            		authorName, 
            		authorId
            	);
            	
            	// enviamos a la room correspondiente:
            	server.getRoomOperations(data.getRoom()).sendEvent(SocketEvents.ON_SEND_MESSAGE.value, message);
            	// TODO esto es para mandar a todos los clientes. No para mandar a los de una Room
            	// senderClient.getNamespace().getBroadcastOperations().sendEvent("chat message", message);

            	// esto puede que veamos mas adelante
                // acknowledge.sendAckData("El mensaje se envio al destinatario satisfactoriamente");

            	// enviar notificaciones
            	List<String> deviceTokens = new ArrayList<String>();
            	deviceTokens.add("fELjajD7Q-mg4nUrPMzOPa:APA91bEUjgJdPiD7dSYh5FW44rkq9b3FABvAVW9fhHFdGXhnHvcXFXqu4gJxBJNnwO6wv962hPGVVdGMrn0K_7Nm3-TwL5D2duhsjbjQN_GltVa8Ck2VbBKFu3vXoGYN8kzdHtnTeIo4");
            	firebaseMessagingOperationsService.sendMulticastNotification(deviceTokens);
        	} else {
        		// TODO
        		// como minimo no dejar. se podria devolver un mensaje como MessageType.SERVER de que no puede enviar...
        		// incluso ampliar la clase messageServer con otro enum de errores
        		// o crear un evento nuevo, no "chat message" con otros datos
        	}
        };
    }



	private boolean checkIfSendCanSendToRoom(SocketIOClient senderClient, String room) {
    	if (senderClient.getAllRooms().contains(room)) {
    		System.out.println("SI tiene permiso para enviar mensaje en la room");
    		return true;
    	} else {
    		System.out.println("NO tiene permiso para enviar mensaje en la room");
    		return false;
    	}
	}

	@PreDestroy
	public void stopSocketIOServer() {
		this.server.stop();
	}
}