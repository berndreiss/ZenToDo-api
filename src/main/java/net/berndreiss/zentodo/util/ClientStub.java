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
    public static String SERVER = "10.0.0.6:8080/";
    private  User user;
    private TimeDrift timeDrift;

    public static void main(String[] args) throws IOException, InterruptedException, URISyntaxException {

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
    }

    /**
     * TODO DESCRIBE
     * @param passwordSupplier
     * @return
     */
    public String authenticate(Supplier<String> passwordSupplier){
        try {
            user = dbHandler.getUserByEmail(email);
            System.out.println(user==null);
            if (user == null) {
                System.out.println("USER==null");
                String loginRequest = getLoginRequest(email, passwordSupplier.get());

                int attempts = 0;
                while (attempts++ < 10){

                    HttpURLConnection connection = sendPostMessage("http://" + SERVER + "auth/register", loginRequest);
                    if (connection.getResponseCode() == 200){

                        String body = getBody(connection);
                        String[] ids  = body.split(",");
                        BufferedReader is = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                        String line;
                        while ((line = is.readLine()) != null)
                            System.out.println(line);
                        System.out.println(connection.getResponseMessage());
                        System.out.println(body);

                        if (Integer.parseInt(ids[0]) == 0) {
                            dbHandler.addUser(Long.parseLong(ids[1]), email, userName, Long.parseLong(ids[2]));
                            return "User was registered. Check your mails for verification.";
                        }
                        if (Integer.parseInt(ids[0]) == 1) {
                            dbHandler.addUser(Long.parseLong(ids[1]), email, userName, Long.parseLong(ids[2]));
                            dbHandler.enableUser(email);
                            dbHandler.setDevice(email, Long.parseLong(ids[2]));
                            user = dbHandler.getUserByEmail(email);
                            dbHandler.setToken(Long.parseLong(ids[1]), ids[3]);
                            return "User logged in.";
                        }
                        return "User was not registered because return code did not mean anything (code " + ids[0] + ").";
                    }
                }
            }

            if (!user.getEnabled()){
                System.out.println("USER!=ENABLED");
                String loginRequest = getLoginRequest(email, passwordSupplier.get());
                HttpURLConnection connection = sendPostMessage("http://" + SERVER + "auth/status", loginRequest);
                String body = getBody(connection);
                if (body.equals("exists"))
                    return "User already exists, but was not verified. Check your mails.";
                else if (body.startsWith("enabled")){
                    user.setEnabled(true);
                    dbHandler.enableUser(email);
                    dbHandler.setToken(user.getId(), body.split(",")[1]);
                    return "User logged in.";
                } else
                    return "Something went wrong when retrieving the status of the user from the server.";

            }
            String token = dbHandler.getToken(user.getId());

            if (token != null) {
                System.out.println("RENEW TOKEN");
                HttpURLConnection connection = sendPostMessage("http://" + SERVER + "auth/renewToken", getLoginRequest(email, token));
                if (connection.getResponseCode() == 200) {
                    dbHandler.setToken(user.getId(), getBody(connection));
                    return "User logged in.";
                }
            }

            String loginRequest = getLoginRequest(email, passwordSupplier.get());

            int attempts = 0;
            System.out.println("LOGIN");
            while (attempts++ < 10) {
                HttpURLConnection connection = sendPostMessage("http://" + SERVER + "auth/login", loginRequest);
                if (connection.getResponseCode() == 200) {
                    dbHandler.setToken(user.getId(), getBody(connection));
                    return "User logged in.";
                }
            }


            return "Something went wrong. Try logging in later.";

        } catch (Exception e) {
            if (exceptionHandler != null)
                exceptionHandler.handle(e);
            return "Something went wrong. Try logging in later.";
        }
    }

    /**
     * TODO
     */
    public void init() {

        try {
            // Set clock drift
            for (int i = 0; i < 8; i++) {
                sendAuthGetMessage("http://" + SERVER + "test");
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
            HttpURLConnection connection = sendAuthPostMessage("http://" + SERVER + "add", jsonifyList(list));

            System.out.println(jsonifyList(list));
            System.out.println(connection.getResponseCode());
            System.out.println("HEADER:" + connection.getHeaderField("t3"));

            client = new ZenWebSocketClient(rawMessage -> {

                    String id = getId(rawMessage);
                    JSONArray parsedMessage  = getMessage(rawMessage);

                try {
                    HttpURLConnection conn = sendAuthPostMessage("http://" + SERVER + "ackn", "{" +
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
        System.out.println(connection.getResponseCode());

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
        connection.setRequestProperty("email", user.getEmail());
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
        connection.setRequestProperty("email", user.getEmail());
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

        System.out.println(message.getType());

        for (Object o: message.getArguments())
            System.out.println(o);
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

    @Override
    public void updateUserName(long id, String name) {

    }

    @Override
    public boolean updateEmail(long id, String email) {

        return true;
    }

}
