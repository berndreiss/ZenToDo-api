package net.berndreiss.zentodo.util;

import com.sun.istack.NotNull;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.persistence.criteria.CriteriaBuilder;
import net.berndreiss.zentodo.data.*;
import net.berndreiss.zentodo.exceptions.*;
import net.berndreiss.zentodo.operations.ClientOperationHandlerI;
import net.berndreiss.zentodo.operations.OperationHandlerI;
import net.berndreiss.zentodo.operations.OperationType;
import net.berndreiss.zentodo.persistence.DbHandler;
import net.berndreiss.zentodo.tests.ListManagerTests;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * A client stub for communication with the server. If a user is initialized the stub synchronizes all operations
 * with the server. If no user is initialized a default user with id==0 is used.
 * If the server is not available all operations are stored in a queue and sent on the next interaction with the server.
 * //TODO make if possible for the client stub to be 'live only' -> no DB, just syncing with the server
 */
public class ClientStub implements OperationHandlerI {

    public static final Logger logger = LoggerFactory.getLogger(ClientStub.class);
    /**
     * The protocol being used for the websocket client
     */
    public static String WEBSOCKET_PROTOCOL = "wss";
    /**
     * The protocol being used with the server
     */
    public static String PROTOCOL = "https";
    /**
     * The server url
     */
    public static String SERVER = "zentodo.berndreiss.net/api";
    /**
     * The time drift with the server
     */
    //TODO implement time drift logic
    public static TimeDrift timeDrift = new TimeDrift();
    /**
     * The database for persisting data
     */
    public final Database dbHandler;
    /**
     * Handlers being triggered when data is being received
     */
    private final List<ClientOperationHandlerI> otherHandlers = new ArrayList<>();
    /**
     * The current user. Default user without online synchronization has id 0
     */
    @NotNull
    public User user;
    /**
     * Status in regard to the server
     */
    public Status status;
    /**
     * The websocket client for allowing the server to synch data live
     */
    private ZenWebSocketClient webSocketClient;
    /**
     * Prints informative messages (e.g., Problem connecting to server)
     */
    private Consumer<String> messagePrinter = _ -> {
    };
    /**
     * The current vector clock for the user
     */
    private VectorClock vectorClock;
    /**
     * Means of retrieving the password for login
     */
    private Supplier<String> passwordSupplier;

