package net.berndreiss.zentodo.util;

import com.sun.istack.NotNull;
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
import java.util.stream.Collectors;

/**
 * TODO DESCRIBE
 */
public class ClientStub implements OperationHandlerI {

    private ZenWebSocketClient client;

    public final Database dbHandler;
    private final List<ClientOperationHandlerI> otherHandlers = new ArrayList<>();
    private ExceptionHandler exceptionHandler = _ -> {};
    private Consumer<String> messagePrinter = _ -> {};
    public static String PROTOCOL = "https://";
    public static String SERVER = "zentodo.berndreiss.net/api/";
    public  User user;
    public int profile;
    public Status status;
    private VectorClock vectorClock;
    public static TimeDrift timeDrift = new TimeDrift();
    public List<ZenServerMessage> currentMessages = new ArrayList<>();
    private Supplier<String> passwordSupplier;

    public static void  main(String[] args) throws PositionOutOfBoundException {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("ZenToDoPU1");
        Database dbHandler = new DbHandler(emf, null);
        ClientStub stub = new ClientStub(dbHandler);

        stub.setMessagePrinter(System.out::println);
        stub.setExceptionHandler(System.out::println);
        stub.init("bd_reiss@yahoo.de", null, () -> "Test1234!?");
        Entry entry = stub.addNewEntry("TEST", 0);
        //stub.removeEntry(entry.getId());

        dbHandler.close();
        emf.close();
    }
    public ClientStub(String email, String userName, Supplier<String> passwordSupplier, @NotNull Database dbHandler){
        this.dbHandler = dbHandler;
        this.passwordSupplier = passwordSupplier;
        init(email, userName, passwordSupplier);
    }
    public ClientStub(@NotNull Database dbHandler){
        this.dbHandler = dbHandler;
        Optional<User> user = dbHandler.getUserManager().getUser(0);
        if (user.isEmpty())
            throw new RuntimeException("No default user has been found.");
        this.user = user.get();
    }

