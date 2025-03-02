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
    private User user;
    private ClientOperationHandler dbHandler;

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
            URL url = new URI(ClientStub.PROTOCOL + ClientStub.SERVER + "queue").toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + dbHandler.getToken(user.getId()));
            connection.setRequestProperty("device", String.valueOf(user.getDevice()));
            connection.setRequestProperty("t1", TimeDrift.getTimeStamp());
            connection.setDoOutput(true);

            String body = " ";
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = body.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
            if (connection.getResponseCode() != 200)
                throw new RuntimeException("Could not retrieve data from server.");

            dbHandler.clearQueue();

        } catch (URISyntaxException | ProtocolException ignored){} catch (IOException e) {
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
        System.err.println("WebSocket error: " + throwable.getMessage());
    }

    public ZenWebSocketClient(Consumer<String> messageConsumer, User user, ClientOperationHandler dbHandler){
        this.messageConsumer = messageConsumer;
        this.user = user;
        this.dbHandler = dbHandler;
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

            synchronized (ZenWebSocketClient.class){
                ZenWebSocketClient.class.wait();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
