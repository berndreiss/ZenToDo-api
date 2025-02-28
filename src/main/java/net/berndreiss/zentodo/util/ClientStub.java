package net.berndreiss.zentodo.util;

import net.berndreiss.zentodo.OperationType;
import net.berndreiss.zentodo.data.OperationHandler;
import net.berndreiss.zentodo.data.ClientOperationHandler;
import net.berndreiss.zentodo.data.Entry;
import net.berndreiss.zentodo.data.User;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * TODO DESCRIBE
 */
public class ClientStub implements OperationHandler {

    private ZenWebSocketClient client;

    private final String email;
    private final String userName;
    private final ClientOperationHandler dbHandler;
    private List<ClientOperationHandler> otherHandlers = null;
    private ExceptionHandler exceptionHandler;
    private Consumer<String> messagePrinter;
    public static String PROTOCOL = "http://";
    public static String SERVER = "10.0.0.6:8080/";
    private  User user;
    public Status status;
    public static TimeDrift timeDrift = new TimeDrift();

    public static void main(String[] args) throws IOException, InterruptedException, URISyntaxException {

        TestDbHandler dbTestHandler = new TestDbHandler("ZenToDoPU");

        Supplier<String> passwordSupplier = () -> {
            System.out.println("PLEASE ENTER PASSWORD:");
            try (Scanner scanner = new Scanner(System.in)){
                return scanner.nextLine();
            }
        };

        passwordSupplier = () -> "test";

        ClientStub stub = new ClientStub("bd_reiss@yahoo.de", dbTestHandler);
        stub.setExceptionHandler(Throwable::printStackTrace);
        stub.setMessagePrinter(System.out::println);
        stub.authenticate(passwordSupplier);
        stub.init();

        //System.out.println("ADDING");
        //stub.addNewEntry(1, "TEST", 3L, 0);
        //dbTestHandler.close();

    }

    public ClientStub(String email, ClientOperationHandler dbHandler){
        this(email, null, dbHandler);
    }
    public ClientStub(String email, String userName, ClientOperationHandler dbHandler){
        this.email = email;
        this.userName = userName;
        this.dbHandler = dbHandler;
    }

