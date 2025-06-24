package net.berndreiss.zentodo.util;

import jakarta.websocket.*;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * A websocket client that receives operations performed by other devices.
 * The messages received are in the form of {"message": "value", "id": "value"}.
 * The message has to be acknowledged by sending the id in the body to /api/ackn.
 */
@ClientEndpoint
public class ZenWebSocketClient extends Endpoint {

    public final static String WEBSOCKET_ENDPOINT = "/wss/pubsub";

    /**
     * The message consumer to process messages from the server
     */
    private final Consumer<String> messageConsumer;
    /**
     * The client stub that uses this websocket client
     */
    private final ClientStub clientStub;

    /**
     * Get a new websocket client.
     *
     * @param messageConsumer the action to perform when receiving a message
     * @param clientStub      the client stub using the websocket client
     */
    public ZenWebSocketClient(Consumer<String> messageConsumer, ClientStub clientStub) {
        this.messageConsumer = messageConsumer;
        this.clientStub = clientStub;
    }

    @OnOpen
    public void onOpen(Session session, EndpointConfig endpointConfig) {
        if (clientStub.getMessagePrinter() != null)
            clientStub.getMessagePrinter().accept("Connected websocket client");
        session.addMessageHandler((MessageHandler.Whole<Object>) message ->{
            System.out.println(">>> Raw message type: " + message.getClass());
            System.out.println(">>> Raw message: " + message);
            if (message instanceof String str)
                messageConsumer.accept(str);
            else
                ClientStub.logger.warn("Unexpected message type: " + message.getClass().getName() + ", " + message);

        });
    }

    @OnMessage
    public void onMessage(String message) {}

    @OnClose
    public void onClose(Session session, CloseReason reason) {
        if (clientStub.getMessagePrinter() != null)
            clientStub.getMessagePrinter().accept("Disconnected websocket client");
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        ClientStub.logger.error("Websocket client encountered an error.", throwable);
    }

    /**
     * Establish a connection to the server.
     *
     * @param email  the email of the user
     * @param token  the JWT token for authorization
     * @param device the current device used
     * @throws DeploymentException thrown when there is a problem deploying the websocket client
     * @throws IOException         thrown when there is a problem connecting to the server
     */
    public void connect(String email, String token, long device) throws DeploymentException, IOException {

        WebSocketContainer container = ContainerProvider.getWebSocketContainer();
        URI serverUri;
        try {
            serverUri = new URI(ClientStub.WEBSOCKET_PROTOCOL + "://" + ClientStub.SERVER + WEBSOCKET_ENDPOINT);
        } catch (URISyntaxException e) {
            ClientStub.logger.error("URI not valid", e);
            throw new RuntimeException("Invalid URI", e);
        }

        ClientEndpointConfig config = ClientEndpointConfig.Builder.create()
                .configurator(new ClientEndpointConfig.Configurator() {
                    @Override
                    public void beforeRequest(Map<String, List<String>> headers) {
                        headers.put("Authorization", Collections.singletonList("Bearer " + token));
                        headers.put("email", Collections.singletonList(email));
                        headers.put("device", Collections.singletonList(String.valueOf(device)));
                    }
                })
                .build();
        container.connectToServer(this, config, serverUri);
    }
}
