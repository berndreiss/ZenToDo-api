package net.berndreiss.zentodo.util;

import net.berndreiss.zentodo.OperationType;
import net.berndreiss.zentodo.data.OperationHandler;
import net.berndreiss.zentodo.data.ClientOperationHandler;
import net.berndreiss.zentodo.data.Entry;
import net.berndreiss.zentodo.data.User;

import java.net.URI;
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

    private String email;
    private String userName;
    private final ClientOperationHandler dbHandler;
    private List<ClientOperationHandler> otherHandlers = null;
    private ExceptionHandler exceptionHandler;
    private HttpClient client;
    public static final String SERVER = "http://localhost:8080/";

    public static void main(String[] args){

        TestDbHandler dbTestHandler = new TestDbHandler("ZenToDoPU");

        new ClientStub("bd_reiss@gmx.at", dbTestHandler, Throwable::printStackTrace);
        //dbTestHandler.addNewEntry(2L, "Test", null);
        dbTestHandler.close();
    }

    public ClientStub(String email, ClientOperationHandler dbHandler){
        this(email, null, dbHandler, null, null);
    }
    public ClientStub(String email, String userName, ClientOperationHandler dbHandler){
        this(email, userName, dbHandler, null, null);
    }
    public ClientStub(String email, ClientOperationHandler dbHandler, ExceptionHandler exceptionHandler){
        this(email, null, dbHandler, null, exceptionHandler);
    }
    public ClientStub(String email, String userName, ClientOperationHandler dbHandler, ExceptionHandler exceptionHandler){
        this(email, userName, dbHandler, null, exceptionHandler);
    }
    public ClientStub(String email, ClientOperationHandler dbHandler, List<ClientOperationHandler> otherHandlers){
        this(email, null, dbHandler, otherHandlers, null);
    }
    public ClientStub(String email, String userName, ClientOperationHandler dbHandler, List<ClientOperationHandler> otherHandlers){
        this(email, userName, dbHandler, otherHandlers, null);
    }
    public ClientStub(String email, String userName, ClientOperationHandler dbHandler, List<ClientOperationHandler> otherHandlers, ExceptionHandler exceptionHandler){
        this.email = email;
        this.userName = userName;
        this.dbHandler = dbHandler;
        this.otherHandlers = otherHandlers;
        this.exceptionHandler = exceptionHandler;
        client = HttpClient.newBuilder().build();
        try {
            User user = dbHandler.getUserByEmail(email);
            if (user == null || !user.getEnabled()) {
                String loginRequest = getLoginRequest(email, "test");//TODO handle password
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(SERVER +"auth/register"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(loginRequest, StandardCharsets.UTF_8))
                        .build();

                HttpResponse<String> response;
                int attempts = 0;
                while (attempts++ < 10){
                    response = client.send(request, HttpResponse.BodyHandlers.ofString());
                    if (response.statusCode() == 200){

                        if (response.body().equals("enabled")){
                            System.out.println("ENABLE USER.");
                            dbHandler.enableUser(email);
                            break;
                        }
                        if (response.body().equals("exists")) {
                            System.out.println("USER ALREADY EXISTS, BUT WAS NOT VERIFIED. CHECK YOUR MAILS.");
                            return;//TODO HANDLE USER ALREADY EXISTS
                        }
                        dbHandler.addUser(Long.parseLong(response.body()), email, userName);
                        System.out.println("USER WAS REGISTERED, BUT WAS NOT VERIFIED. CHECK YOUR MAILS.");
                        return ;//TODO HANDLE SUCCESS
                    }
                }
            }

            assert user != null;
            String token = dbHandler.getToken(user.getId());

            if (token == null) {
                String loginRequest = getLoginRequest(email, "test");//TODO handle password
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(SERVER + "auth/login"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(loginRequest, StandardCharsets.UTF_8))
                    .build();

                HttpResponse<String> response = null;
                int attempts = 0;
                while (attempts++ < 10) {
                    response = client.send(request, HttpResponse.BodyHandlers.ofString());
                    if (response.statusCode() == 200) {
                        dbHandler.setToken(user.getId(), response.body());
                        System.out.println("RECEIVED A TOKEN AND LOGGED IN.");
                        break;
                    }
                }
            }
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(SERVER + "test")).header("Content-Type", "application/json").header("Authorization", "Bearer " + dbHandler.getToken(user.getId())).GET().build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println(response);

            //TODO SET CLOCK

            //TODO SEND QUEUE ITEMS TO SERVER

            /**
            List<Object> arguments = new ArrayList<>();
            arguments.add(3);
            arguments.add(1);
            arguments.add("Test");
            ZenMessage message = new ZenMessage(OperationType.POST, arguments);
            String jsonInput = jsonify(message);
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(SERVER + "add")).header("Content-Type", "application/json").header("Authorization", "Bearer " + dbHandler.getToken(user.getId())).POST(HttpRequest.BodyPublishers.ofString(jsonInput, StandardCharsets.UTF_8)).build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println(response);
            System.out.println(response.body());
*/
        } catch (Exception e) {
            exceptionHandler.handle(e);
        }


    }

    private String getAuthHeader(String email, String password){

        String encodedAuth = Base64.getEncoder().encodeToString((email + ":" + password).getBytes(StandardCharsets.UTF_8));
        return "Basic " + encodedAuth;
    }

    private String getLoginRequest(String email, String password){
        return "{\"email\": \"" + email + "\", \"password\": \"" + password + "\"}";
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
    public void addNewEntry(long id, String task, Long userId) {

    }

    @Override
    public void delete(long id) {

    }

    @Override
    public void swapEntries(long id, int position) {

    }

    @Override
    public void swapListEntries(long id, int position) {

    }

    @Override
    public void updateTask(long id, String value) {

    }

    @Override
    public void updateFocus(long id, int value) {

    }

    @Override
    public void updateDropped(long id, int value) {

    }

    @Override
    public void updateList(long id, String value, int position) {

    }

    @Override
    public void updateReminderDate(long id, Long value) {

    }

    @Override
    public void updateRecurrence(long id, Long reminderDate, String value) {

    }

    @Override
    public void updateListColor(String list, String color) {

    }

}