    /**
     * TODO DESCRIBE
     * @param passwordSupplier
     * @return
     */
    public Status authenticate(Supplier<String> passwordSupplier){
        try {
            user = dbHandler.getUserByEmail(email);
            if (user == null) {
                String loginRequest = getLoginRequest(email, passwordSupplier.get());

                int attempts = 0;
                while (attempts++ < 10){

                    HttpURLConnection connection = sendPostMessage(PROTOCOL + SERVER + "auth/register", loginRequest);
                    if (connection.getResponseCode() == 200){

                        String body = getBody(connection);

                        if (body.equals("exists")){
                            if (messagePrinter != null)
                                messagePrinter.accept("User is already registered, but not verified. Check your mail.");
                            return Status.REGISTERED;
                        }


                        String[] ids  = body.split(",");

                        if (Integer.parseInt(ids[0]) == 0) {
                            user = dbHandler.addUser(Long.parseLong(ids[1]), email, userName, Long.parseLong(ids[2]));
                            if (messagePrinter != null)
                                messagePrinter.accept("User was registered. Check your mails for verification.");

                            return Status.REGISTERED;
                        }
                        if (Integer.parseInt(ids[0]) == 1) {
                            user = dbHandler.addUser(Long.parseLong(ids[1]), email, userName, Long.parseLong(ids[2]));
                            dbHandler.enableUser(email);
                            user.setEnabled(true);
                            dbHandler.setToken(Long.parseLong(ids[1]), ids[3]);
                            if (messagePrinter != null)
                                 messagePrinter.accept("User logged in.");
                            return Status.ENABLED;
                        }

                        throw new Exception("User could not be registered.");
                    }
                }
            }

            if (!user.getEnabled()){
                String password = passwordSupplier.get();
                String loginRequest = getLoginRequest(email, password);
                HttpURLConnection connection = sendPostMessage(PROTOCOL + SERVER + "auth/status", loginRequest);
                String body = getBody(connection);

                if (body.equals("non")){
                    dbHandler.removeUser(user.getId());
                    return Status.DELETED;
                }

                if (body.equals("exists")) {
                    if (messagePrinter != null)
                        messagePrinter.accept("User already exists, but was not verified. Check your mails.");
                    return Status.REGISTERED;
                }
                else if (body.startsWith("enabled")){
                    user.setEnabled(true);
                    dbHandler.enableUser(email);
                    dbHandler.setToken(user.getId(), body.split(",")[1]);
                    //TODO retrieve complete server side db
                    if (messagePrinter != null)
                        messagePrinter.accept("User logged in.");
                    return Status.ENABLED;
                } else {
                    if (messagePrinter != null)
                        messagePrinter.accept("Something went wrong when retrieving the status of the user from the server.");
                    return Status.ERROR;
                }

            }
            String token = dbHandler.getToken(user.getId());

            if (token != null) {
                HttpURLConnection connection = sendPostMessage(PROTOCOL + SERVER + "auth/renewToken", getLoginRequest(email, token));
                if (connection.getResponseCode() == 200) {
                    dbHandler.setToken(user.getId(), getBody(connection));
                    if (messagePrinter != null)
                        messagePrinter.accept("User logged in.");
                    return Status.ENABLED;
                }
            }

            String loginRequest = getLoginRequest(email, passwordSupplier.get());

            int attempts = 0;
            while (attempts++ < 10) {
                HttpURLConnection connection = sendPostMessage(PROTOCOL + SERVER + "auth/login", loginRequest);
                if (connection.getResponseCode() == 200) {
                    dbHandler.setToken(user.getId(), getBody(connection));
                    if (messagePrinter != null)
                        messagePrinter.accept("User logged in.");
                    return Status.ENABLED;
                }
            }
            throw new Exception("Was not able to login.");
        } catch (Exception e) {
            if (exceptionHandler != null)
                exceptionHandler.handle(e);
            if (user == null && messagePrinter != null)
                messagePrinter.accept("There was a problem connecting to the server. Please try again later.");
            else
                if (messagePrinter != null)
                    messagePrinter.accept("Something went wrong. Try logging in later.");
            return Status.ERROR;
        }
    }

    /**
     * TODO
     */
    public void init() {

        try {

            if (user == null)
                throw new Exception("User does not exist. Authenticate first!");

            if (!user.getEnabled())
                throw new Exception("User is not enabled. Make sure, the user is verified.");

            // Set clock drift
            for (int i = 0; i < 8; i++) {
                sendAuthGetMessage(PROTOCOL + SERVER + "test");
            }

            //TODO SEND QUEUE ITEMS TO SERVER

            //TODO LISTEN FOR EVENTS

            List<Object> arguments = new ArrayList<>();
            arguments.add(3);
            arguments.add(1);
            arguments.add("Test");
            arguments.add(4);
            ZenServerMessage message = new ZenServerMessage(OperationType.POST, arguments);
            ZenServerMessage message1 = new ZenServerMessage(OperationType.POST, arguments);
            List<ZenMessage> list = new ArrayList<>();

            list.add(message);
            list.add(message1);
            //HttpURLConnection connection = sendAuthPostMessage(PROTOCOL + SERVER + "add", jsonifyList(list));


            client = new ZenWebSocketClient(rawMessage -> {

                    String id = getId(rawMessage);
                    JSONArray parsedMessage  = getMessage(rawMessage);

                try {
                    HttpURLConnection conn = sendAuthPostMessage(PROTOCOL + SERVER + "ackn", "{" +
                            "\"id\": \"" + id + "\"," +
                            "\"email\": \"" + email + "\"," +
                            "\"device\": \"" + user.getDevice() + "\"" +
                            "}");
                    if (conn.getResponseCode() != 200)
                        return;
                } catch (IOException | InterruptedException | URISyntaxException e) {
                    throw new RuntimeException(e);
                }

                for (ZenMessage zm: parseMessage(parsedMessage))
                        receiveMessage(zm);
            });
            Thread thread = new Thread(() -> client.connect(user.getEmail(), dbHandler.getToken(user.getId()), user.getDevice()));
            thread.start();

        } catch (Exception e){
            if (exceptionHandler != null)
                exceptionHandler.handle(e);
        }

    }