    private Status authenticate(String email, String userName, Supplier<String> passwordSupplier){
        try {
            UserManagerI userManager = dbHandler.getUserManager();
            Optional<User> userOpt = userManager.getUserByEmail(email);

            userOpt.ifPresent(value -> user = value);

            if (userOpt.isEmpty()) {
                String loginRequest = getLoginRequest(email, passwordSupplier.get());

                messagePrinter.accept("STARTING ATTEMPTS");
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
                            return Status.ONLINE;
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
                    return Status.ONLINE;
                } else {
                    if (messagePrinter != null)
                        messagePrinter.accept("Something went wrong when retrieving the status of the user from the server.");
                    return Status.OFFLINE;
                }

            }
            String token = userManager.getToken(user.getId());

            if (token != null) {
                HttpURLConnection connection = sendPostMessage(PROTOCOL + SERVER + "auth/renewToken", getLoginRequest(email, token));
                if (connection.getResponseCode() == 200) {
                    userManager.setToken(user.getId(), getBody(connection));
                    if (messagePrinter != null)
                        messagePrinter.accept("User logged in.");
                    return Status.ONLINE;
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
                    return Status.ONLINE;
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
            return Status.OFFLINE;
        }
    }

    public void reinit(){
        init(user.getEmail(), user.getUserName(), passwordSupplier);
    }
    /**
     * TODO
     */
    public void init(String email, String name, Supplier<String> passwordSupplier) {

        try {

            //Consumer<String> oldMessagePrinter = messagePrinter;
            //messagePrinter = null;
            status = authenticate(email, name, passwordSupplier);
            //messagePrinter = oldMessagePrinter;

            if (user.getId() == 0)
                return;


            //initialize Vector clock
            vectorClock = new VectorClock(user);


            if (status != Status.ONLINE) {
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
        } catch(Exception e){
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            messagePrinter.accept("Exception in getOutputStream: " + sw.toString());
        }
        processResponse(connection);
        return connection;
    }

    //TODO
    public synchronized HttpURLConnection sendAuthPostMessage(String urlString, String body) throws Exception {

        if (status == Status.DIRTY) {
            HttpURLConnection connection = sendAuthPostMessage(PROTOCOL + SERVER + "process", jsonifyServerList(dbHandler.getUserManager().getQueued(user.getId())));

            if (connection.getResponseCode() != 200) {
                throw new Exception("Something went wrong sending data to server.");
            }
            status = Status.ONLINE;
        }

        URL url = new URI(urlString).toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
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
            HttpURLConnection connection = sendAuthPostMessage(PROTOCOL + SERVER + "process", jsonifyServerList(dbHandler.getUserManager().getQueued(user.getId())));

            if (connection.getResponseCode() != 200) {
                throw new Exception("Something went wrong sending data to server.");
            }
            status = Status.ONLINE;
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
     */
    public void addOperationHandler(ClientOperationHandlerI operationHandler){
        this.otherHandlers.add(operationHandler);
    }

    /**
     * TODO DESCRIBE
     */
    public void removeOperationHandler(ClientOperationHandlerI operationHandler){
        this.otherHandlers.remove(operationHandler);
    }

    /**
     * TODO DESCRIBE
     */
    public void setExceptionHandler(ExceptionHandler exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
    }

    public ExceptionHandler getExceptionHandler(){
        return exceptionHandler;
    }
    /**
     * //TODO
     */
    public void setMessagePrinter(Consumer<String> messagePrinter){
        this.messagePrinter = messagePrinter;
    }

    public Consumer<String> getMessagePrinter() {
        return messagePrinter;
    }
    /**
     *
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
        //TODO CHECK VALIDITY
        switch (message.type){
            case POST -> {}
            case ADD_NEW_ENTRY -> {
                List<Object> args = message.arguments;
                int profile = Integer.parseInt(args.get(0).toString());
                long id = Long.parseLong(args.get(1).toString());
                String task = args.get(2).toString();
                int position = Integer.parseInt(args.get(3).toString());
                Entry entry = null;
                try {
                        entry =dbHandler.getEntryManager().addNewEntry(user.getId(), profile, id, task, position);
                } catch (PositionOutOfBoundException e){

                } catch (DuplicateIdException e) {
                    throw new RuntimeException(e);
                } catch (InvalidActionException e) {
                    throw new RuntimeException(e);
                }
                Entry finalEntry = entry;
                otherHandlers.forEach(handler ->{
                        handler.addNewEntry(finalEntry);
                });
            }
            case DELETE -> {
                int profile = Integer.parseInt(message.arguments.get(0).toString());
                long id = Long.parseLong(message.arguments.get(1).toString());
                dbHandler.getEntryManager().removeEntry(user.getId(), profile, id);
                otherHandlers.forEach(h -> h.removeEntry(id));
            }
            case SWAP -> {
                int profile = Integer.parseInt(message.arguments.get(0).toString());
                long id = Long.parseLong(message.arguments.get(1).toString());
                int position = Integer.parseInt(message.arguments.get(2).toString());
                try {
                    dbHandler.getEntryManager().swapEntries(user.getId(), profile, id, position);
                    otherHandlers.forEach(h -> {
                        try {
                            h.swapEntries(id, position);
                        } catch (PositionOutOfBoundException e) {
                           exceptionHandler.handle(e);
                        }
                    });
                } catch (PositionOutOfBoundException e){
                    //TODO HANDLE
                }
            }
            case SWAP_LIST -> {
                int profile = Integer.parseInt(message.arguments.get(0).toString());
                long id = Long.parseLong(message.arguments.get(1).toString());
                long list = Long.parseLong(message.arguments.get(2).toString());
                int position = Integer.parseInt(message.arguments.get(3).toString());
                try {
                    dbHandler.getListManager().swapListEntries(user.getId(), profile, id, list, position);
                    otherHandlers.forEach(h -> {
                        try {
                            h.swapEntries(id, position);
                        } catch (PositionOutOfBoundException e) {
                           exceptionHandler.handle(e);
                        }
                    });
                } catch (PositionOutOfBoundException e) {
                    //TODO HANDLE
                }
            }
            case UPDATE_LIST -> {
                int profile = Integer.parseInt(message.arguments.get(0).toString());
                long id = Long.parseLong(message.arguments.get(1).toString());
                Long list = Long.parseLong(message.arguments.get(2).toString());
                dbHandler.getListManager().updateList(user.getId(), profile, id, list);
                otherHandlers.forEach(h -> h.updateList(id, list));
            }
            case UPDATE_TASK -> {
                int profile = Integer.parseInt(message.arguments.get(0).toString());
                long id = Long.parseLong(message.arguments.get(1).toString());
                String task = message.arguments.get(2).toString();
                dbHandler.getEntryManager().updateTask(user.getId(), profile, id, task);
                otherHandlers.forEach(h -> h.updateTask(id, task));
            }
            case UPDATE_FOCUS -> {
                int profile = Integer.parseInt(message.arguments.get(0).toString());
                long id = Long.parseLong(message.arguments.get(1).toString());
                boolean focus = Boolean.parseBoolean(message.arguments.get(2).toString());
                dbHandler.getEntryManager().updateFocus(user.getId(), profile, id, focus);
                otherHandlers.forEach(h -> h.updateFocus(id, focus));
            }
            case UPDATE_DROPPED -> {
                int profile = Integer.parseInt(message.arguments.get(0).toString());
                long id = Long.parseLong(message.arguments.get(1).toString());
                boolean dropped = Boolean.parseBoolean(message.arguments.get(2).toString());
                dbHandler.getEntryManager().updateDropped(user.getId(), profile, id, dropped);
                otherHandlers.forEach(h -> h.updateDropped(id, dropped));
            }
            case UPDATE_RECURRENCE -> {
                int profile = Integer.parseInt(message.arguments.get(0).toString());
                long id = Long.parseLong(message.arguments.get(1).toString());
                String recurrence = message.arguments.get(2).toString();
                dbHandler.getEntryManager().updateRecurrence(user.getId(), profile, id, recurrence);
                otherHandlers.forEach(h -> h.updateRecurrence(id, recurrence));
            }
            case UPDATE_REMINDER_DATE -> {
                int profile = Integer.parseInt(message.arguments.get(0).toString());
                long id = Long.parseLong(message.arguments.get(1).toString());
                Instant date  = Instant.parse(message.arguments.get(1).toString());
                dbHandler.getEntryManager().updateReminderDate(user.getId(), profile, id, date);
                otherHandlers.forEach(h -> h.updateReminderDate(id, date));
            }
            case UPDATE_LIST_COLOR -> {
                long list = Long.parseLong(message.arguments.get(0).toString());
                String color = message.arguments.get(1).toString();
                dbHandler.getListManager().updateListColor(list, color);
                otherHandlers.forEach(h -> h.updateListColor(list, color));
            }
            case UPDATE_MAIL -> {
                String mail = message.arguments.get(0).toString();
                try {
                    dbHandler.getUserManager().updateEmail(user.getId(), mail);
                } catch (InvalidActionException e){
                    //TODO HANDLE
                }
            }
            case UPDATE_USER_NAME -> {
                String name = message.arguments.get(0).toString();
                dbHandler.getUserManager().updateUserName(user.getId(), name);
            }
        }

    }

    private synchronized void sendUpdate(OperationType type, List<Object> arguments) {
        if (user.getId() == 0)
            return;
        vectorClock.increment();
        dbHandler.getUserManager().setClock(user.getId(), vectorClock);
        ZenServerMessage zm = new ZenServerMessage(type, arguments, vectorClock);
        List<ZenServerMessage> list = new ArrayList<>();
        list.add(zm);
        currentMessages.add(zm);
        try {
            if (status != Status.ONLINE)
                init(user.getEmail(), user.getUserName(), passwordSupplier);
            int responseCode = 404;
            if (status == Status.ONLINE) {
                HttpURLConnection connection = sendAuthPostMessage(PROTOCOL + SERVER + "process", jsonifyServerList(list));
                responseCode = connection.getResponseCode();
            }
            if (responseCode != 200)
                throw new Exception("There was a problem sending data to the server: " + responseCode);
        } catch (Exception e) {
            exceptionHandler.handle(e);
            dbHandler.getUserManager().addToQueue(user, zm);
        }
        currentMessages.remove(zm);
    }

    @Override
    public synchronized Entry addNewEntry(String task) {
        Entry entry = dbHandler.getEntryManager().addNewEntry(user == null ? 0 : user.getId(), user == null ? profile : user.getProfile(), task);
        return entry == null ? null : addNewEntry(entry);
    }
    @Override
    public synchronized Entry addNewEntry(String task, int position) throws PositionOutOfBoundException {
        Entry entry = dbHandler.getEntryManager().addNewEntry(user == null ? 0 : user.getId(), user == null ? profile : user.getProfile(), task, position);
        return addNewEntry(entry);
    }

    @Override
    public synchronized Entry addNewEntry(Entry entry) {
        List<Object> arguments = new ArrayList<>();
        arguments.add(user.getProfile());
        arguments.add(entry.getId());
        arguments.add(entry.getTask());
        arguments.add(entry.getPosition());
        sendUpdate(OperationType.ADD_NEW_ENTRY, arguments);
        return entry;
    }


    @Override
    public synchronized void removeEntry(long id) {
        List<Object> arguments = new ArrayList<>();
        arguments.add(user.getProfile());
        arguments.add(id);
        sendUpdate(OperationType.DELETE, arguments);
        dbHandler.getEntryManager().removeEntry(user == null ? 0 : user.getId(), user == null ? profile : user.getProfile(), id);
    }

    @Override
    public synchronized Optional<Entry> getEntry(long id) {
        return dbHandler.getEntryManager().getEntry(user == null ? 0 : user.getId(), user == null ? profile : user.getProfile(), id);
    }

    @Override
    public List<Entry> loadEntries() {
        return dbHandler.getEntryManager().getEntries(user == null ? 0 : user.getId(), user == null ? profile : user.getProfile());
    }

    @Override
    public List<Entry> loadFocus() {
        return dbHandler.getEntryManager().loadFocus(user.getId(), user.getProfile());
    }

    @Override
    public List<Entry> loadDropped() {
        return dbHandler.getEntryManager().loadDropped(user.getId(), user.getProfile());
    }

    @Override
    public List<Entry> loadList(Long list) {
        return dbHandler.getListManager().getListEntries(user.getId(), user.getProfile(), list);
    }

    @Override
    public List<TaskList> loadLists() {
        return dbHandler.getListManager().getListsForUser(user.getId(), user.getProfile());
    }

    @Override
    public Map<Long, String> getListColors() {
        List<TaskList> lists = loadLists();
        return lists.stream().collect(Collectors.toMap(
                TaskList::getId,
                TaskList::getColor
        ));
    }

    @Override
    public Optional<TaskList> getListByName(String name) {
        return dbHandler.getListManager().getListByName(user.getId(), user.getProfile(), name);
    }

    @Override
    public synchronized void swapEntries(long id, int position) throws PositionOutOfBoundException {
        dbHandler.getEntryManager().swapEntries(user.getId(), user.getProfile(), id, position);
        List<Object> arguments = new ArrayList<>();
        arguments.add(user.getProfile());
        arguments.add(id);
        arguments.add(position);
        sendUpdate(OperationType.SWAP, arguments);
    }

    @Override
    public synchronized void swapListEntries(long list, long id, int position) throws PositionOutOfBoundException {
        dbHandler.getListManager().swapListEntries(user.getId(), user.getProfile(), list, id, position);
        List<Object> arguments = new ArrayList<>();
        arguments.add(user.getProfile());
        arguments.add(id);
        arguments.add(position);
        sendUpdate(OperationType.SWAP_LIST, arguments);
    }

    @Override
    public synchronized void updateTask(long id, String value) {
        List<Object> arguments = new ArrayList<>();
        arguments.add(user.getProfile());
        arguments.add(id);
        arguments.add(value);
        sendUpdate(OperationType.UPDATE_LIST, arguments);
        dbHandler.getEntryManager().updateTask(user.getId(), user.getProfile(), id, value);
    }

    @Override
    public synchronized void updateFocus(long id, boolean value) {
        List<Object> arguments = new ArrayList<>();
        arguments.add(user.getProfile());
        arguments.add(id);
        arguments.add(value);
        sendUpdate(OperationType.UPDATE_LIST, arguments);
        dbHandler.getEntryManager().updateFocus(user.getId(), user.getProfile(), id, value);
    }

    @Override
    public synchronized void updateDropped(long id, boolean value) {
        List<Object> arguments = new ArrayList<>();
        arguments.add(user.getProfile());
        arguments.add(id);
        arguments.add(value);
        sendUpdate(OperationType.UPDATE_LIST, arguments);
        dbHandler.getEntryManager().updateDropped(user.getId(), user.getProfile(), id, value);
    }

    @Override
    public synchronized void updateList(long id, Long newId) {
        List<Object> arguments = new ArrayList<>();
        arguments.add(user.getProfile());
        arguments.add(id);
        arguments.add(newId);
        sendUpdate(OperationType.UPDATE_LIST, arguments);
        dbHandler.getListManager().updateList(user.getId(), user.getProfile(), id, newId);
    }

    @Override
    public synchronized void updateReminderDate(long id, Instant value) {
        List<Object> arguments = new ArrayList<>();
        arguments.add(user.getProfile());
        arguments.add(id);
        arguments.add(value);
        sendUpdate(OperationType.UPDATE_REMINDER_DATE, arguments);
        dbHandler.getEntryManager().updateReminderDate(user.getId(), user.getProfile(), id, value);
    }

    @Override
    public synchronized void updateRecurrence(long id, String value) {
        List<Object> arguments = new ArrayList<>();
        arguments.add(user.getProfile());
        arguments.add(id);
        arguments.add(value);
        sendUpdate(OperationType.UPDATE_RECURRENCE, arguments);
        dbHandler.getEntryManager().updateRecurrence(user.getId(), user.getProfile(), id, value);
    }

    @Override
    public synchronized void updateListColor(long list, String color) {
        List<Object> arguments = new ArrayList<>();
        arguments.add(list);
        arguments.add(color);
        sendUpdate(OperationType.UPDATE_LIST_COLOR, arguments);
        dbHandler.getListManager().updateListColor(list, color);
    }

    @Override
    public synchronized void updateUserName(String name) {
        if (user.getId() == 0)
            return;
        List<Object> arguments = new ArrayList<>();
        arguments.add(name);
        sendUpdate(OperationType.UPDATE_USER_NAME, arguments);
        dbHandler.getUserManager().updateUserName(user.getId(), name);
        user.setUserName(name);
    }

    @Override
    public synchronized void updateEmail(String email) throws InvalidActionException, IOException {
        if (user.getId() == 0)
            return;
        String password = passwordSupplier.get();
        String loginRequest = getLoginRequest(email, password);
        try {
            HttpURLConnection connection = sendPostMessage(PROTOCOL + SERVER + "auth/status", loginRequest);
            String body = getBody(connection);
            if (!body.equals("non"))
                throw new InvalidActionException("User with mail address already exists.");
        } catch (URISyntaxException _){}
        List<Object> arguments = new ArrayList<>();
        arguments.add(email);
        sendUpdate(OperationType.UPDATE_MAIL, arguments);
        dbHandler.getUserManager().updateEmail(user.getId(), email);
        user.setEmail(email);
    }

    public synchronized void clearQueue() {
        if (user.getId() == 0)
            return;
        dbHandler.getUserManager().clearQueue(user.getId());
    }
    }
