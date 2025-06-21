package net.berndreiss.zentodo.util;

import com.sun.istack.NotNull;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.websocket.DeploymentException;
import net.berndreiss.zentodo.operations.ClientOperationHandlerI;
import net.berndreiss.zentodo.operations.OperationHandlerI;
import net.berndreiss.zentodo.operations.OperationType;
import net.berndreiss.zentodo.data.Entry;
import net.berndreiss.zentodo.data.TaskList;
import net.berndreiss.zentodo.data.User;
import net.berndreiss.zentodo.exceptions.*;
import net.berndreiss.zentodo.persistence.DbHandler;
import net.berndreiss.zentodo.data.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * A client stub for communication with the server.
 * Default user
 * Queue
 */
public class ClientStub implements OperationHandlerI {

    private static final Logger logger = LoggerFactory.getLogger(ClientStub.class);

    /** The websocket client for allowing the server to synch data live*/
    private ZenWebSocketClient webSocketClient;

    /** The database for persisting data */
    public final Database dbHandler;
    /** Handlers being triggered when data is being received */
    private final List<ClientOperationHandlerI> otherHandlers = new ArrayList<>();
    /** Prints informative messages (e.g., Problem connecting to server) */
    private Consumer<String> messagePrinter = _ -> {};
    /** The protocol being used for the websocket client */
    public static String WEBSOCKET_PROTOCOL = "wss";
    /** The protocol being used with the server */
    public static String PROTOCOL = "https";
    /** The server url */
    public static String SERVER = "zentodo.berndreiss.net/api";
    /** The current user. Default user without online synchronization has id 0 */
    @NotNull
    public User user;
    /** Status in regard to the server */
    public Status status;
    /**The current vector clock for the user */
    private VectorClock vectorClock;
    /** The time drift with the server */
    public static TimeDrift timeDrift = new TimeDrift();
    /** Means of retrieving the password for login */
    private Supplier<String> passwordSupplier;

    //Method for testing purposes
    public static ClientStub getStub(String userName, String email, String persistenceUnit) throws IOException {

        Path path = Paths.get(userName);

        try {
            Files.createDirectory(path);
            System.out.println("Directory created: " + path.toAbsolutePath());
        } catch (Exception e) {
            System.err.println("Failed to create directory: " + e.getMessage());
        }
        EntityManagerFactory emf = Persistence.createEntityManagerFactory(persistenceUnit);
        Database opHandler = new DbHandler(emf, userName);
        ClientStub stub = new  ClientStub(opHandler);
        stub.init(email, userName, () -> "Test1234!?");
        stub.setMessagePrinter(System.out::println);
        return stub;
    }

    //Method for testing purposes
    public static void main(String[] args) throws IOException {
        for (int i = 0; i < 1; i++) {
            try {
                Files.createDirectory(Paths.get("user0"));
                Files.createDirectory(Paths.get("user1"));
                Files.createDirectory(Paths.get("user2"));
            } catch (Exception _) {
            }

            ClientStub stub0 = getStub("user0", "bd_reiss@yahoo.de", "ZenToDoPU");
            //List<Entry> entries0 = stub0.loadEntries();
            ClientStub stub1 = getStub("user1", "bd_reiss@yahoo.de", "ZenToDoPU1");
            //List<Entry> entries1 = stub1.loadEntries();
            ClientStub stub2 = getStub("user2", "bd_reiss@yahoo.de", "ZenToDoPU2");
            //List<Entry> entries2 = stub2.loadEntries();

            //Entry entry0 = stub0.addNewEntry("TASK0");
            //Entry entry1 = stub1.addNewEntry("TASK1");
            //Entry entry2 = stub2.addNewEntry("TASK2");

            stub0.reinit();
            stub1.reinit();
            stub2.reinit();
            //Profile profile1 = dbHandler.getUserManager().addProfile(stub.user.getId());
            //stub.user.setProfile(profile1.getId());
            //Entry entry = stub.addNewEntry("Test");
            //stub.removeEntry(entry.getId());
            //Optional<Entry> entry1 = stub.getEntry(entry.getId());

            stub0.dbHandler.close();
            stub1.dbHandler.close();
            stub2.dbHandler.close();
        }
    }

