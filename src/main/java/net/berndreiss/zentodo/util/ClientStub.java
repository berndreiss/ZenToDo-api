package net.berndreiss.zentodo.util;

import net.berndreiss.zentodo.OperationType;
import net.berndreiss.zentodo.data.OperationHandler;
import net.berndreiss.zentodo.data.ClientOperationHandler;
import net.berndreiss.zentodo.data.Entry;

import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * TODO DESCRIBE
 */
public class ClientStub implements OperationHandler {

    public static String token;

    private final ClientOperationHandler dbHandler;
    private final String user;
    private List<ClientOperationHandler> otherHandlers = null;
    private ExceptionHandler exceptionHandler;
    private HttpClient client;
    public static final String SERVER = "http://localhost:8080/";

    public static void main(String[] args){
        new ClientStub("bd_reiss@gmx.at", new ClientOperationHandler() {
            @Override
            public void updateId(int entry, int id) {

            }

            @Override
            public void setTimeDelay(long delay) {

            }

            @Override
            public void addToQueue(OperationType type, List<Object> arguments) {

            }

            @Override
            public List<ZenMessage> geQueued() {
                return List.of();
            }

            @Override
            public String getToken(String user) {
                return ClientStub.token;
            }

            @Override
            public void setToken(String user, String token) {
                ClientStub.token = token;
            }

            @Override
            public void post(List<Entry> entries) {

            }

            @Override
            public void addNewEntry(int id, String task) {

            }

            @Override
            public void delete(int id) {

            }

            @Override
            public void swapEntries(int id, int position) {

            }

            @Override
            public void swapListEntries(int id, int position) {

            }

            @Override
            public void updateTask(int id, String value) {

            }

            @Override
            public void updateFocus(int id, int value) {

            }

            @Override
            public void updateDropped(int id, int value) {

            }

            @Override
            public void updateList(int id, String value, int position) {

            }

            @Override
            public void updateReminderDate(int id, Long value) {

            }

            @Override
            public void updateRecurrence(int id, Long reminderDate, String value) {

            }

            @Override
            public void updateListColor(String list, String color) {

            }
        }, Throwable::printStackTrace);
    }

    public ClientStub(String user, ClientOperationHandler dbHandler){
        this(user, dbHandler, null, null);
    }
    public ClientStub(String user, ClientOperationHandler dbHandler, ExceptionHandler exceptionHandler){
        this(user, dbHandler, null, exceptionHandler);
    }
    public ClientStub(String user, ClientOperationHandler dbHandler, List<ClientOperationHandler> otherHandlers){
        this(user, dbHandler, otherHandlers, null);
    }
    public ClientStub(String user, ClientOperationHandler dbHandler, List<ClientOperationHandler> otherHandlers, ExceptionHandler exceptionHandler){
        this.user = user;
        this.dbHandler = dbHandler;
        this.otherHandlers = otherHandlers;
        this.exceptionHandler = exceptionHandler;
        client = HttpClient.newBuilder().build();
        try {
            URL url = new URL("https://zentodo.berndreiss.net");
            List<Object> arguments = new ArrayList<>();
            arguments.add(3);
            arguments.add(1);
            arguments.add("Test");


            ZenMessage message = new ZenMessage(OperationType.POST, arguments);
            String jsonInput = jsonify(message);


            System.out.println(jsonInput);
            //HttpRequest request = HttpRequest.newBuilder().uri(URI.create("https://zentodo.berndreiss.net")).GET().build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/auth/register?email=bd_reiss@gmx.at&password=test"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonInput, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println(response);
            System.out.println(response.body());

            String authJson = "{\"email\":\"bd_reiss@gmx.at\", \"password\":\"test\"}";

            request = HttpRequest.newBuilder()
                    .uri(URI.create(SERVER + "auth/login"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", getAuthHeader("bd_reiss@gmx.at", "test"))
                    .POST(HttpRequest.BodyPublishers.ofString(authJson, StandardCharsets.UTF_8))
                    .build();

            response = client.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println(response);
            System.out.println(response.body());

            token = response.body();
            request = HttpRequest.newBuilder().uri(URI.create(SERVER + "add")).header("Content-Type", "application/json").header("Authorization", "Bearer " + token).POST(HttpRequest.BodyPublishers.ofString(jsonInput, StandardCharsets.UTF_8)).build();
            response = client.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println(response);
            System.out.println(response.body());

        } catch (Exception e) {
            exceptionHandler.handle(e);
        }

        //TODO HANDLE REGISTRATION

        //TODO AUTHENTICATE

        //TODO SET CLOCK

        //TODO SEND QUEUE ITEMS TO SERVER

    }

    private String getAuthHeader(String email, String password){

        String encodedAuth = Base64.getEncoder().encodeToString((email + ":" + password).getBytes(StandardCharsets.UTF_8));
        return "Basic " + encodedAuth;
    }
    public String jsonify(ZenMessage message){
        StringBuilder sb = new StringBuilder();

        sb.append("{\n");
        sb.append("  \"type\": \"").append(message.getType()).append("\",\n");
        sb.append("  \"arguments\": [");
        String prefix = "";
        for(Object argument: message.getArguments()) {
            sb.append(prefix);
            prefix = ", ";
            sb.append("\"").append(argument).append("\"");

        }
        sb.append("]");
        sb.append("}");

        return sb.toString();
    }

    public void receiveMessage(ZenMessage message){



    }

    @Override
    public void post(List<Entry> entries) {

    }

    @Override
    public void addNewEntry(int id, String task) {

    }

    @Override
    public void delete(int id) {

    }

    @Override
    public void swapEntries(int id, int position) {

    }

    @Override
    public void swapListEntries(int id, int position) {

    }

    @Override
    public void updateTask(int id, String value) {

    }

    @Override
    public void updateFocus(int id, int value) {

    }

    @Override
    public void updateDropped(int id, int value) {

    }

    @Override
    public void updateList(int id, String value, int position) {

    }

    @Override
    public void updateReminderDate(int id, Long value) {

    }

    @Override
    public void updateRecurrence(int id, Long reminderDate, String value) {

    }

    @Override
    public void updateListColor(String list, String color) {

    }

}
