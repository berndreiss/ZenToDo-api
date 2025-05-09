package net.berndreiss.zentodo.util;

import jdk.dynalink.linker.support.CompositeTypeBasedGuardingDynamicLinker;
import net.berndreiss.zentodo.OperationType;
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

    public static ZenMessage parse(String jsonString) {
        JSONObject obj = new JSONObject(jsonString);
        return parse(obj);
    }
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
}
