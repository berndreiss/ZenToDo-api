package net.berndreiss.zentodo.util;

import net.berndreiss.zentodo.data.User;
import org.json.JSONObject;

import java.util.*;

public class VectorClock implements  Comparable<VectorClock>{
    public Map<Integer, Long> entries = new HashMap<>();

    public Integer identity = null;

    public VectorClock(){}
    public VectorClock(Integer identity){
        this.identity = identity;
        entries.put(identity, 0L);
    }
    public VectorClock(User user){
        this(user.getClock());
        identity = user.getDevice();
    }

    public VectorClock(String clock){
        this(new JSONObject(clock));
    }

    public VectorClock(JSONObject clock){

        //DO NOT USE toMap() HERE, ANDROID DOESN'T LIKE THAT
        for (Iterator<String> it = clock.keys(); it.hasNext(); ) {
            String key = it.next();
            entries.put(Integer.parseInt(key), Long.parseLong(clock.getString(key)));
        }

    }

    @Override
    public int compareTo(VectorClock other) {
        return -changeDifference(other);
    }

    public int changeDifference(VectorClock other){
        return (int) (other.countChanges(identity) - this.countChanges(identity));
    }

    private long countChanges(Integer identity){
        Long changes = 0L;
        for (Integer key: entries.keySet()) {
            if (Objects.equals(key, identity))
                continue;
            changes += entries.get(key);
        }
        return changes;
    }

    public void increment(){
        if (identity == null)
            return;
        entries.put(identity, entries.get(identity)+1);
    }

    public void increment(int device){
        if (entries.get(device) == null)
            return;
        entries.put(device, entries.get(device) + 1);
    }

    public void addDevice(int device){
        entries.put(device, 0L);
    }


    public String jsonify(){

        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        String prefix = "  ";
        for (Integer key: entries.keySet()){
            sb.append(prefix).append("\"").append(key).append("\": \"").append(entries.get(key)).append("\"");
            prefix = ",\n  ";
        }
        sb.append("\n}");
        return sb.toString();

    }
}