    /**
     * Creates a new instance of the client stub and sets the use to the default user.
     * @param dbHandler The database for persisting data
     */
    public ClientStub(@NotNull Database dbHandler) {
        this.dbHandler = dbHandler;
        Optional<User> user = dbHandler.getUserManager().getUser(0);
        if (user.isEmpty()) {
            logger.error("No default user has been found.");
            throw new RuntimeException("No default user has been found.");
        }
        this.user = user.get();
        this.status = Status.OFFLINE;
    }

    //Authenticate the user -> if non-existent in database register and add to database
    private Status authenticate(String email, String userName) throws IOException, DuplicateUserIdException, InvalidUserActionException {

        //Check whether user exists already in local database
        UserManagerI userManager = dbHandler.getUserManager();
        Optional<User> userOpt = userManager.getUserByEmail(email);
        userOpt.ifPresent(value -> {
            user = value;
            vectorClock = new VectorClock(user);
        });

        //If the user does NOT exist, register user
        if (user.getId() == 0 || userOpt.isEmpty() || userOpt.get().getDevice() == null) {

            //get login request as JSON with "email" and "password"
            String loginRequest = getLoginRequest(email, passwordSupplier.get());
            int attempts = 0;
            while (attempts++ < 10) {//try to register x times

                /*
                 *  Send login request to server for registration
                 *  The answer can be:
                 *    -200:
                 *      ->"exists" -> user is already registered and needs to confirm mail
                 *        * Fields: "status", "id"
                 *      ->"registered" -> user was registered and needs to confirm mail
                 *        * Fields: "status", "id"
                 *      ->"logged_in" -> user was already enabled and
                 *        * Fields: "status", "id", "device", "jwtToken"
                 */
                HttpURLConnection connection = sendPostMessage(PROTOCOL + "://" + SERVER + "/auth/register", loginRequest);
                int responseCode = connection.getResponseCode();

                //Received valid answer
                if (responseCode == 200) {

                    String body = getBody(connection);
                    String[] fields = body.split(",");

                    //User needs to verify mail -> add user and return REGISTERED
                    switch (fields[0]) {
                        case "exists" -> {
                            userManager.addUser(Long.parseLong(fields[1]), email, userName, Integer.parseInt(fields[2]));
                            if (messagePrinter != null)
                                messagePrinter.accept("User is already registered, but not verified. Check your mail.");
                            return Status.REGISTERED;
                        }


                        //User needs to verify mail -> add user and return REGISTERED
                        case "registered" -> {
                            userManager.addUser(Long.parseLong(fields[1]), email, userName, null);
                            if (messagePrinter != null)
                                messagePrinter.accept("User was registered. Check your mails for verification.");
                            return Status.REGISTERED;
                        }


                        //User was logged in -> add user, save token and return ONLINE
                        case "logged_in" -> {
                            user = userManager.addUser(Long.parseLong(fields[1]), email, userName, Integer.parseInt(fields[2]));
                            vectorClock = new VectorClock(user);
                            userManager.enableUser(user.getId());
                            user.setEnabled(true);
                            userManager.setToken(Long.parseLong(fields[1]), fields[3]);
                            if (messagePrinter != null)
                                messagePrinter.accept("User logged in.");
                            return Status.ONLINE;
                        }
                    }

                    throw new ConnectException("User could not be registered");
                }
            }
            if (attempts > 10)
                throw new ConnectException("Server not available");
        }

        /*
         *  If user exists but was not enabled, get the status.
         *  Possible answers from the server:
         *    - "non": user does not exist
         *      -> TODO WHAT TO DO HERE?
         *          * Form: "status"
         *    - "exists": user exists, but the mail has to be verified
         *          * Form: "status"
         *    - "enabled":
         *          * Form: "status", "jwtToken"
         *
         */
        if (!user.getEnabled()) {

            //Get login request as JSON with "email" and "password"
            String loginRequest = getLoginRequest(email, passwordSupplier.get());

            //Get the status from the server
            HttpURLConnection connection = sendPostMessage(PROTOCOL + "://" + SERVER + "/auth/status", loginRequest);
            String body = getBody(connection);

            //The user has been deleted
            if (body.equals("non"))
                return Status.DELETED;

            //User exists, but mail was not verified. Return REGISTERED.
            if (body.equals("exists")) {
                if (messagePrinter != null)
                    messagePrinter.accept("User already exists, but was not verified. Check your mails.");
                return Status.REGISTERED;
            }
            //User is enabled -> Process token.
            if (body.startsWith("enabled")) {
                user.setEnabled(true);
                userManager.enableUser(user.getId());
                userManager.setToken(user.getId(), body.split(",")[1]);
                if (messagePrinter != null)
                    messagePrinter.accept("User logged in.");
                return Status.ONLINE;
            }
            //The server did not send a valid response
            if (messagePrinter != null)
                messagePrinter.accept("Something went wrong when retrieving the status of the user from the server.");
            throw new ConnectException("There was a problem retrieving the status of the user from the server");
        }

        //User exists and is enabled. Proceed to retrieve token and log in.
        String token = userManager.getToken(user.getId());

        if (token != null) {
            //Renew token and log in.
            HttpURLConnection connection = sendPostMessage(PROTOCOL + "://" + SERVER + "/auth/renewToken", getLoginRequest(email, token));
            if (connection.getResponseCode() == 200) {
                userManager.setToken(user.getId(), getBody(connection));
                if (messagePrinter != null)
                    messagePrinter.accept("User logged in.");
                return Status.ONLINE;
            }
        }

        //Token does not exist. Retrieve it from the server.
        String loginRequest = getLoginRequest(email, passwordSupplier.get());

        int attempts = 0;
        while (attempts++ < 10) {//try for 10 times
            HttpURLConnection connection = sendPostMessage(PROTOCOL + "://" + SERVER + "/auth/login", loginRequest);
            //We received the token
            if (connection.getResponseCode() == 200) {
                userManager.setToken(user.getId(), getBody(connection));
                if (messagePrinter != null)
                    messagePrinter.accept("User logged in.");
                return Status.ONLINE;
            }
            //User does not exist anymore
            if (connection.getResponseCode() == 404)
                return Status.DELETED;
        }
        throw new ConnectException("Was not able to login.");
    }

