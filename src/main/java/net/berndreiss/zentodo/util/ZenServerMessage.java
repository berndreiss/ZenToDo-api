package net.berndreiss.zentodo.util;

import net.berndreiss.zentodo.operations.OperationType;
import org.json.JSONObject;

import java.time.Instant;
import java.util.List;

public class ZenServerMessage extends ZenMessage{

    public final Instant timeStamp;

    public ZenServerMessage(OperationType type, List<Object> arguments, VectorClock clock){
        super(type, arguments, clock);
        timeStamp = Instant.now();

    }
    public ZenServerMessage(OperationType type, List<Object> arguments, VectorClock clock, Instant timeStamp){
        super(type, arguments, clock);
        this.timeStamp = timeStamp;

    }

    public ZenServerMessage(ZenMessage message){
        this(message.type, message.arguments, message.clock);
    }
    public ZenServerMessage(ZenMessage message, Instant timeStamp){
        this(message.type, message.arguments, message.clock, timeStamp);
    }

    public static ZenServerMessage parse(String jsonString){
        JSONObject obj = new JSONObject(jsonString);
        Instant timeStamp = Instant.parse(obj.getString("timestamp"));
        return new ZenServerMessage(ZenMessage.parse(obj), timeStamp);
    }

    /**
     * Jsonify a list of server messages.
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
