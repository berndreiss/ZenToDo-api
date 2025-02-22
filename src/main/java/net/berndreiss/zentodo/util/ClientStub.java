package net.berndreiss.zentodo.util;

import net.berndreiss.zentodo.OperationType;
import net.berndreiss.zentodo.data.OperationField;
import net.berndreiss.zentodo.data.OperationHandler;
import net.berndreiss.zentodo.data.ClientOperationHandler;
import net.berndreiss.zentodo.data.Entry;

import java.io.IOException;
import java.net.MalformedURLException;
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

    private List<ClientOperationHandler> operationHandlers;
    private ExceptionHandler exceptionHandler;
    private HttpClient client;


    public static void main(String[] args){
        new ClientStub(null, Throwable::printStackTrace);
    }

    public ClientStub(List<ClientOperationHandler> operationHandlers, ExceptionHandler exceptionHandler){
        this.operationHandlers = operationHandlers;
        this.exceptionHandler = exceptionHandler;
        client = HttpClient.newBuilder().build();
        try {
            URL url = new URL("https://zentodo.berndreiss.net");
            List<Object> arguments = new ArrayList<>();
            arguments.add("Test");
            //arguments.add(1);


            Message message = new Message(OperationType.POST, arguments);
            String jsonInput = jsonify(message);

            jsonInput = "{\"mail\": \"TEST\"}";
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
            request = HttpRequest.newBuilder().uri(URI.create("http://localhost:8080/")).header("Content-Type", "application/json").header("Authorization", getAuthHeader("bd_reiss@gmx.at", "test")).POST(HttpRequest.BodyPublishers.ofString(jsonInput, StandardCharsets.UTF_8)).build();
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
    public String jsonify(Message message){
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

    public void receiveMessage(){



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
