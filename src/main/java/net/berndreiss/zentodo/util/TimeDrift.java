package net.berndreiss.zentodo.util;

import java.time.Instant;

/**
 * TODO DESCRIBE
 */
public class TimeDrift implements Comparable<TimeDrift> {

    /**
     * TODO
     */
    public final long theta;
    /**
     * TODO
     */
    public final long delta;

    /**
     * TODO DESCRIBE
     * @param T1
     * @param T2
     * @param T3
     * @param T4
     */
    public TimeDrift(Instant T1, Instant T2, Instant T3, Instant T4){
        long t1 = T2.toEpochMilli() - T1.toEpochMilli();
        long t2 = T4.toEpochMilli() - T3.toEpochMilli();
        delta = (t1 + t2) / 2;
        theta = -t2 + delta;
    }

    @Override
    public int compareTo(TimeDrift timeDrift) {
        return delta - timeDrift.delta < 0 ? -1 : delta == timeDrift.delta ? 0 : 1;
    }

    /**
     * TODO DESCRIBE
     * @return
     */
    public static String getTimeStamp(){
        return Instant.now().toString();
    }

    /**
     * TODO DESCRIBE
     * @param timeStamp
     * @return
     */
    public static Instant parseTimeStamp(String timeStamp){
        return Instant.parse(timeStamp);
    }
}
