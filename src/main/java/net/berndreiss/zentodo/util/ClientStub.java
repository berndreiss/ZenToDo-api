package net.berndreiss.zentodo.util;

import jakarta.persistence.criteria.CriteriaBuilder;
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
    private final List<ClientOperationHandler> otherHandlers = new ArrayList<>();
    private ExceptionHandler exceptionHandler;
    private Consumer<String> messagePrinter;
    public static String PROTOCOL = "http://";
    public static String SERVER = "10.0.0.6:8080/";
    private  User user;
    public Status status;
    private VectorClock vectorClock;
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
        stub.init(passwordSupplier);
        //stub.addNewEntry(7L, "TEST", 0L, 4);


        VectorClock vc1 = new VectorClock();

        vc1.entries.put(0L, 0L);
        vc1.entries.put(1L, 1L);
        vc1.entries.put(2L, 2L);


        VectorClock vc2 = new VectorClock();

        vc2.entries.put(0L, 1000L);
        vc2.entries.put(1L, 0L);
        vc2.entries.put(2L, 0L);




        List<Object> entries = new ArrayList<>();
        entries.add(0L);
        entries.add("TEST");
        entries.add(0);
        entries.add(0L);


        ZenMessage zm = new ZenMessage(OperationType.ADD_NEW_ENTRY, entries, stub.vectorClock);



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
    private Status authenticate(Supplier<String> passwordSupplier){
        try {
            user = dbHandler.getUserByEmail(email);
            System.out.println(user == null);
            if (user == null) {
                String loginRequest = getLoginRequest(email, passwordSupplier.get());

                int attempts = 0;
                while (attempts++ < 10){

                    HttpURLConnection connection = sendPostMessage(PROTOCOL + SERVER + "auth/register", loginRequest);
                    if (connection.getResponseCode() == 200){

                        String body = getBody(connection);

                        if (body.startsWith("exists")){

                            String[] ids = body.split(",");
                            dbHandler.addUser(Long.parseLong(ids[1]), email, userName, Long.parseLong(ids[2]));

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
                if (attempts > 10)
                    throw new Exception("Server not available");
            }

            if (!user.getEnabled()){
                String password = passwordSupplier.get();
                String loginRequest = getLoginRequest(email, password);
                HttpURLConnection connection = sendPostMessage(PROTOCOL + SERVER + "auth/status", loginRequest);
                String body = getBody(connection);

                if (body.equals("non")){
                    dbHandler.removeUser(user.getEmail());
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
                if (connection.getResponseCode() == 404) {
                    dbHandler.removeUser(email);
                    return Status.DELETED;
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
    public void init(Supplier<String> passwordSupplier) {

        try {

            //Consumer<String> oldMessagePrinter = messagePrinter;
            //messagePrinter = null;
            status= authenticate(passwordSupplier);
            //messagePrinter = oldMessagePrinter;

            if (user == null)
                return;


            //initialize Vector clock
            vectorClock = new VectorClock(user);


            if (status != Status.ENABLED) {
                //TODO HANDLE
                return;
            }


            HttpURLConnection connection = sendAuthPostMessage(PROTOCOL + SERVER + "process", jsonifyServerList(dbHandler.getQueued(user.getId())));

            //System.out.println("QUEUE " + dbHandler.getQueued(user.getId()).size());
            //System.out.println(jsonifyServerList(dbHandler.getQueued(user.getId())));

            if (connection.getResponseCode() != 200){
                throw new Exception("Something went wrong sending data to server.");
            }


            client = new ZenWebSocketClient(rawMessage -> {

                String id = getId(rawMessage);
                JSONArray parsedMessage  = getMessage(rawMessage);

                try {
                    HttpURLConnection conn = sendAuthPostMessage(PROTOCOL + SERVER + "ackn", id);
                    if (conn.getResponseCode() != 200)
                        return;
                } catch (IOException | InterruptedException | URISyntaxException e) {
                    throw new RuntimeException(e);
                }

                for (ZenMessage zm: parseMessage(parsedMessage))
                    receiveMessage(zm);
            }, user, dbHandler);
            Thread thread = new Thread(() -> client.connect(user.getEmail(), dbHandler.getToken(user.getId()), user.getDevice()));
            thread.start();
            // Set clock drift
            for (int i = 0; i < 8; i++)
                sendAuthPostMessage(PROTOCOL + SERVER + "test", "");

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
    private HttpURLConnection sendAuthGetMessageBU(String urlString) throws IOException, InterruptedException, URISyntaxException {

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
        sb.append(whitespace).append("  \"type\": \"").append(message.type).append("\",\n");
        sb.append(whitespace).append("  \"arguments\": [");
        String prefix = "";
        for(Object argument: message.arguments) {
            sb.append(prefix);
            prefix = ", ";
            sb.append("\"").append(argument).append("\"");

        }
        sb.append("]");
        if (message instanceof ZenServerMessage){
            sb.append(",\n");
            sb.append(whitespace).append("  \"timestamp\": \"").append(((ZenServerMessage) message).timeStamp.toString()).append("\"");
        }
        sb.append(",\n");
        sb.append("\"clock\": ").append(message.clock == null ? "null" : message.clock.jsonify()).append("\n");
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

            VectorClock clock = new VectorClock(obj.getJSONObject("clock"));

            // Create ZenMessage instance and add it to the list
            list.add(new ZenMessage(type, arguments,clock));
        }
        return list;
    }

    public static String getId(String rawMessage){
        JSONObject obj = new JSONObject(rawMessage);
        return obj.get("id").toString();
    }
    public static JSONArray getMessage(String rawMessage){
        JSONObject obj = new JSONObject(rawMessage);
        return (JSONArray) obj.get("message");
    }

    private void receiveMessage(ZenMessage message){

        System.out.println(jsonifyMessage(message));
        switch (message.type){
            case POST -> {}
            case ADD_NEW_ENTRY -> {
                List<Object> args = message.arguments;
                dbHandler.addNewEntry(
                        Long.parseLong(args.get(0).toString()),
                        args.get(1).toString(),
                        Long.parseLong(args.get(2).toString()),
                        Integer.parseInt(args.get(3).toString()));
                otherHandlers.forEach(handler ->{
                    handler.addNewEntry(
                            Long.parseLong(args.get(0).toString()),
                            args.get(1).toString(),
                            Long.parseLong(args.get(2).toString()),
                            Integer.parseInt(args.get(3).toString()));
                });
            }
            case DELETE -> {
                long id = Long.parseLong(message.arguments.getFirst().toString());
                dbHandler.delete(id);
                otherHandlers.forEach(h -> h.delete(id));
            }
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

        for (Entry entry: ((TestDbHandler) dbHandler).getAllEntries()) {
            System.out.println(entry.getTask() + entry.getPosition());
        }
        System.out.println();
    }

    @Override
    public Entry addNewEntry(long id, String task, Long userId, int position) {

        System.out.println(user.getEmail());
        vectorClock.increment();
        dbHandler.setClock(email, vectorClock);
        //messagePrinter.accept(String.valueOf(vectorClock == null));
        List<Object> arguments = new ArrayList<>();
        arguments.add(id);
        arguments.add(task);
        arguments.add(userId);
        arguments.add(position);
        ZenServerMessage zm = new ZenServerMessage(OperationType.ADD_NEW_ENTRY, arguments, vectorClock);
        List<ZenServerMessage> list = new ArrayList<>();
        list.add(zm);
        try {
            HttpURLConnection connection = sendAuthPostMessage(PROTOCOL + SERVER + "process", jsonifyServerList(list));

            if (connection.getResponseCode() != 200) {
                messagePrinter.accept(String.valueOf(connection.getResponseCode()));
                dbHandler.addToQueue(user, zm);
            }
        } catch (Exception e) {
            dbHandler.addToQueue(user, zm);
        }
        return null;

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
