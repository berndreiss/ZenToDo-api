package net.berndreiss.zentodo.util;

import net.berndreiss.zentodo.operations.OperationType;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ZenMessage {

    public final VectorClock clock;
    public final OperationType type;
    public final List<Object> arguments;

    public ZenMessage(OperationType type, List<Object> arguments, VectorClock clock){
        this.type = type;
        this.arguments = arguments;
        this.clock = clock;
    }

    /**
     * Parse a JSON String as a ZenMessage.
     * @param jsonString The String to be parsed.
     * @return the ZenMessage.
     */
    public static ZenMessage parse(String jsonString) {
        JSONObject obj = new JSONObject(jsonString);
        return parse(obj);
    }

    /**
     * Parse a JSON object as a ZenMessage.
     * @param obj The String to be parsed.
     * @return the ZenMessage.
     */
    public static ZenMessage parse(JSONObject obj){
        VectorClock clock = new VectorClock(obj.getJSONObject("clock"));
        OperationType type = OperationType.valueOf(obj.getString("type"));
        JSONArray array = obj.getJSONArray("arguments");
        List<Object> arguments = new ArrayList<>();
        for (int i = 0; i < array.length(); i++){
            arguments.add(array.get(i));
        }
        return new ZenMessage(type, arguments, clock);
    }

    /**
     * Parse a JSON array as a list of ZenMessages.
     * @param jsonArray Array to be parsed.
     * @return a list of ZenMessages.
     */
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

    /**
     * Returns a ZenMessage in JSON format.
     * @param message The message to jsonify.
     * @return the jsonified message.
     */
    public static String jsonifyMessage(ZenMessage message) {
        return jsonifyMessage(message, "");
    }

    /**
     * Returns a ZenMessage in JSON format.
     * @param message The message to jsonify.
     * @param whitespace The whitespace to be added at the beginning of each line.
     * @return the jsonified message.
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
     * Jsonifies a list of messages.
     * @param list The messages to be jsonified.
     * @return the jsonified list.
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

}