    /**
     * Reinitialize the user, if it was initialized before.
     * @throws IOException Exception thrown when there are problems communicating with the server.
     */
    public void reinit() throws IOException {
        //If default user return
        if (user.getId() == 0)
            return;
        init(user.getEmail(), user.getUserName(), passwordSupplier);
    }

    /**
     * Initialize the stub with a user.
     * @param email Mail address of the user.
     * @param name Name of the user.
     * @param passwordSupplier Means of retrieving the password for the user.
     * @throws IOException Exception thrown when communicating with the server.
     */
    public void init(@NotNull String email, String name, @NotNull Supplier<String> passwordSupplier) throws IOException {
        this.passwordSupplier = passwordSupplier;

        //Authenticate the user and get the status.
        try {
            status = authenticate(email, name);
        } catch (DuplicateUserIdException e) {//this should actually never happen
            logger.error("Attempt to add duplicate user id in authenticate.", e);
            throw new RuntimeException(e);
        } catch (InvalidUserActionException e){
            logger.error("Attempt to perform invalid user action in authenticate.", e);
            throw new RuntimeException(e);
        }

        //If the user is still the default user return
        if (user.getId() == 0)
            return;

        //Initialize the vector clock
        vectorClock = new VectorClock(user);

        //If we are not online return
        if (status != Status.ONLINE)
            return;

        HttpURLConnection connection;

        //TODO ignore if queue is empty
        //Send the local queue to the server
        try {
            connection = sendAuthPostMessage(PROTOCOL + "://" + SERVER + "/process_operation", jsonifyServerList(dbHandler.getUserManager().getQueued(user.getId())));
        } catch (URISyntaxException e) {
            logger.error("URI is not valid", e);
            throw new RuntimeException(e);
        }

        //If the response was not valid go offline and throw an exception
        if (connection.getResponseCode() != 200) {
            status = Status.OFFLINE;
            throw new ConnectException("There was a problem sending queue data to the server");
        }

        //Get the queue from the server and process it
        try {
            connection = sendAuthPostMessage(PROTOCOL + "://" + SERVER + "/queue", "");
        } catch (URISyntaxException e) {//This should never happen
            logger.error("URI is not valid", e);
            throw new RuntimeException(e);
        }

        //If the response was not valid go offline and throw an exception
        int responseCode = connection.getResponseCode();
        if (responseCode != 200) {
            status = Status.OFFLINE;
            throw new ConnectException("There was a problem getting queue data from the server.");
        }

        //Get the response and process the messages it contains
        String response = getBody(connection);
        for (ZenMessage message : parseMessage(getMessage(response))) {
            try {
                receiveMessage(message);
            } catch (PositionOutOfBoundException e) {
                logger.error("Position out of bound:\n{}", jsonifyMessage(message), e);
                throw new RuntimeException(e);
            } catch (DuplicateIdException e){
                logger.error("Attempt to add entry with duplicate id:\n{}", jsonifyMessage(message), e);
                throw new RuntimeException(e);
            } catch (InvalidActionException e){
                logger.error("Attempt to perform invalid action:\n{}", jsonifyMessage(message), e);
                throw new RuntimeException(e);
            }
        }

        //Set up the websocket client and run it on another thread to retrieve messages live from the server
        webSocketClient = new ZenWebSocketClient(this::consumeMessage, this);
        Thread thread = new Thread(() -> {
            try {
                webSocketClient.connect(user.getEmail(), dbHandler.getUserManager().getToken(user.getId()), user.getDevice());
            } catch (DeploymentException | IOException e) {
                logger.error("Problem connecting the websocket client", e);
                throw new RuntimeException(e);
            }
        });
        thread.start();
        // Set clock drift
        for (int i = 0; i < 8; i++) {
            try {
                sendAuthPostMessage(PROTOCOL + "://" + SERVER + "test", "");
            } catch (URISyntaxException e) {
                logger.error("URI is not valid", e);
                throw new RuntimeException(e);
            }
        }
    }

