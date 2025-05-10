package net.berndreiss.zentodo.util;

import jakarta.websocket.*;
import net.berndreiss.zentodo.data.ClientOperationHandler;
import net.berndreiss.zentodo.data.User;

import java.io.IOException;
import java.io.OutputStream;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@ClientEndpoint
public class ZenWebSocketClient extends Endpoint {

    public final static String WEBSOCKET_ENDPOINT = "ws/pubsub";

    private Session session;
    private Consumer<String> messageConsumer;
    private ClientStub clientStub;

    @OnOpen
    public void onOpen(Session session, EndpointConfig endpointConfig) {
        this.session = session;
        System.out.println("Connected to server");

        session.addMessageHandler(new MessageHandler.Whole<String>() {
            @Override
            public void onMessage(String message) {
                messageConsumer.accept(message);
            }
        });
        try {
            HttpURLConnection connection = clientStub.sendAuthPostMessage(ClientStub.PROTOCOL + ClientStub.SERVER + "queue", " ");
            if (connection.getResponseCode() != 200)
                throw new RuntimeException("Could not retrieve data from server.");

            clientStub.status = Status.UPDATED;
            clientStub.clearQueue();


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @OnMessage
    public void onMessage(String message) {
        System.out.println("Received message: " + message);
    }

    @OnClose
    public void onClose(Session session, CloseReason reason) {
        System.out.println("Connection closed: " + reason.getReasonPhrase());
    }


    @OnError
    public void onError(Session session, Throwable throwable) {
        System.err.println("WebSocket error: " + throwable.getCause());
    }

    public ZenWebSocketClient(Consumer<String> messageConsumer, ClientStub clientStub){
        this.messageConsumer = messageConsumer;
        this.clientStub = clientStub;
    }

    public void connect(String email, String token, long device) {
        try {

            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            URI serverUri = new URI("ws://" + ClientStub.SERVER + WEBSOCKET_ENDPOINT);


            ClientEndpointConfig config = ClientEndpointConfig.Builder.create()
                    .configurator(new ClientEndpointConfig.Configurator(){
                        @Override
                        public void beforeRequest(Map<String, List<String>> headers){
                            headers.put("Authorization", Collections.singletonList("Bearer " + token));
                            headers.put("email", Collections.singletonList(email));
                            headers.put("device", Collections.singletonList(String.valueOf(device)));
                        }
                    })
                    .build();

            container.connectToServer(this, config, serverUri);

        } catch (Exception e) {
            //TODO IMPLEMENT
        }
    }
}
