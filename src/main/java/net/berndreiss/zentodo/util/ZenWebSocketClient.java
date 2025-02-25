package net.berndreiss.zentodo.util;

import jakarta.websocket.*;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@ClientEndpoint
public class ZenWebSocketClient extends Endpoint {

    public final static String WEBSOCKET_ENDPOINT = "ws/pubsub";

    private Session session;
    private Consumer<String> messageConsumer;

    @OnOpen
    public void onOpen(Session session, EndpointConfig endpointConfig) {
        this.session = session;
        System.out.println("Connected to server");

        session.addMessageHandler(new MessageHandler.Whole<String>() {
            @Override
            public void onMessage(String message) {
                messageConsumer.accept(message);
                System.out.println("Received message: " + message);
            }
        });
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
        System.err.println("WebSocket error: " + throwable.getMessage());
    }

    public ZenWebSocketClient(Consumer<String> messageConsumer){
        this.messageConsumer = messageConsumer;
    }

    public void connect(String token) {
        try {

            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            URI serverUri = new URI("ws://" + ClientStub.SERVER + WEBSOCKET_ENDPOINT);


            ClientEndpointConfig config = ClientEndpointConfig.Builder.create()
                    .configurator(new ClientEndpointConfig.Configurator(){
                        @Override
                        public void beforeRequest(Map<String, List<String>> headers){
                            headers.put("Authorization", Collections.singletonList("Bearer " + token));
                        }
                    })
                    .build();

            container.connectToServer(this, config, serverUri);

            synchronized (ZenWebSocketClient.class){
                ZenWebSocketClient.class.wait();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
