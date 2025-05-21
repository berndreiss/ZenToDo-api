package net.berndreiss.zentodo.util;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import net.berndreiss.zentodo.OperationType;
import net.berndreiss.zentodo.persistence.DbHandler;
import net.berndreiss.zentodo.data.*;
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
import java.util.Optional;

/**
 * TODO DESCRIBE
 */
public class ClientStub implements OperationHandlerI {

    private ZenWebSocketClient client;

    public final Database dbHandler;
    private final List<ClientOperationHandlerI> otherHandlers = new ArrayList<>();
    private ExceptionHandler exceptionHandler;
    private Consumer<String> messagePrinter;
    public static String PROTOCOL = "http://";
    public static String SERVER = "localhost:8080/";
    public  User user;
    public int profile;
    public Status status;
    public String tokenDirectory;
    private VectorClock vectorClock;
    public static TimeDrift timeDrift = new TimeDrift();

    public static void  main(String[] args) throws InterruptedException {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("ZenToDoPU1");
        Database dbHandler = new DbHandler(emf, null);
        ClientStub stub = new ClientStub(dbHandler);

        stub.init("test@test.net", null, () -> "Test1234!?");

        dbHandler.close();
        emf.close();
    }
    public ClientStub(String email, String userName, Supplier<String> passwordSupplier, Database dbHandler){
        this.dbHandler = dbHandler;
        init(email, userName, passwordSupplier);
    }
    public ClientStub(Database dbHandler){
        this.dbHandler = dbHandler;
        Optional<User> user = dbHandler.getUserManager().getUser(0);
        if (user.isEmpty())
            throw new RuntimeException("No default user has been found.");
        this.user = user.get();
    }

