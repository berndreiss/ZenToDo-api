package net.berndreiss.zentodo.util;

import okhttp3.*;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * A websocket client that receives operations performed by other devices.
 * The messages received are in the form of {"message": "value", "id": "value"}.
 * The message has to be acknowledged by sending the id in the body to /api/ackn.
 */
public class ZenWebSocketClient {

    public final static String WEBSOCKET_ENDPOINT = "/wss/pubsub";

    private final Consumer<String> messageConsumer;
    private final ClientStub clientStub;
    private WebSocket webSocket;
    private OkHttpClient client;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private static final long KEEP_ALIVE_INTERVAL_SECONDS = 30;

    public ZenWebSocketClient(Consumer<String> messageConsumer, ClientStub clientStub) {
        this.messageConsumer = messageConsumer;
        this.clientStub = clientStub;
    }

    public void connect(String email, String token, long device) {
        String url = ClientStub.WEBSOCKET_PROTOCOL + "://" + ClientStub.SERVER + WEBSOCKET_ENDPOINT;

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + token)
                .addHeader("email", email)
                .addHeader("device", String.valueOf(device))
                .build();

        client = new OkHttpClient();

        webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(@NotNull WebSocket webSocket, @NotNull Response response) {
                if (clientStub.getMessagePrinter() != null)
                    clientStub.getMessagePrinter().accept("Connected to server");

                scheduler.scheduleAtFixedRate(() -> {
                    if (ZenWebSocketClient.this.webSocket != null) {
                        ZenWebSocketClient.this.webSocket.send("{\"type\":\"ping\"}");
                    }
                }, KEEP_ALIVE_INTERVAL_SECONDS, KEEP_ALIVE_INTERVAL_SECONDS, TimeUnit.SECONDS);
            }

            @Override
            public void onMessage(@NotNull WebSocket webSocket, @NotNull String text) {
                if (text.contains("{\"type\":\"pong\"}"))
                    System.out.println("PONG");
                else
                    messageConsumer.accept(text);
            }

            @Override
            public void onClosing(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
                webSocket.close(1000, null);
                if (clientStub.getMessagePrinter() != null)
                    clientStub.getMessagePrinter().accept("Disconnected websocket client");
                scheduler.shutdown();
            }

            @Override
            public void onFailure(@NotNull WebSocket webSocket, @NotNull Throwable t, Response response) {
                ClientStub.logger.error("WebSocket error", t);
                scheduler.shutdown();
            }
        });

    }

    public void disconnect() {
        if (webSocket != null) {
            webSocket.close(1000, "Client disconnect");
        }
        if (client != null) {
            client.dispatcher().executorService().shutdown();
        }
        scheduler.shutdown();
    }

    public void sendMessage(String message) {
        if (webSocket != null) {
            webSocket.send(message);
        }
    }
}