    /*
     * Consume a raw message form the server:
     *   - parses the message as a list of ZenMessage
     *   - sends an acknowledgement for the message to the server
     *   - processes the operations contained in the message
     */
    private void consumeMessage(String rawMessage) {

        String id = getId(rawMessage);
        JSONArray parsedMessage = getMessage(rawMessage);
        List<ZenMessage> messages = parseMessage(parsedMessage);

        //Send acknowledgement to the server
        HttpURLConnection conn;
        try {
            conn = sendAuthPostMessage(PROTOCOL + "://" + SERVER + "/ackn", id);
            if (conn.getResponseCode() != 200) {
                status = Status.OFFLINE;
                return;
            }
        } catch (URISyntaxException e) {
            logger.error("URI is not valid", e);
            throw new RuntimeException(e);
        } catch (IOException e){
            status = Status.OFFLINE;
            return;
        }

        //Process the operations contained in the message
        for (ZenMessage zm : messages) {
            try {
                receiveMessage(zm);
            } catch (DuplicateIdException e) {
                logger.error("There was an attempt to add an entry with a duplicate id:\n{}", jsonifyMessage(zm), e);
                throw new RuntimeException(e);
            } catch (InvalidActionException e){
                logger.error("There was an attempt to perform an invalid action:\n{}", jsonifyMessage(zm), e);
                throw new RuntimeException(e);
            } catch (PositionOutOfBoundException e){
                logger.error("An entry was out of bound:\n{}", jsonifyMessage(zm), e);
                throw new RuntimeException(e);
                //TODO remove ConnectException for other handlers?
            } catch (ConnectException _){}
        }
    }

