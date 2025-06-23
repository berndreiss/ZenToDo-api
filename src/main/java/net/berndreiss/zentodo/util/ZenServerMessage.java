package net.berndreiss.zentodo.util;

import net.berndreiss.zentodo.operations.OperationType;
import org.json.JSONObject;

import java.time.Instant;
import java.util.List;

/**
 * A message sent to the server. It adds a timestamp to the ZenMessage.
 */
public class ZenServerMessage extends ZenMessage {

    /**
     * The timestamp of the message.
     */
    public final Instant timeStamp;

    /**
     * Create a new instance of ZenServerMessage. Sets the timestamp to now.
     *
     * @param type      the type of the operation
     * @param arguments the arguments used in the operation
     * @param clock     the vector clock at the time of the operation
     */
    public ZenServerMessage(OperationType type, List<Object> arguments, VectorClock clock) {
        super(type, arguments, clock);
        timeStamp = Instant.now();

    }

    /**
     * Create a new instance of ZenServerMessage.
     *
     * @param type      the type of the operation
     * @param arguments the arguments used in the operation
     * @param clock     the vector clock at the time of the operation
     * @param timeStamp the timestamp of the message
     */
    public ZenServerMessage(OperationType type, List<Object> arguments, VectorClock clock, Instant timeStamp) {
        super(type, arguments, clock);
        this.timeStamp = timeStamp;

    }

    /**
     * Create a ZenServerMessage from a ZenMessage (aka add a timestamp). Set the timestamp to now.
     *
     * @param message the message without a timestamp
     */
    public ZenServerMessage(ZenMessage message) {
        this(message.type, message.arguments, message.clock);
    }

    /**
     * Create a ZenServerMessage from a ZenMessage (aka add a timestamp).
     *
     * @param message   the message without a timestamp
     * @param timeStamp the timestamp to add
     */
    public ZenServerMessage(ZenMessage message, Instant timeStamp) {
        this(message.type, message.arguments, message.clock, timeStamp);
    }

    /**
     * Parse a JSON String as a ZenServerMessage.
     *
     * @param jsonString the String to parse
     * @return the ZenServerMessage
     */
    public static ZenServerMessage parse(String jsonString) {
        JSONObject obj = new JSONObject(jsonString);
        Instant timeStamp = Instant.parse(obj.getString("timestamp"));
        return new ZenServerMessage(ZenMessage.parse(obj), timeStamp);
    }

    /**
     * Jsonify a list of server messages.
     *
     * @param list The messages to be jsonified.
     * @return the jsonified list.
     */
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

}
