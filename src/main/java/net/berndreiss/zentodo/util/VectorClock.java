package net.berndreiss.zentodo.util;

import com.sun.istack.NotNull;
import net.berndreiss.zentodo.data.User;
import org.json.JSONObject;

import java.util.*;

/**
 * A vector clock: every device gets an entry in the vector represented by a key in the vectorMap, while the value is a
 * counter.  For every operation performed by a device the counter is incremented. Enables the distributed data to
 * establish an absolute order of operations performed.
 */
public class VectorClock implements  Comparable<VectorClock>{
    /** The vector map holding devices and count of operations. */
    public Map<Integer, Long> vectorMap = new HashMap<>();

    /** The device currently using this vector clock. */
    public Integer identity = null;

    /** Create a new instance of a vector clock. */
    public VectorClock(){}

    /**
     * Create a new instance of a vector clock.
     * @param identity the device using the vector clock
     */
    public VectorClock(Integer identity){
        this.identity = identity;
        vectorMap.put(identity, 0L);
    }

    /**
     * Create a new instance of a vector clock. The current device set for the user is used for the identity.
     * @param user the user using the vector clock
     */
    public VectorClock(User user){
        this(user.getClock());
        identity = user.getDevice();
    }

    /**
     * Create a new instance of a vector clock.
     * @param clock the clock as a JSON String
     */
    public VectorClock(String clock){
        this(new JSONObject(clock));
    }

    /**
     * Create a new instance of a vector clock
     * @param clock the clock as a JSON object
     */
    public VectorClock(JSONObject clock){
        //DO NOT USE toMap() HERE, ANDROID DOESN'T LIKE THAT
        for (Iterator<String> it = clock.keys(); it.hasNext(); ) {
            String key = it.next();
            vectorMap.put(Integer.parseInt(key), Long.parseLong(clock.getString(key)));
        }
    }

    @Override
    public int compareTo(VectorClock other) {
        return -changeDifference(other);
    }

    /**
     * Calculate the change difference by counting changes made by other devices in this clock and comparing it to
     * changes made by other devices in the other clock. We disregard own changes, since when receiving data from the
     * server we might already 'be ahead'. This way we can determine, if our clock is behind or in front of the other.
     * @param other the other vector clock to compare to
     * @return the change difference as an integer
     */
    public int changeDifference(@NotNull VectorClock other){
        return (int) (other.countChanges(identity) - this.countChanges(identity));
    }

    /**
     * Count changes made by other devices.
     * @param identity the identity to disregard
     * @return the changes made by other devices
     */
    private long countChanges(Integer identity){
        Long changes = 0L;
        for (Integer key: vectorMap.keySet()) {
            if (Objects.equals(key, identity))
                continue;
            changes += vectorMap.get(key);
        }
        return changes;
    }

    /**
     * Increment the counter for the current device.
     */
    public void increment(){
        if (identity == null)
            return;
        vectorMap.put(identity, vectorMap.get(identity)+1);
    }

    /**
     * Increment the counter for the device passed to the function.
     * @param device the device for which to increment the counter
     */
    public void increment(int device){
        if (vectorMap.get(device) == null)
            return;
        vectorMap.put(device, vectorMap.get(device) + 1);
    }

    /**
     * Add a new device to the vector clock.
     * @param device the id of the device to add
     */
    public void addDevice(int device){
        vectorMap.put(device, 0L);
    }

    /**
     * Transform the clock to a JSON String.
     * @return the jsonified clock
     */
    public String jsonify(){
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        String prefix = "  ";
        for (Integer key: vectorMap.keySet()){
            sb.append(prefix).append("\"").append(key).append("\": \"").append(vectorMap.get(key)).append("\"");
            prefix = ",\n  ";
        }
        sb.append("\n}");
        return sb.toString();
    }
}