    /**
     * TODO DESCRIBE
     * @param passwordSupplier
     * @return
     */
    private Status authenticate(String email, String userName, Supplier<String> passwordSupplier){
        try {
            UserManagerI userManager = dbHandler.getUserManager();
            Optional<User> userOpt = userManager.getUserByEmail(email);

            userOpt.ifPresent(value -> user = value);

            if (userOpt.isEmpty()) {
                String loginRequest = getLoginRequest(email, passwordSupplier.get());

                int attempts = 0;
                while (attempts++ < 10){

                    HttpURLConnection connection = sendPostMessage(PROTOCOL + SERVER + "auth/register", loginRequest);
                    int responseCode = connection.getResponseCode();
                    if (responseCode == 200){

                        String body = getBody(connection);

                        if (body.startsWith("exists")){

                            String[] ids = body.split(",");
                            userManager.addUser(Long.parseLong(ids[1]), email, userName, Integer.parseInt(ids[2]));

                            if (messagePrinter != null)
                                messagePrinter.accept("User is already registered, but not verified. Check your mail.");
                            return Status.REGISTERED;
                        }


                        String[] ids  = body.split(",");

                        if (Integer.parseInt(ids[0]) == 0) {
                            user = userManager.addUser(Long.parseLong(ids[1]), email, userName, Integer.parseInt(ids[2]));
                            if (messagePrinter != null)
                                messagePrinter.accept("User was registered. Check your mails for verification.");

                            return Status.REGISTERED;
                        }
                        if (Integer.parseInt(ids[0]) == 1) {
                            user = userManager.addUser(Long.parseLong(ids[1]), email, userName, Integer.parseInt(ids[2]));
                            userManager.enableUser(user.getId());
                            user.setEnabled(true);
                            userManager.setToken(Long.parseLong(ids[1]), ids[3]);
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
                    //TODO ASK WHEHTER USER SHOULD BE DELETED!
                    userManager.removeUser(user.getId());
                    return Status.DELETED;
                }

                if (body.equals("exists")) {
                    if (messagePrinter != null)
                        messagePrinter.accept("User already exists, but was not verified. Check your mails.");
                    return Status.REGISTERED;
                }
                else if (body.startsWith("enabled")){
                    user.setEnabled(true);
                    userManager.enableUser(user.getId());
                    userManager.setToken(user.getId(), body.split(",")[1]);
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
            String token = userManager.getToken(user.getId());

            if (token != null) {
                HttpURLConnection connection = sendPostMessage(PROTOCOL + SERVER + "auth/renewToken", getLoginRequest(email, token));
                if (connection.getResponseCode() == 200) {
                    userManager.setToken(user.getId(), getBody(connection));
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
                    userManager.setToken(user.getId(), getBody(connection));
                    if (messagePrinter != null)
                        messagePrinter.accept("User logged in.");
                    return Status.ENABLED;
                }
                if (connection.getResponseCode() == 404) {
                    userManager.removeUser(user.getId());
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
    public void init(String email, String name, Supplier<String> passwordSupplier) {

        try {

            //Consumer<String> oldMessagePrinter = messagePrinter;
            //messagePrinter = null;
            status= authenticate(email, name, passwordSupplier);
            //messagePrinter = oldMessagePrinter;

            if (user.getId() == 0)
                return;


            //initialize Vector clock
            vectorClock = new VectorClock(user);


            if (status != Status.ENABLED) {
                //TODO HANDLE
                return;
            }

            HttpURLConnection connection = sendAuthPostMessage(PROTOCOL + SERVER + "process", jsonifyServerList(dbHandler.getUserManager().getQueued(user.getId())));

            if (connection.getResponseCode() != 200){
                throw new Exception("Something went wrong sending data to server.");
            }


            client = new ZenWebSocketClient(rawMessage -> {

                String id = getId(rawMessage);
                JSONArray parsedMessage  = getMessage(rawMessage);

                List<ZenMessage> messages = parseMessage(parsedMessage);

                if (!messages.isEmpty()) {
                /*
                if (!messages.isEmpty() && messages.getFirst().clock.changeDifference(vectorClock ) != 1){

                    try {
                        HttpURLConnection conn = sendAuthPostMessage(ClientStub.PROTOCOL + ClientStub.SERVER + "queue", " ");
                        if (conn.getResponseCode() != 200)
                            throw new RuntimeException("Could not retrieve data from server.");
                        //return;
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
*/
                }

                try {
                    HttpURLConnection conn = sendAuthPostMessage(PROTOCOL + SERVER + "ackn", id);
                    if (conn.getResponseCode() != 200)
                        return;
                } catch (Exception e) {
                    if (exceptionHandler != null)
                        exceptionHandler.handle(e);

                }

                for (ZenMessage zm: messages)
                    receiveMessage(zm);
            }, this);
            Thread thread = new Thread(() -> client.connect(user.getEmail(), dbHandler.getUserManager().getToken(user.getId()), user.getDevice()));
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
    public  HttpURLConnection sendPostMessage(String urlString, String body) throws IOException, URISyntaxException {

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
    public synchronized HttpURLConnection sendAuthPostMessage(String urlString, String body) throws Exception {

        if (status == Status.DIRTY) {
            status = Status.UPDATED;
            HttpURLConnection connection = sendAuthPostMessage(PROTOCOL + SERVER + "process", jsonifyServerList(dbHandler.getUserManager().getQueued(user.getId())));

            if (connection.getResponseCode() != 200) {
                status = Status.DIRTY;
                throw new Exception("Something went wrong sending data to server.");
            }
        }

        URL url = new URI(urlString).toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Authorization", "Bearer " + dbHandler.getUserManager().getToken(user.getId()));
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
    public synchronized HttpURLConnection sendAuthGetMessageBU(String urlString) throws Exception {

        if (status == Status.DIRTY) {
            status = Status.UPDATED;
            HttpURLConnection connection = sendAuthPostMessage(PROTOCOL + SERVER + "process", jsonifyServerList(dbHandler.getUserManager().getQueued(user.getId())));

            if (connection.getResponseCode() != 200) {
                status = Status.DIRTY;
                throw new Exception("Something went wrong sending data to server.");
            }
        }

        URL url = new URI(urlString).toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("GET");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Authorization", "Bearer " + dbHandler.getUserManager().getToken(user.getId()));
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
       List<ZenServerMessage> queue = dbHandler.getUserManager().getQueued(user.getId());
    }

    /**
     * TODO DESCRIBE
     * @param operationHandler
     */
    public void addOperationHandler(ClientOperationHandlerI operationHandler){
        this.otherHandlers.add(operationHandler);
    }

    /**
     * TODO DESCRIBE
     * @param operationHandler
     */
    public void removeOperationHandler(ClientOperationHandlerI operationHandler){
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

        System.out.println(message);

        switch (message.type){
            case POST -> {}
            case ADD_NEW_ENTRY -> {
                List<Object> args = message.arguments;
                long userId = Long.parseLong(args.get(0).toString());
                int profile = Integer.parseInt(args.get(1).toString());
                long id = Long.parseLong(args.get(2).toString());
                String task = args.get(3).toString();
                int position = Integer.parseInt(args.get(4).toString());
                Entry entry = null;
                try {
                        entry =dbHandler.getEntryManager().addNewEntry(userId, id, task, position);
                } catch (PositionOutOfBoundException e){

                }
                Entry finalEntry = entry;
                otherHandlers.forEach(handler ->{
                        handler.addNewEntry(finalEntry);
                });
            }
            case DELETE -> {
                long id = Long.parseLong(message.arguments.getFirst().toString());
                dbHandler.getEntryManager().removeEntry(user.getId(), user.getProfile(), id);
                otherHandlers.forEach(h -> h.removeEntry(id));
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

    }

    @Override
    public Entry addNewEntry(String task) {
        Entry entry = dbHandler.getEntryManager().addNewEntry(user == null ? 0 : user.getId(), user == null ? profile : user.getProfile(), task);
        return entry == null ? null : addNewEntry(entry);
    }
    @Override
    public Entry addNewEntry(String task, int position) throws PositionOutOfBoundException {
        Entry entry = dbHandler.getEntryManager().addNewEntry(user == null ? 0 : user.getId(), user == null ? profile : user.getProfile(), task, position);
        return addNewEntry(entry);
    }

    @Override
    public Entry addNewEntry(Entry entry) {

        vectorClock.increment();
        dbHandler.getUserManager().setClock(user.getId(), vectorClock);
        //messagePrinter.accept(String.valueOf(vectorClock == null));
        List<Object> arguments = new ArrayList<>();
        arguments.add(user.getId());
        arguments.add(user.getProfile());
        arguments.add(entry.getId());
        arguments.add(entry.getTask());
        arguments.add(entry.getPosition());
        ZenServerMessage zm = new ZenServerMessage(OperationType.ADD_NEW_ENTRY, arguments, vectorClock);
        List<ZenServerMessage> list = new ArrayList<>();
        list.add(zm);
        try {
            if (status != Status.UPDATED)
                throw new Exception("Updates haven't been processed yet.");

            HttpURLConnection connection = sendAuthPostMessage(PROTOCOL + SERVER + "process", jsonifyServerList(list));

            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                dbHandler.getUserManager().addToQueue(user, zm);
            }
        } catch (Exception e) {
            exceptionHandler.handle(e);
            dbHandler.getUserManager().addToQueue(user, zm);
        }
        return entry;

    }

    @Override
    public void removeEntry(long id) {
        dbHandler.getEntryManager().removeEntry(user == null ? 0 : user.getId(), user == null ? profile : user.getProfile(), id);
    }

    @Override
    public Optional<Entry> getEntry(long id) {
        return dbHandler.getEntryManager().getEntry(user == null ? 0 : user.getId(), user == null ? profile : user.getProfile(), id);
    }

    @Override
    public List<Entry> loadEntries() {
        return dbHandler.getEntryManager().getEntries(user == null ? 0 : user.getId(), user == null ? profile : user.getProfile());
    }

    @Override
    public List<Entry> loadFocus() {
        return new ArrayList<Entry>();
    }

    @Override
    public List<Entry> loadDropped() {
        return new ArrayList<Entry>();
    }

    @Override
    public List<Entry> loadList(Long list) {
        return new ArrayList<Entry>();
    }

    @Override
    public List<TaskList> loadLists() {
        return new ArrayList<TaskList>();
    }

    @Override
    public Map<Long, String> getListColors() {
        return new  HashMap<Long, String>();
    }

    @Override
    public Optional<TaskList> getListByName(String name) {
        return Optional.empty();
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
    public void updateListColor(long list, String color) {

    }

    @Override
    public void updateUserName(long id, String name) {

    }

    @Override
    public boolean updateEmail(long id, String email) {

        return true;
    }

    public void clearQueue() {
        dbHandler.getUserManager().clearQueue(user.getId());
    }
}