    public static String getBody(HttpURLConnection connection) throws IOException {

        BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String inputLine;
        StringBuilder response = new StringBuilder();

        while ((inputLine = br.readLine())!=null)
            response.append(inputLine);

        return response.toString();
    }
    //TODO COMMENT
    private  HttpURLConnection sendPostMessage(String urlString, String body) throws IOException, URISyntaxException {

        URL url = new URI(urlString).toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("t1", TimeDrift.getTimeStamp());
        connection.setDoOutput(true);

        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = body.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }
        processResponse(connection);
        return connection;
    }

    //TODO
    private HttpURLConnection sendAuthPostMessage(String urlString, String body) throws IOException, InterruptedException, URISyntaxException {

        URL url = new URI(urlString).toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Authorization", "Bearer " + dbHandler.getToken(user.getId()));
        connection.setRequestProperty("device", String.valueOf(user.getDevice()));
        connection.setRequestProperty("t1", TimeDrift.getTimeStamp());
        connection.setDoOutput(true);

        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = body.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }
        processResponse(connection);
        return connection;
    }
    //TODO
    private HttpURLConnection sendAuthGetMessage(String urlString) throws IOException, InterruptedException, URISyntaxException {

        URL url = new URI(urlString).toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("GET");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Authorization", "Bearer " + dbHandler.getToken(user.getId()));
        connection.setRequestProperty("device", String.valueOf(user.getDevice()));
        connection.setRequestProperty("t1", TimeDrift.getTimeStamp());
        connection.setDoOutput(true);

        processResponse(connection);
        return connection;
    }

    //TODO
    private void processResponse(HttpURLConnection connection){
        try {
            Instant t4 = TimeDrift.parseTimeStamp(TimeDrift.getTimeStamp());
            Instant t1 = TimeDrift.parseTimeStamp(connection.getHeaderField("t1"));
            Instant t2 = TimeDrift.parseTimeStamp(connection.getHeaderField("t2"));
            Instant t3 = TimeDrift.parseTimeStamp(connection.getHeaderField("t3"));

            if (t1 == null || t2 == null || t3 == null)
                return;

            TimeDrift td = new TimeDrift(t1, t2, t3, t4);

            if (timeDrift == null || td.compareTo(timeDrift) < 0)
                timeDrift = td;

        } catch (DateTimeParseException ignored){}

    }

    /**
     *
     */
    public void sync(){
       List<ZenServerMessage> queue = dbHandler.getQueued(user.getId());
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

    /**
     * //TODO
     * @param messagePrinter
     */
    public void setMessagePrinter(Consumer<String> messagePrinter){
        this.messagePrinter = messagePrinter;
    }

    /**
     *
     * @return
     */
    public User getUser(){return user;}

    //TODO DESCRIBE
    private static String getAuthHeader(String email, String password){

        String encodedAuth = Base64.getEncoder().encodeToString((email + ":" + password).getBytes(StandardCharsets.UTF_8));
        return "Basic " + encodedAuth;
    }

    //TODO DESCRIBE
    private static String getLoginRequest(String email, String password){
        return "{\"email\": \"" + email + "\", \"password\": \"" + password + "\"}";
    }

    public static String jsonifyMessage(ZenMessage message){
        return jsonifyMessage(message, "");
    }
    /**
     * TODO DESCRIBE
     * @param message
     * @return
     */
    public static String jsonifyMessage(ZenMessage message, String whitespace){
        StringBuilder sb = new StringBuilder();

        sb.append(whitespace).append("{\n");
        sb.append(whitespace).append("  \"type\": \"").append(message.getType()).append("\",\n");
        sb.append(whitespace).append("  \"arguments\": [");
        String prefix = "";
        for(Object argument: message.getArguments()) {
            sb.append(prefix);
            prefix = ", ";
            sb.append("\"").append(argument).append("\"");

        }
        sb.append("]");
        if (message instanceof ZenServerMessage){
            sb.append(",\n");
            sb.append(whitespace).append("  \"timestamp\": \"").append(((ZenServerMessage) message).getTimeStamp().toString()).append("\"");
        }
        sb.append("\n");
        sb.append(whitespace).append("}");

        return sb.toString();
    }

    /**
     * TODO
     * @param list
     * @return
     */
    public static String jsonifyList(List<ZenMessage> list){
        StringBuilder sb = new StringBuilder();

        sb.append("[");

        String prefix = "\n";
        for (ZenMessage message: list){
            sb.append(prefix);
            prefix = ",\n";
            sb.append(jsonifyMessage(message, "  "));
        }

        sb.append("\n]");

        return sb.toString();
    }

    public static String jsonifyServerList(List<ZenServerMessage> list){
        StringBuilder sb = new StringBuilder();

        sb.append("[");

        String prefix = "\n";
        for (ZenMessage message: list){
            sb.append(prefix);
            prefix = ",\n";
            sb.append(jsonifyMessage(message, "  "));
        }

        sb.append("\n]");

        return sb.toString();
    }

    public static List<ZenMessage> parseMessage(JSONArray jsonArray) {


        List<ZenMessage> list = new ArrayList<>();

        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject obj = jsonArray.getJSONObject(i);

            // Convert "type" field to Enum
            OperationType type = OperationType.valueOf(obj.getString("type"));

            // Extract arguments array
            JSONArray argumentsObj = obj.getJSONArray("arguments");
            List<Object> arguments = new ArrayList<>();

            for (int j = 0; j < argumentsObj.length(); j++) {
                arguments.add(argumentsObj.get(j)); // Extracting JSON values as generic Object
            }

            // Create ZenMessage instance and add it to the list
            list.add(new ZenMessage(type, arguments));
        }
        return list;
    }

    public static String getId(String rawMessage){
        JSONObject obj = new JSONObject(rawMessage);
        return obj.getString("id");
    }
    public static JSONArray getMessage(String rawMessage){
        JSONObject obj = new JSONObject(rawMessage);
        return (JSONArray) obj.get("message");
    }

    private void receiveMessage(ZenMessage message){

        ZenServerMessage zm = new ZenServerMessage(message.getType(), message.getArguments());

        dbHandler.addToQueue(user.getId(), zm);
        System.out.println(message.getType());

        System.out.println(message.getArguments().get(1));
        switch (message.getType()){
            case POST -> {}
            case ADD_NEW_ENTRY -> {}
            case DELETE -> {}
            case SWAP -> {}
            case SWAP_LIST -> {}
            case UPDATE_LIST -> {}
            case UPDATE_TASK -> {}
            case UPDATE_FOCUS -> {}
            case UPDATE_DROPPED -> {}
            case UPDATE_RECURRENCE -> {}
            case UPDATE_REMINDER_DATE -> {}
            case UPDATE_LIST_COLOR -> {}
            case UPDATE_MAIL -> {}
            case UPDATE_USER_NAME -> {}
        }
    }

    @Override
    public void addNewEntry(long id, String task, Long userId, int position) {

        List<Object> arguments = new ArrayList<>();
        arguments.add(id);
        arguments.add(task);
        arguments.add(userId);
        arguments.add(position);
        ZenServerMessage zm = new ZenServerMessage(OperationType.ADD_NEW_ENTRY, arguments);
        List<ZenServerMessage> list = new ArrayList<>();
        list.add(zm);
        try {
            HttpURLConnection connection = sendAuthPostMessage(PROTOCOL + SERVER + "process", jsonifyServerList(list));

            if (connection.getResponseCode() != 200) {
                dbHandler.addToQueue(user.getId(), zm);
            }
        } catch (IOException | URISyntaxException | InterruptedException e) {
            dbHandler.addToQueue(user.getId(), zm);
        }

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

    @Override
    public void updateUserName(long id, String name) {

    }

    @Override
    public boolean updateEmail(long id, String email) {

        return true;
    }

}
