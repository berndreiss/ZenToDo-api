package net.berndreiss.zentodo.util;

import java.time.Instant;

/**
 * Time drift between server and client. This is based on the Network Time Protocol (NTP) and is calculated by
 * getting four time stamps:
 *   - one when the message has been sent by the client (t1),
 *   - one when the message was received by the server (t2),
 *   - one when the return message has been sent by the server (t3), and
 *   - one when the return message was received by the client (t4)
 * From this the time offset is calculated by ((t2 - t1) + (t3 - t4)) / 2 represented by theta and the delay by
 * (t4 - t1) - (t3 - t2) represented by delta.
 * TODO continue explaining
 */
public class TimeDrift implements Comparable<TimeDrift> {

    /** The time offset. */
    public final long theta;
    /** The time delay. */
    public final long delta;

    /**
     * Create new instance of time drift and set theta to 0 and delta to MAX_VALUE.
     */
    public TimeDrift(){
        theta = 0L;
        delta = Long.MAX_VALUE;
    }
    /**
     * Crate new time drift with actual values.
     * @param T1 the time stamp of sending the client message
     * @param T2 the time stamp of receiving the message at the server
     * @param T3 the time stamp of sending the server response message
     * @param T4 the time stamp of receiving the response message at the client
     */
    public TimeDrift(Instant T1, Instant T2, Instant T3, Instant T4){
        long t1 = T2.toEpochMilli() - T1.toEpochMilli();
        long t2 = T4.toEpochMilli() - T3.toEpochMilli();
        delta = (t1 + t2) / 2;
        theta = -t2 + delta;
    }

    /**
     * Get the offset of the time drift
     * @return theta
     */
    public long getOffSet(){
        return theta;
    }

    @Override
    public int compareTo(TimeDrift timeDrift) {
        return delta - timeDrift.delta < 0 ? -1 : delta == timeDrift.delta ? 0 : 1;
    }

    /**
     * Get the current time as a String.
     * @return the current time stamp
     */
    public static String getTimeStamp(){
        return Instant.now().toString();
    }

    /**
     * Parse a time string as an Instant.
     * @param timeStamp the time stamp
     * @return the instant
     */
    public static Instant parseTimeStamp(String timeStamp){
        if (timeStamp == null)
            return null;
        return Instant.parse(timeStamp);
    }
}