    /**
     * Get the body of a URL connection.
     * @param connection The connection to process.
     * @return The body.
     * @throws IOException Thrown when there are problems communicating with the server.
     */
    public static String getBody(HttpURLConnection connection) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String inputLine;
        StringBuilder response = new StringBuilder();
        while ((inputLine = br.readLine()) != null)
            response.append(inputLine);
        return response.toString();
    }

    /**
     * Send a non authenticated Post message.
     * @param urlString URL to use.
     * @param body Message body to send.
     * @return the URL connection.
     * @throws IOException Thrown when there are problems communicating with the server.
     */
    public HttpURLConnection sendPostMessage(String urlString, String body) throws IOException {

        URL url;
        try {
            url = new URI(urlString).toURL();
        } catch (URISyntaxException e) {
            logger.error("URI was not valid", e);
            throw new RuntimeException(e);
        }
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        //This is used to determine the time drift between the server and the client using the basic NTP
        // -> see class TimeDrift
        connection.setRequestProperty("t1", TimeDrift.getTimeStamp());
        connection.setDoOutput(true);

        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = body.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }
        processResponse(connection);
        return connection;
    }

    /**
     * Send an authenticated post message to the server.
     * If the status is DIRTY also send the queue to the server.
     *
     * @param urlString URL to use.
     * @param body The body to send.
     * @return a connection.
     * @throws IOException Thrown when there is a problem communicating with the server.
     * @throws URISyntaxException Thrown when the URI is not valid.
     */
    public synchronized HttpURLConnection sendAuthPostMessage(String urlString, String body) throws IOException, URISyntaxException {

        //If the status is DIRTY send the queue to the server.
        if (status == Status.DIRTY) {
            HttpURLConnection connection = sendAuthPostMessage(PROTOCOL + "://" + SERVER + "/process_operation", jsonifyServerList(dbHandler.getUserManager().getQueued(user.getId())));

            if (connection.getResponseCode() != 200) {
                throw new ConnectException("Something went wrong sending data to server.");
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

    //This method is used at the moment since there is a problem using GET methods with Android
    /*
    public synchronized HttpURLConnection sendAuthGetMessage(String urlString) throws Exception {

        if (status == Status.DIRTY) {
            HttpURLConnection connection = sendAuthPostMessage(PROTOCOL + "://" + SERVER + "/process_operation", jsonifyServerList(dbHandler.getUserManager().getQueued(user.getId())));

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
    }*/

    //TODO
    private void processResponse(HttpURLConnection connection) {
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

        } catch (DateTimeParseException ignored) {
        }

    }

    //TODO do we want this?
    /**
     *
     */
    public void sync() {
        List<ZenServerMessage> queue = dbHandler.getUserManager().getQueued(user.getId());
    }

    /**
     * TODO DESCRIBE
     */
    public void addOperationHandler(ClientOperationHandlerI operationHandler) {
        this.otherHandlers.add(operationHandler);
    }

    /**
     * TODO DESCRIBE
     */
    public void removeOperationHandler(ClientOperationHandlerI operationHandler) {
        this.otherHandlers.remove(operationHandler);
    }

    /**
     * //TODO
     */
    public void setMessagePrinter(Consumer<String> messagePrinter) {
        this.messagePrinter = messagePrinter;
    }

    public Consumer<String> getMessagePrinter() {
        return messagePrinter;
    }

    /**
     *
     */
    public User getUser() {
        return user;
    }

    //TODO DESCRIBE
    private static String getAuthHeader(String email, String password) {

        String encodedAuth = Base64.getEncoder().encodeToString((email + ":" + password).getBytes(StandardCharsets.UTF_8));
        return "Basic " + encodedAuth;
    }

    //TODO DESCRIBE
    private static String getLoginRequest(String email, String password) {
        return "{\"email\": \"" + email + "\", \"password\": \"" + password + "\"}";
    }

    public static String jsonifyMessage(ZenMessage message) {
        return jsonifyMessage(message, "");
    }

    /**
     * TODO DESCRIBE
     */
    public static String jsonifyMessage(ZenMessage message, String whitespace) {
        StringBuilder sb = new StringBuilder();

        sb.append(whitespace).append("{\n");
        sb.append(whitespace).append("  \"type\": \"").append(message.type).append("\",\n");
        sb.append(whitespace).append("  \"arguments\": [");
        String prefix = "";
        for (Object argument : message.arguments) {
            sb.append(prefix);
            prefix = ", ";
            sb.append("\"").append(argument).append("\"");

        }
        sb.append("]");
        if (message instanceof ZenServerMessage) {
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
    public static String jsonifyList(List<ZenMessage> list) {
        StringBuilder sb = new StringBuilder();

        sb.append("[");

        String prefix = "\n";
        for (ZenMessage message : list) {
            sb.append(prefix);
            prefix = ",\n";
            sb.append(jsonifyMessage(message, "  "));
        }

        sb.append("\n]");

        return sb.toString();
    }

    public static String jsonifyServerList(List<ZenServerMessage> list) {
        StringBuilder sb = new StringBuilder();

        sb.append("[");

        String prefix = "\n";
        for (ZenMessage message : list) {
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
            list.add(new ZenMessage(type, arguments, clock));
        }
        return list;
    }

    public static String getId(String rawMessage) {
        JSONObject obj = new JSONObject(rawMessage);
        return obj.get("id").toString();
    }

    public static JSONArray getMessage(String rawMessage) {
        JSONObject obj = new JSONObject(rawMessage);
        return (JSONArray) obj.get("message");
    }

    private void receiveMessage(ZenMessage message) throws DuplicateIdException, InvalidActionException, PositionOutOfBoundException, ConnectException {
        //TODO CHECK VALIDITY
        switch (message.type) {
            case POST -> {
                List<Object> args = message.arguments;
                int profile = Integer.parseInt(args.get(0).toString());
                long id = Long.parseLong(args.get(1).toString());
                String task = args.get(2).toString();
                int position = Integer.parseInt(args.get(3).toString());
                boolean focus = Boolean.parseBoolean(args.get(4).toString());
                boolean dropped = Boolean.parseBoolean(args.get(5).toString());
                Long list = args.get(6).toString().isEmpty() ? null : Long.parseLong(args.get(6).toString());
                Integer listPosition = args.get(7).toString().isEmpty() ? null : Integer.parseInt(args.get(7).toString());
                Instant reminderDate = args.get(8).toString().isEmpty() ? null : Instant.parse(args.get(8).toString());
                String recurrence = args.get(9).toString().isEmpty() ? null : args.get(9).toString();

                Entry entry = new Entry(user.getId(), profile, task, position);
                entry.setId(id);
                entry.setFocus(focus);
                entry.setDropped(dropped);
                entry.setList(list);
                entry.setListPosition(listPosition);
                entry.setReminderDate(reminderDate);
                entry.setRecurrence(recurrence);

                dbHandler.getEntryManager().postEntry(entry);
            }
            case ADD_NEW_ENTRY -> {
                List<Object> args = message.arguments;
                int profile = Integer.parseInt(args.get(0).toString());
                long id = Long.parseLong(args.get(1).toString());
                String task = args.get(2).toString();
                int position = Integer.parseInt(args.get(3).toString());
                Entry entry = null;
                entry = dbHandler.getEntryManager().addNewEntry(user.getId(), profile, id, task, position);

                Entry finalEntry = entry;
                for (OperationHandlerI oh : otherHandlers)
                    oh.addNewEntry(finalEntry);
            }
            case DELETE -> {
                int profile = Integer.parseInt(message.arguments.get(0).toString());
                long id = Long.parseLong(message.arguments.get(1).toString());
                dbHandler.getEntryManager().removeEntry(user.getId(), profile, id);
                for (OperationHandlerI oh : otherHandlers)
                    oh.removeEntry(id);
            }
            case SWAP -> {
                int profile = Integer.parseInt(message.arguments.get(0).toString());
                long id = Long.parseLong(message.arguments.get(1).toString());
                int position = Integer.parseInt(message.arguments.get(2).toString());
                dbHandler.getEntryManager().swapEntries(user.getId(), profile, id, position);
                for (OperationHandlerI oh : otherHandlers)
                    oh.swapEntries(id, position);
            }
            case SWAP_LIST -> {
                int profile = Integer.parseInt(message.arguments.get(0).toString());
                long id = Long.parseLong(message.arguments.get(1).toString());
                long list = Long.parseLong(message.arguments.get(2).toString());
                int position = Integer.parseInt(message.arguments.get(3).toString());
                dbHandler.getListManager().swapListEntries(user.getId(), profile, id, list, position);
                for (OperationHandlerI oh : otherHandlers)
                    oh.swapEntries(id, position);
            }
            case UPDATE_TASK -> {
                int profile = Integer.parseInt(message.arguments.get(0).toString());
                long id = Long.parseLong(message.arguments.get(1).toString());
                String task = message.arguments.get(2).toString();
                dbHandler.getEntryManager().updateTask(user.getId(), profile, id, task);
                for (OperationHandlerI oh : otherHandlers)
                    oh.updateTask(id, task);
            }
            case UPDATE_FOCUS -> {
                int profile = Integer.parseInt(message.arguments.get(0).toString());
                long id = Long.parseLong(message.arguments.get(1).toString());
                boolean focus = Boolean.parseBoolean(message.arguments.get(2).toString());
                dbHandler.getEntryManager().updateFocus(user.getId(), profile, id, focus);
                for (OperationHandlerI oh : otherHandlers)
                    oh.updateFocus(id, focus);
            }
            case UPDATE_DROPPED -> {
                int profile = Integer.parseInt(message.arguments.get(0).toString());
                long id = Long.parseLong(message.arguments.get(1).toString());
                boolean dropped = Boolean.parseBoolean(message.arguments.get(2).toString());
                dbHandler.getEntryManager().updateDropped(user.getId(), profile, id, dropped);
                for (OperationHandlerI oh : otherHandlers)
                    oh.updateDropped(id, dropped);
            }
            case UPDATE_LIST -> {
                int profile = Integer.parseInt(message.arguments.get(0).toString());
                long id = Long.parseLong(message.arguments.get(1).toString());
                Long list = Long.parseLong(message.arguments.get(2).toString());
                dbHandler.getListManager().updateList(user.getId(), profile, id, list);
                for (OperationHandlerI oh : otherHandlers)
                    oh.updateList(id, list);
            }
            case UPDATE_REMINDER_DATE -> {
                int profile = Integer.parseInt(message.arguments.get(0).toString());
                long id = Long.parseLong(message.arguments.get(1).toString());
                Instant date = Instant.parse(message.arguments.get(1).toString());
                dbHandler.getEntryManager().updateReminderDate(user.getId(), profile, id, date);
                for (OperationHandlerI oh : otherHandlers)
                    oh.updateReminderDate(id, date);
            }
            case UPDATE_RECURRENCE -> {
                int profile = Integer.parseInt(message.arguments.get(0).toString());
                long id = Long.parseLong(message.arguments.get(1).toString());
                String recurrence = message.arguments.get(2).toString();
                dbHandler.getEntryManager().updateRecurrence(user.getId(), profile, id, recurrence);
                for (OperationHandlerI oh : otherHandlers)
                    oh.updateRecurrence(id, recurrence);
            }
            case UPDATE_LIST_COLOR -> {
                long list = Long.parseLong(message.arguments.get(0).toString());
                String color = message.arguments.get(1).toString();
                dbHandler.getListManager().updateListColor(list, color);
                for (OperationHandlerI oh : otherHandlers)
                    oh.updateListColor(list, color);
            }
            case UPDATE_USER_NAME -> {
                String name = message.arguments.get(0).toString();
                dbHandler.getUserManager().updateUserName(user.getId(), name);
            }
            case UPDATE_MAIL -> {
                String mail = message.arguments.get(0).toString();
                dbHandler.getUserManager().updateEmail(user.getId(), mail);
            }
            case UPDATE_ID -> {
                //TODO IMPLEMENT
            }
        }

    }

    private synchronized void sendUpdate(OperationType type, List<Object> arguments) throws ConnectException {
        if (user.getId() == 0)
            return;
        vectorClock.increment();
        dbHandler.getUserManager().setClock(user.getId(), vectorClock);
        ZenServerMessage zm = new ZenServerMessage(type, arguments, vectorClock);
        List<ZenServerMessage> list = new ArrayList<>();
        list.add(zm);
        try {
            if (status != Status.ONLINE)
                init(user.getEmail(), user.getUserName(), passwordSupplier);
            int responseCode = 404;
            if (status == Status.ONLINE) {
                HttpURLConnection connection = sendAuthPostMessage(PROTOCOL + "://" + SERVER + "/process_operation", jsonifyServerList(list));
                responseCode = connection.getResponseCode();
            }
            if (responseCode != 200)
                throw new Exception("There was a problem sending data to the server: " + responseCode);
        } catch (Exception e) {
            status = Status.OFFLINE;
            dbHandler.getUserManager().addToQueue(user, zm);
        }
    }

    @Override
    public synchronized Entry addNewEntry(String task) throws ConnectException {
        Entry entry = dbHandler.getEntryManager().addNewEntry(user.getId(), user.getProfile(), task);
        if (entry != null)
            sendAddEntryUpdate(entry);
        return entry;
    }

    @Override
    public synchronized Entry addNewEntry(String task, int position) throws PositionOutOfBoundException, ConnectException {
        Entry entry = dbHandler.getEntryManager().addNewEntry(user.getId(), user.getProfile(), task, position);
        if (entry != null)
            sendAddEntryUpdate(entry);
        return entry;
    }

    //TODO THIS IS NOT IDEAL
    @Override
    public synchronized Entry addNewEntry(Entry entry) throws PositionOutOfBoundException, DuplicateIdException, InvalidActionException, ConnectException {
        Entry persistedEntry = dbHandler.getEntryManager().addNewEntry(user.getId(), user.getProfile(), entry.getId(), entry.getTask(), entry.getPosition());
        if (persistedEntry == null)
            return null;
        sendAddEntryUpdate(persistedEntry);
        return persistedEntry;
    }

    private void sendAddEntryUpdate(Entry entry) throws ConnectException {
        List<Object> arguments = new ArrayList<>();
        arguments.add(user.getProfile());
        arguments.add(entry.getId());
        arguments.add(entry.getTask());
        arguments.add(entry.getPosition());
        sendUpdate(OperationType.ADD_NEW_ENTRY, arguments);
    }

    @Override
    public synchronized void removeEntry(long id) throws ConnectException {
        List<Object> arguments = new ArrayList<>();
        arguments.add(user.getProfile());
        arguments.add(id);
        sendUpdate(OperationType.DELETE, arguments);
        dbHandler.getEntryManager().removeEntry(user.getId(), user.getProfile(), id);
    }

    @Override
    public synchronized Optional<Entry> getEntry(long id) {
        return dbHandler.getEntryManager().getEntry(user.getId(), user.getProfile(), id);
    }

    @Override
    public List<Entry> loadEntries() {
        return dbHandler.getEntryManager().getEntries(user.getId(), user.getProfile());
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
    public synchronized void swapEntries(long id, int position) throws PositionOutOfBoundException, ConnectException {
        dbHandler.getEntryManager().swapEntries(user.getId(), user.getProfile(), id, position);
        List<Object> arguments = new ArrayList<>();
        arguments.add(user.getProfile());
        arguments.add(id);
        arguments.add(position);
        sendUpdate(OperationType.SWAP, arguments);
    }

    @Override
    public synchronized void swapListEntries(long list, long id, int position) throws PositionOutOfBoundException, ConnectException {
        dbHandler.getListManager().swapListEntries(user.getId(), user.getProfile(), list, id, position);
        List<Object> arguments = new ArrayList<>();
        arguments.add(user.getProfile());
        arguments.add(id);
        arguments.add(position);
        sendUpdate(OperationType.SWAP_LIST, arguments);
    }

    @Override
    public synchronized void updateTask(long id, String value) throws ConnectException {
        List<Object> arguments = new ArrayList<>();
        arguments.add(user.getProfile());
        arguments.add(id);
        arguments.add(value);
        sendUpdate(OperationType.UPDATE_LIST, arguments);
        dbHandler.getEntryManager().updateTask(user.getId(), user.getProfile(), id, value);
    }

    @Override
    public synchronized void updateFocus(long id, boolean value) throws ConnectException {
        List<Object> arguments = new ArrayList<>();
        arguments.add(user.getProfile());
        arguments.add(id);
        arguments.add(value);
        sendUpdate(OperationType.UPDATE_LIST, arguments);
        dbHandler.getEntryManager().updateFocus(user.getId(), user.getProfile(), id, value);
    }

    @Override
    public synchronized void updateDropped(long id, boolean value) throws ConnectException {
        List<Object> arguments = new ArrayList<>();
        arguments.add(user.getProfile());
        arguments.add(id);
        arguments.add(value);
        sendUpdate(OperationType.UPDATE_LIST, arguments);
        dbHandler.getEntryManager().updateDropped(user.getId(), user.getProfile(), id, value);
    }

    @Override
    public synchronized void updateList(long id, Long newId) throws ConnectException {
        List<Object> arguments = new ArrayList<>();
        arguments.add(user.getProfile());
        arguments.add(id);
        arguments.add(newId);
        sendUpdate(OperationType.UPDATE_LIST, arguments);
        dbHandler.getListManager().updateList(user.getId(), user.getProfile(), id, newId);
    }

    @Override
    public synchronized void updateReminderDate(long id, Instant value) throws ConnectException {
        List<Object> arguments = new ArrayList<>();
        arguments.add(user.getProfile());
        arguments.add(id);
        arguments.add(value);
        sendUpdate(OperationType.UPDATE_REMINDER_DATE, arguments);
        dbHandler.getEntryManager().updateReminderDate(user.getId(), user.getProfile(), id, value);
    }

    @Override
    public synchronized void updateRecurrence(long id, String value) throws ConnectException {
        List<Object> arguments = new ArrayList<>();
        arguments.add(user.getProfile());
        arguments.add(id);
        arguments.add(value);
        sendUpdate(OperationType.UPDATE_RECURRENCE, arguments);
        dbHandler.getEntryManager().updateRecurrence(user.getId(), user.getProfile(), id, value);
    }

    @Override
    public synchronized void updateListColor(long list, String color) throws ConnectException {
        List<Object> arguments = new ArrayList<>();
        arguments.add(list);
        arguments.add(color);
        sendUpdate(OperationType.UPDATE_LIST_COLOR, arguments);
        dbHandler.getListManager().updateListColor(list, color);
    }

    @Override
    public synchronized void updateUserName(String name) throws ConnectException {
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
        HttpURLConnection connection = sendPostMessage(PROTOCOL + "://" + SERVER + "/auth/status", loginRequest);
        String body = getBody(connection);
        if (!body.equals("non"))
            throw new InvalidActionException("User with mail address already exists.");
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
