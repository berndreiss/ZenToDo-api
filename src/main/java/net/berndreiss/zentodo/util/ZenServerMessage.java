package net.berndreiss.zentodo.util;

import net.berndreiss.zentodo.OperationType;

import java.time.Instant;
import java.util.List;

public class ZenServerMessage extends ZenMessage{

    private final Instant timeStamp;

    public ZenServerMessage(){
        super();
        timeStamp = Instant.now();
    }
    public ZenServerMessage(OperationType type, List<Object> arguments){
        super(type, arguments);
        timeStamp = Instant.now();

    }

    public Instant getTimeStamp() {
        return timeStamp;
    }
}