    /**
     * Creates a new instance of the client stub and sets the use to the default user.
     *
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

    //Method for testing purposes
    public static ClientStub getStub(String userName, String email, String persistenceUnit) {

        Path path = Paths.get(userName);

        try {
            Files.createDirectory(path);
            System.out.println("Directory created: " + path.toAbsolutePath());
        } catch (Exception e) {
            System.err.println("Failed to create directory: " + e.getMessage());
        }
        EntityManagerFactory emf = Persistence.createEntityManagerFactory(persistenceUnit);
        Database opHandler = new DbHandler(emf, userName);
        ClientStub stub = new ClientStub(opHandler);
        try {
            stub.init(email, userName, () -> "Test1234!?");
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        stub.setMessagePrinter(System.out::println);
        return stub;
    }

    //Method for testing purposes
    public static void main(String[] args) {
        for (int i = 0; i < 1; i++) {
            try {
                Files.createDirectory(Paths.get("user0"));
                Files.createDirectory(Paths.get("user1"));
                Files.createDirectory(Paths.get("user2"));
            } catch (Exception _) {
            }

            ClientStub stub0 = getStub("user0", "bd_reiss@yahoo.de", "ZenToDoPU");
            //List<Entry> entries0 = stub0.loadEntries();
            //ClientStub stub1 = getStub("user1", "bd_reiss@yahoo.de", "ZenToDoPU1");
            //List<Entry> entries1 = stub1.loadEntries();
            //ClientStub stub2 = getStub("user2", "bd_reiss@yahoo.de", "ZenToDoPU2");
            //List<Entry> entries2 = stub2.loadEntries();

            Task task0 = stub0.addNewTask("TASK0");
            //Entry entry1 = stub1.addNewEntry("TASK1");
            //Entry entry2 = stub2.addNewEntry("TASK2");

            //stub0.reinit();
            //stub1.reinit();
            //stub2.reinit();
            //Profile profile1 = dbHandler.getUserManager().addProfile(stub.user.getId());
            //stub.user.setProfile(profile1.getId());
            //Entry entry = stub.addNewEntry("Test");
            //stub.removeEntry(entry.getId());
            //Optional<Entry> entry1 = stub.getEntry(entry.getId());

            stub0.dbHandler.close();
            //stub1.dbHandler.close();
            //stub2.dbHandler.close();
        }
    }

    /**
     * Get the body of a URL connection.
     *
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
     * Get a body for a login request as a JSON format.
     *
     * @param email    The email for the login request.
     * @param password The password for the login request.
     * @return the login request.
     */
    private static String getLoginRequest(String email, String password) {
        return "{\"email\": \"" + email + "\", \"password\": \"" + password + "\"}";
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
        Optional<String> token = userManager.getToken(user.getId());

        if (token.isPresent()) {
            //Renew token and log in.
            HttpURLConnection connection = sendPostMessage(PROTOCOL + "://" + SERVER + "/auth/renewToken", getLoginRequest(email, token.get()));
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
     *
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
     *
     * @param email            Mail address of the user.
     * @param name             Name of the user.
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
        } catch (InvalidUserActionException e) {
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
            connection = sendAuthPostMessage(PROTOCOL + "://" + SERVER + "/process_operation", ZenServerMessage.jsonifyServerList(dbHandler.getUserManager().getQueued(user.getId())));
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
        System.out.println(response);
        JSONObject obj = new JSONObject(response);
        JSONArray array = (JSONArray) obj.get("message");
        List<ZenMessage> messages = ZenMessage.parseMessage(array);
        for (ZenMessage message : messages) {
            try {
                receiveMessage(message);
            } catch (PositionOutOfBoundException e) {
                logger.error("Position out of bound:\n{}", ZenMessage.jsonifyMessage(message), e);
                throw new RuntimeException(e);
            } catch (DuplicateIdException e) {
                logger.error("Attempt to add entry with duplicate id:\n{}", ZenMessage.jsonifyMessage(message), e);
                throw new RuntimeException(e);
            } catch (InvalidActionException e) {
                logger.error("Attempt to perform invalid action:\n{}", ZenMessage.jsonifyMessage(message), e);
                throw new RuntimeException(e);
            }
        }

        //Set up the websocket client and run it on another thread to retrieve messages live from the server
        webSocketClient = new ZenWebSocketClient(this::consumeMessage, this);
        final Optional<String> token = dbHandler.getUserManager().getToken(user.getId());
        if (token.isEmpty()) {
            logger.error("There is no token although the user is logged in.");
            throw new RuntimeException("There is no token although the user is logged in.");
        }

        Thread thread = new Thread(() -> {
            webSocketClient.connect(user.getEmail(), token.get(), user.getDevice());
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

    /**
     * Consume a raw message form the server:
     * - parses the message as a list of ZenMessage
     * - sends an acknowledgement for the message to the server
     * - processes the operations contained in the message
     */
    private void consumeMessage(String rawMessage) {

        JSONObject obj = new JSONObject(rawMessage);
        String id = obj.get("id").toString();
        JSONArray array = (JSONArray) obj.get("message");
        List<ZenMessage> messages = ZenMessage.parseMessage(array);

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
        } catch (IOException e) {
            status = Status.OFFLINE;
            return;
        }

        //Process the operations contained in the message
        for (ZenMessage zm : messages) {
            try {
                receiveMessage(zm);
            } catch (DuplicateIdException e) {
                logger.error("There was an attempt to add an entry with a duplicate id:\n{}", ZenMessage.jsonifyMessage(zm), e);
                throw new RuntimeException(e);
            } catch (InvalidActionException e) {
                logger.error("There was an attempt to perform an invalid action:\n{}", ZenMessage.jsonifyMessage(zm), e);
                throw new RuntimeException(e);
            } catch (PositionOutOfBoundException e) {
                logger.error("An entry was out of bound:\n{}", ZenMessage.jsonifyMessage(zm), e);
                throw new RuntimeException(e);
                //TODO remove ConnectException for other handlers?
            }
        }
    }

    /**
     * Send a non authenticated Post message.
     *
     * @param urlString URL to use.
     * @param body      Message body to send.
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

    /**
     * Send an authenticated post message to the server.
     * If the status is DIRTY also send the queue to the server.
     *
     * @param urlString URL to use.
     * @param body      The body to send.
     * @return a connection.
     * @throws IOException        Thrown when there is a problem communicating with the server.
     * @throws URISyntaxException Thrown when the URI is not valid.
     */
    public synchronized HttpURLConnection sendAuthPostMessage(String urlString, String body) throws IOException, URISyntaxException {

        //If the status is DIRTY send the queue to the server.
        if (status == Status.DIRTY) {
            HttpURLConnection connection = sendAuthPostMessage(PROTOCOL + "://" + SERVER + "/process_operation", ZenServerMessage.jsonifyServerList(dbHandler.getUserManager().getQueued(user.getId())));

            if (connection.getResponseCode() != 200) {
                throw new ConnectException("Something went wrong sending data to server.");
            }
            status = Status.ONLINE;
        }

        Optional<String> token = dbHandler.getUserManager().getToken(user.getId());
        if (token.isEmpty()){
            status = Status.OFFLINE;
            throw new ConnectException("No token has been found for th user.");
        }
        URL url = new URI(urlString).toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Authorization", "Bearer " + token.get());
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

    //TODO do we want this?

    /**
     * Processing the response involves calculating the time drift.
     *
     * @param connection the connection to be processed
     */
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

        } catch (DateTimeParseException _) {
        }

    }

    /**
     *
     */
    public void sync() {
        List<ZenServerMessage> queue = dbHandler.getUserManager().getQueued(user.getId());
    }

    /**
     * Add a handler for client side operations.
     *
     * @param operationHandler The handler to be added.
     */
    public void addOperationHandler(ClientOperationHandlerI operationHandler) {
        this.otherHandlers.add(operationHandler);
    }

    /**
     * Remove a handler for client side operations.
     *
     * @param operationHandler The handler to be removed.
     */
    public void removeOperationHandler(ClientOperationHandlerI operationHandler) {
        this.otherHandlers.remove(operationHandler);
    }

    /**
     * Get the message printer.
     *
     * @return the printer.
     */
    public Consumer<String> getMessagePrinter() {
        return messagePrinter;
    }

    /**
     * Set the message printer.
     *
     * @param messagePrinter Printer to set.
     */
    public void setMessagePrinter(Consumer<String> messagePrinter) {
        this.messagePrinter = messagePrinter;
    }

    //TODO replace this with headers?

    /**
     * Get the user currently initialized in the stub. If no specific user was initialized, the default user is set (id==0);
     *
     * @return the user currently initialized or the default user.
     */
    public User getUser() {
        return user;
    }

    /**
     * Parses the message and performs the operation it contains.
     *
     * @param message the message to process
     * @throws DuplicateIdException        thrown if task with duplicate id is added
     * @throws InvalidActionException      thrown if invalid action is performed
     * @throws PositionOutOfBoundException thrown if task or list entry is out of bounds
     */
    private void receiveMessage(ZenMessage message) throws DuplicateIdException, InvalidActionException, PositionOutOfBoundException {
        System.out.println(message.type);
        //TODO CHECK VALIDITY
        switch (message.type) {
            case POST -> {
                List<Object> args = message.arguments;
                int profile = Integer.parseInt(args.get(0).toString());
                long task = Long.parseLong(args.get(1).toString());
                String taskName = args.get(2).toString();
                int position = Integer.parseInt(args.get(3).toString());
                boolean focus = Boolean.parseBoolean(args.get(4).toString());
                boolean dropped = Boolean.parseBoolean(args.get(5).toString());
                Long list = args.get(6).toString().isEmpty() ? null : Long.parseLong(args.get(6).toString());
                Integer listPosition = args.get(7).toString().isEmpty() ? null : Integer.parseInt(args.get(7).toString());
                Instant reminderDate = args.get(8).toString().isEmpty() ? null : Instant.parse(args.get(8).toString());
                String recurrence = args.get(9).toString().isEmpty() ? null : args.get(9).toString();

                Task newTask = new Task(user.getId(), profile, taskName, position);
                newTask.setId(task);
                newTask.setFocus(focus);
                newTask.setDropped(dropped);
                newTask.setList(list);
                newTask.setListPosition(listPosition);
                newTask.setReminderDate(reminderDate);
                newTask.setRecurrence(recurrence);
                dbHandler.getTaskManager().postTask(newTask);
            }
            case ADD_NEW_TASK -> {
                //TODO handle id already existing -> check in with the server to get a new one for the other task
                //DO NOT ACKNOWLEDGE UNTIL WE HAVE THIS RESOLVED!
                List<Object> args = message.arguments;
                int profile = Integer.parseInt(args.get(0).toString());
                long task = Long.parseLong(args.get(1).toString());
                String taskName = args.get(2).toString();
                int position = Integer.parseInt(args.get(3).toString());
                Task newTask = dbHandler.getTaskManager().addNewTask(user.getId(), profile, task, taskName, position);
                for (OperationHandlerI oh : otherHandlers)
                    oh.addNewTask(newTask);
            }
            case ADD_NEW_LIST -> {
                List<Object> args = message.arguments;
                long task = Long.parseLong(args.get(0).toString());
                String name = args.get(1).toString();
                String color = args.get(2).toString().isEmpty() ? null : args.get(2).toString();
                dbHandler.getListManager().addList(task, name, color);
                for (OperationHandlerI oh: otherHandlers)
                    oh.addNewList(name, color);
            }
            case ADD_USER_PROFILE_TO_LIST -> {
                List<Object> args = message.arguments;
                long user = Long.parseLong(args.get(0).toString());
                int profile = Integer.parseInt(args.get(1).toString());
                long list = Long.parseLong(args.get(2).toString());
                dbHandler.getListManager().addUserProfileToList(user, profile, list);
            }
            case DELETE -> {
                int profile = Integer.parseInt(message.arguments.get(0).toString());
                long task = Long.parseLong(message.arguments.get(1).toString());
                dbHandler.getTaskManager().removeTask(user.getId(), profile, task);
                for (OperationHandlerI oh : otherHandlers)
                    oh.removeTask(task);
            }
            case SWAP -> {
                int profile = Integer.parseInt(message.arguments.get(0).toString());
                long task = Long.parseLong(message.arguments.get(1).toString());
                int position = Integer.parseInt(message.arguments.get(2).toString());
                dbHandler.getTaskManager().swapTasks(user.getId(), profile, task, position);
                for (OperationHandlerI oh : otherHandlers)
                    oh.swapTasks(task, position);
            }
            case SWAP_LIST -> {
                int profile = Integer.parseInt(message.arguments.get(0).toString());
                long task = Long.parseLong(message.arguments.get(1).toString());
                long list = Long.parseLong(message.arguments.get(2).toString());
                int position = Integer.parseInt(message.arguments.get(3).toString());
                dbHandler.getListManager().swapListEntries(user.getId(), profile, task, list, position);
                for (OperationHandlerI oh : otherHandlers)
                    oh.swapListEntries(list, task, position);
            }
            case UPDATE_TASK -> {
                int profile = Integer.parseInt(message.arguments.get(0).toString());
                long task = Long.parseLong(message.arguments.get(1).toString());
                String taskName = message.arguments.get(2).toString();
                dbHandler.getTaskManager().updateTask(user.getId(), profile, task, taskName);
                for (OperationHandlerI oh : otherHandlers)
                    oh.updateTask(task, taskName);
            }
            case UPDATE_FOCUS -> {
                int profile = Integer.parseInt(message.arguments.get(0).toString());
                long task = Long.parseLong(message.arguments.get(1).toString());
                boolean focus = Boolean.parseBoolean(message.arguments.get(2).toString());
                dbHandler.getTaskManager().updateFocus(user.getId(), profile, task, focus);
                for (OperationHandlerI oh : otherHandlers)
                    oh.updateFocus(task, focus);
            }
            case UPDATE_DROPPED -> {
                int profile = Integer.parseInt(message.arguments.get(0).toString());
                long task = Long.parseLong(message.arguments.get(1).toString());
                boolean dropped = Boolean.parseBoolean(message.arguments.get(2).toString());
                dbHandler.getTaskManager().updateDropped(user.getId(), profile, task, dropped);
                for (OperationHandlerI oh : otherHandlers)
                    oh.updateDropped(task, dropped);
            }
            case UPDATE_LIST -> {
                int profile = Integer.parseInt(message.arguments.get(0).toString());
                long task = Long.parseLong(message.arguments.get(1).toString());
                Long list = Long.parseLong(message.arguments.get(2).toString());
                dbHandler.getListManager().updateList(user.getId(), profile, task, list);
                for (OperationHandlerI oh : otherHandlers)
                    oh.updateList(task, list);
            }
            case UPDATE_REMINDER_DATE -> {
                int profile = Integer.parseInt(message.arguments.get(0).toString());
                long task = Long.parseLong(message.arguments.get(1).toString());
                Instant date = Instant.parse(message.arguments.get(1).toString());
                dbHandler.getTaskManager().updateReminderDate(user.getId(), profile, task, date);
                for (OperationHandlerI oh : otherHandlers)
                    oh.updateReminderDate(task, date);
            }
            case UPDATE_RECURRENCE -> {
                int profile = Integer.parseInt(message.arguments.get(0).toString());
                long task = Long.parseLong(message.arguments.get(1).toString());
                String recurrence = message.arguments.get(2).toString();
                dbHandler.getTaskManager().updateRecurrence(user.getId(), profile, task, recurrence);
                for (OperationHandlerI oh : otherHandlers)
                    oh.updateRecurrence(task, recurrence);
            }
            case UPDATE_LIST_COLOR -> {
                long list = Long.parseLong(message.arguments.get(0).toString());
                String color = message.arguments.get(1).toString();
                dbHandler.getListManager().updateListColor(list, color);
                for (OperationHandlerI oh : otherHandlers)
                    oh.updateListColor(list, color);
            }
            case UPDATE_USER_NAME -> {
                String name = message.arguments.getFirst().toString();
                dbHandler.getUserManager().updateUserName(user.getId(), name);
            }
            case UPDATE_MAIL -> {
                String mail = message.arguments.getFirst().toString();
                dbHandler.getUserManager().updateEmail(user.getId(), mail);
            }
            case UPDATE_ID -> {
                //TODO IMPLEMENT
            }
        }

    }

    /**
     * Sends an update with an operation to the server.
     *
     * @param type      The type of operation performed.
     * @param arguments The arguments for the operation.
     */
    private synchronized void sendUpdate(OperationType type, List<Object> arguments) {
        //Do nothing for default user
        if (user.getId() == 0)
            return;
        //Increment the vector clock by 1 and save it
        vectorClock.increment();
        dbHandler.getUserManager().setClock(user.getId(), vectorClock);
        //Create a server message list and send it to the server
        ZenServerMessage zm = new ZenServerMessage(type, arguments, vectorClock);
        List<ZenServerMessage> list = new ArrayList<>();
        list.add(zm);
        try {
            //reinit if we are not ONLINE
            if (status != Status.ONLINE)
                reinit();
            //Default error
            int responseCode = 404;
            if (status == Status.ONLINE) {
                HttpURLConnection connection = sendAuthPostMessage(PROTOCOL + "://" + SERVER + "/process_operation", ZenServerMessage.jsonifyServerList(list));
                responseCode = connection.getResponseCode();
            }
            if (responseCode != 200)
                throw new ConnectException("The server response was not valid: " + responseCode);
        } catch (URISyntaxException e) {//This should never happen
            logger.error("URI is not valid", e);
            throw new RuntimeException("URI is not valid", e);
        } catch (IOException e) {
            //If sending the update did not succeed, go offline and add update to the queue
            status = Status.OFFLINE;
            dbHandler.getUserManager().addToQueue(user, zm);
        }
    }

    @Override
    public synchronized Task addNewTask(String task) {
        //Add locally (done first to get an id)
        Task returnedTask = dbHandler.getTaskManager().addNewTask(user.getId(), user.getProfile(), task);
        if (returnedTask == null)
            return null;
        //Tell the server
        sendAddEntryUpdate(returnedTask);
        //Call handlers
        for (OperationHandlerI oh : otherHandlers) {
            try {
                oh.addNewTask(returnedTask);
            } catch (PositionOutOfBoundException | DuplicateIdException | InvalidActionException e) {
                logger.error("Error adding new task.", e);
                throw new RuntimeException(e);
            }
        }
        return returnedTask;
    }

    @Override
    public synchronized Task addNewTask(String task, int position) throws PositionOutOfBoundException {
        //Add locally (done first to get an id)
        Task returnedTask = dbHandler.getTaskManager().addNewTask(user.getId(), user.getProfile(), task, position);
        if (returnedTask == null)
            return null;
        //Tell the server
        sendAddEntryUpdate(returnedTask);
        //Call handlers
        for (OperationHandlerI oh : otherHandlers)
            try {
                oh.addNewTask(returnedTask);
            } catch (PositionOutOfBoundException | DuplicateIdException | InvalidActionException e) {
                logger.error("Error adding new task.", e);
                throw new RuntimeException(e);
            }
        return returnedTask;
    }

    //TODO THIS IS NOT IDEAL
    @Override
    public synchronized void addNewTask(Task task) throws PositionOutOfBoundException, DuplicateIdException, InvalidActionException {
        if (task == null)
            return;
        //Add locally (done first to get an id)
        dbHandler.getTaskManager().addNewTask(user.getId(), user.getProfile(), task.getId(), task.getTask(), task.getPosition());
        //Tell the server
        sendAddEntryUpdate(task);
        //Call handlers
        for (OperationHandlerI oh : otherHandlers)
            try {
                oh.addNewTask(task);
            } catch (PositionOutOfBoundException | DuplicateIdException | InvalidActionException e) {
                logger.error("Error adding new task.", e);
                throw new RuntimeException(e);
            }
    }

    //Helper method for sending operations of type ADD_NEW_ENTRY to the server
    private void sendAddEntryUpdate(Task task) {
        List<Object> arguments = new ArrayList<>();
        arguments.add(user.getProfile());
        arguments.add(task.getId());
        arguments.add(task.getTask());
        arguments.add(task.getPosition());
        sendUpdate(OperationType.ADD_NEW_TASK, arguments);
    }

    @Override
    public synchronized void removeTask(long id) {
        List<Object> arguments = new ArrayList<>();
        arguments.add(user.getProfile());
        arguments.add(id);
        //Tell the server
        sendUpdate(OperationType.DELETE, arguments);
        //Update locally
        dbHandler.getTaskManager().removeTask(user.getId(), user.getProfile(), id);
        //Call handlers
        for (OperationHandlerI oh : otherHandlers)
            oh.removeTask(id);
    }

    @Override
    public synchronized Optional<Task> getTask(long id) {
        return dbHandler.getTaskManager().getTask(user.getId(), user.getProfile(), id);
    }

    @Override
    public List<Task> loadTasks() {
        return dbHandler.getTaskManager().getTasks(user.getId(), user.getProfile());
    }

    @Override
    public List<Task> loadFocus() {
        return dbHandler.getTaskManager().loadFocus(user.getId(), user.getProfile());
    }

    @Override
    public List<Task> loadDropped() {
        return dbHandler.getTaskManager().loadDropped(user.getId(), user.getProfile());
    }

    @Override
    public List<Task> loadList(Long list) {
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
    public synchronized void swapTasks(long task, int position) throws PositionOutOfBoundException {
        dbHandler.getTaskManager().swapTasks(user.getId(), user.getProfile(), task, position);
        List<Object> arguments = new ArrayList<>();
        arguments.add(user.getProfile());
        arguments.add(task);
        arguments.add(position);
        //Tell the server
        sendUpdate(OperationType.SWAP, arguments);
        //Update locally
        dbHandler.getTaskManager().swapTasks(user.getId(), user.getProfile(), task, position);
        //Call handlers
        for (OperationHandlerI oh : otherHandlers)
            oh.swapTasks(task, position);
    }

    @Override
    public synchronized void swapListEntries(long list, long task, int position) throws PositionOutOfBoundException {
        dbHandler.getListManager().swapListEntries(user.getId(), user.getProfile(), task, list, position);
        List<Object> arguments = new ArrayList<>();
        arguments.add(user.getProfile());
        arguments.add(task);
        arguments.add(list);
        arguments.add(position);
        //Tell the server
        sendUpdate(OperationType.SWAP_LIST, arguments);
        //Update locally
        dbHandler.getListManager().swapListEntries(user.getId(), user.getProfile(), task, list, position);
        //Call handlers
        for (OperationHandlerI oh : otherHandlers)
            oh.swapListEntries(list, task, position);
    }

    @Override
    public synchronized void updateTask(long task, String value) {
        List<Object> arguments = new ArrayList<>();
        arguments.add(user.getProfile());
        arguments.add(task);
        arguments.add(value);
        //Tell the server
        sendUpdate(OperationType.UPDATE_TASK, arguments);
        //Update locally
        dbHandler.getTaskManager().updateTask(user.getId(), user.getProfile(), task, value);
        //Call handlers
        for (OperationHandlerI oh : otherHandlers)
            oh.updateTask(task, value);
    }

    @Override
    public synchronized void updateFocus(long task, boolean value) {
        List<Object> arguments = new ArrayList<>();
        arguments.add(user.getProfile());
        arguments.add(task);
        arguments.add(value);
        //Tell the server
        sendUpdate(OperationType.UPDATE_FOCUS, arguments);
        //Update locally
        dbHandler.getTaskManager().updateFocus(user.getId(), user.getProfile(), task, value);
        //Call handlers
        for (OperationHandlerI oh : otherHandlers)
            oh.updateFocus(task, value);
    }

    @Override
    public synchronized void updateDropped(long task, boolean value) {
        List<Object> arguments = new ArrayList<>();
        arguments.add(user.getProfile());
        arguments.add(task);
        arguments.add(value);
        //Tell the sever
        sendUpdate(OperationType.UPDATE_DROPPED, arguments);
        //Update locally
        dbHandler.getTaskManager().updateDropped(user.getId(), user.getProfile(), task, value);
        //Call handlers
        for (OperationHandlerI oh : otherHandlers)
            oh.updateDropped(task, value);
    }

    @Override
    public synchronized void updateList(long task, Long newId) throws InvalidActionException {
        List<Object> arguments = new ArrayList<>();
        arguments.add(user.getProfile());
        arguments.add(task);
        arguments.add(newId);
        //Tell the server
        sendUpdate(OperationType.UPDATE_LIST, arguments);
        //Update locally
        dbHandler.getListManager().updateList(user.getId(), user.getProfile(), task, newId);
        //Call handlers
        for (OperationHandlerI oh : otherHandlers)
            oh.updateList(task, newId);
    }

    @Override
    public synchronized void updateReminderDate(long task, Instant value) {
        List<Object> arguments = new ArrayList<>();
        arguments.add(user.getProfile());
        arguments.add(task);
        arguments.add(value);
        //Tell the server
        sendUpdate(OperationType.UPDATE_REMINDER_DATE, arguments);
        //Update locally
        dbHandler.getTaskManager().updateReminderDate(user.getId(), user.getProfile(), task, value);
        //Call handlers
        for (OperationHandlerI oh : otherHandlers)
            oh.updateReminderDate(task, value);
    }

    @Override
    public synchronized void updateRecurrence(long task, String value) {
        List<Object> arguments = new ArrayList<>();
        arguments.add(user.getProfile());
        arguments.add(task);
        arguments.add(value);
        //Tell the server
        sendUpdate(OperationType.UPDATE_RECURRENCE, arguments);
        //Update locally
        dbHandler.getTaskManager().updateRecurrence(user.getId(), user.getProfile(), task, value);
        //Call handlers
        for (OperationHandlerI oh : otherHandlers)
            oh.updateRecurrence(task, value);
    }

    @Override
    public synchronized void updateListColor(long list, String color) {
        List<Object> arguments = new ArrayList<>();
        arguments.add(list);
        arguments.add(color);
        //Tell the server
        sendUpdate(OperationType.UPDATE_LIST_COLOR, arguments);
        //Update locally
        dbHandler.getListManager().updateListColor(list, color);
        //Call handlers
        for (OperationHandlerI oh : otherHandlers)
            oh.updateListColor(list, color);
    }

    @Override
    public synchronized void updateUserName(String name) {
        if (user.getId() == 0)
            return;
        List<Object> arguments = new ArrayList<>();
        arguments.add(name);
        //Tell the server
        sendUpdate(OperationType.UPDATE_USER_NAME, arguments);
        //Update locally
        dbHandler.getUserManager().updateUserName(user.getId(), name);
        user.setUserName(name);
    }

    @Override
    public synchronized void updateEmail(@NotNull String email) throws InvalidActionException, IOException {
        if (user.getId() == 0)
            return;
        if (user.getEmail().equals(email))
            return;
        String password = passwordSupplier.get();
        String loginRequest = getLoginRequest(email, password);

        //Check whether user with mail address already exists
        HttpURLConnection connection = sendPostMessage(PROTOCOL + "://" + SERVER + "/auth/status", loginRequest);
        String body = getBody(connection);
        if (!body.equals("non"))
            throw new InvalidUserActionException("User with mail address already exists.");
        //Tell the server, that the mail address changed
        List<Object> arguments = new ArrayList<>();
        arguments.add(email);
        sendUpdate(OperationType.UPDATE_MAIL, arguments);
        //Change mail locally
        dbHandler.getUserManager().updateEmail(user.getId(), email);
        user.setEmail(email);
    }

    @Override
    public List<TaskList> getLists() {
        return dbHandler.getListManager().getListsForUser(user.getId(), user.getProfile());
    }

    @Override
    public TaskList addNewList(String name, String color) throws InvalidActionException, DuplicateIdException {
        //TODO try to get List id from server
        long id = ListManagerTests.getUniqueListId(dbHandler);
        List<Object> arguments = new ArrayList<>();
        arguments.add(id);
        arguments.add(name);
        arguments.add(color == null ? "" : color);
        sendUpdate(OperationType.ADD_NEW_LIST, arguments);
        TaskList list = dbHandler.getListManager().addList(id, name, color);
        dbHandler.getListManager().addUserProfileToList(user.getId(), user.getProfile(), list.getId());
        arguments = new ArrayList<>();
        arguments.add(user.getId());
        arguments.add(user.getProfile());
        arguments.add(list.getId());
        sendUpdate(OperationType.ADD_USER_PROFILE_TO_LIST, arguments);
        //TODO make this one message
        for (OperationHandlerI oh: otherHandlers)
            oh.addNewList(name, color);
        return list;
    }

    /**
     * Clear the queue for the initialized user.
     */
    public synchronized void clearQueue() {
        if (user.getId() == 0)
            return;
        dbHandler.getUserManager().clearQueue(user.getId());
    }
}
