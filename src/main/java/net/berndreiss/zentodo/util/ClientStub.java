package net.berndreiss.zentodo.util;

import net.berndreiss.zentodo.data.OperationHandler;
import net.berndreiss.zentodo.data.ClientOperationHandler;
import net.berndreiss.zentodo.data.Entry;
import net.berndreiss.zentodo.data.User;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.function.Supplier;

/**
 * TODO DESCRIBE
 */
public class ClientStub implements OperationHandler {

    private final String email;
    private final String userName;
    private final ClientOperationHandler dbHandler;
    private List<ClientOperationHandler> otherHandlers = null;
    private ExceptionHandler exceptionHandler;
    private final HttpClient client;
    public static String SERVER = "http://localhost:8080/";
    private  User user;
    private TimeDrift timeDrift;

    public static void main(String[] args) throws IOException, InterruptedException {

        TestDbHandler dbTestHandler = new TestDbHandler("ZenToDoPU");

        ClientStub stub = new ClientStub("bd_reiss@yahoo.de", dbTestHandler);
        stub.setExceptionHandler(Throwable::printStackTrace);
        System.out.println(stub.authenticate(() ->{
            System.out.println("PLEASE ENTER PASSWORD:");
            try (Scanner scanner = new Scanner(System.in)){
                return scanner.nextLine();
            }
        }));
        stub.init();
        dbTestHandler.close();
    }

    public ClientStub(String email, ClientOperationHandler dbHandler){
        this(email, null, dbHandler);
    }
    public ClientStub(String email, String userName, ClientOperationHandler dbHandler){
        this.email = email;
        this.userName = userName;
        this.dbHandler = dbHandler;
        client = HttpClient.newBuilder().build();
    }

    /**
     * TODO DESCRIBE
     * @param passwordSupplier
     * @return
     */
    public String authenticate(Supplier<String> passwordSupplier){
        try {
            user = dbHandler.getUserByEmail(email);
            if (user == null || !user.getEnabled()) {
                String loginRequest = getLoginRequest(email, passwordSupplier.get());

                int attempts = 0;
                while (attempts++ < 10){
                    HttpResponse<String> response = sendPostMessage(SERVER + "auth/register", loginRequest);
                    if (response.statusCode() == 200){

                        if (response.body().equals("enabled")){
                            dbHandler.enableUser(email);
                            break;
                        }
                        if (response.body().equals("exists")) {
                            return "User already exists, but was not verified. Check your mails.";
                        }
                        dbHandler.addUser(Long.parseLong(response.body()), email, userName);
                        return "User was registered. Check your mails for verification.";
                    }
                }
            }

            assert user != null;
            String token = dbHandler.getToken(user.getId());

            if (token != null) {
                HttpResponse<String> response = sendPostMessage(SERVER + "auth/renewToken", getLoginRequest(email, token));
                if (response.statusCode() == 200) {
                    dbHandler.setToken(user.getId(), response.body());
                    return "User logged in.";
                }
            }


            String loginRequest = getLoginRequest(email, passwordSupplier.get());

            int attempts = 0;
            while (attempts++ < 10) {
                HttpResponse<String> response = sendPostMessage(SERVER + "auth/login", loginRequest);
                if (response.statusCode() == 200) {
                    dbHandler.setToken(user.getId(), response.body());
                    return "User logged in.";
                }
            }


            return "Something went wrong. Try logging in later.";

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
            if (exceptionHandler != null)
                exceptionHandler.handle(e);
            return "Something went wrong. Try logging in later.";
        }
    }

    /**
     * TODO
     */
    public void init() throws IOException, InterruptedException {

        // Set clock drift
        for (int i = 0; i < 8; i++)
            sendAuthGetMessage(SERVER + "test");

        //TODO SEND QUEUE ITEMS TO SERVER

    }

    //TODO COMMENT
    private HttpResponse<String> sendPostMessage(String uri, String body) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .header("Content-Type", "application/json")
                .header("t1", TimeDrift.getTimeStamp())
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        processResponse(response);
        return response;
    }

    //TODO
    private HttpResponse<String> sendAuthGetMessage(String uri) throws IOException, InterruptedException {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + dbHandler.getToken(user.getId()))
                .header("t1", TimeDrift.getTimeStamp())
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        processResponse(response);
        return response;
    }

    //TODO
    private void processResponse(HttpResponse<String> response){
        try {
            Instant t4 = TimeDrift.parseTimeStamp(TimeDrift.getTimeStamp());
            Instant t1 = TimeDrift.parseTimeStamp(response.headers().firstValue("t1").orElse(""));
            Instant t2 = TimeDrift.parseTimeStamp(response.headers().firstValue("t2").orElse(""));
            Instant t3 = TimeDrift.parseTimeStamp(response.headers().firstValue("t3").orElse(""));
            TimeDrift td = new TimeDrift(t1, t2, t3, t4);

            if (timeDrift == null || td.compareTo(timeDrift) < 0)
                timeDrift = td;

        } catch (DateTimeParseException ignored){}

    }

    /**
     * TODO DESCRIBE
     * @param operationHandler
     */
    public void addOperationHandler(ClientOperationHandler operationHandler){
        this.otherHandlers.add(operationHandler);
    }

    /**
     * TODO DESCRIBE
     * @param operationHandler
     */
    public void removeOperationHandler(ClientOperationHandler operationHandler){
        this.otherHandlers.remove(operationHandler);
    }

    /**
     * TODO DESCRIBE
     * @param exceptionHandler
     */
    public void setExceptionHandler(ExceptionHandler exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
    }

    //TODO DESCRIBE
    private String getAuthHeader(String email, String password){

        String encodedAuth = Base64.getEncoder().encodeToString((email + ":" + password).getBytes(StandardCharsets.UTF_8));
        return "Basic " + encodedAuth;
    }

    //TODO DESCRIBE
    private String getLoginRequest(String email, String password){
        return "{\"email\": \"" + email + "\", \"password\": \"" + password + "\"}";
    }

    /**
     * TODO DESCRIBE
     * @param message
     * @return
     */
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
