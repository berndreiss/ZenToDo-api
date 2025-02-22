package net.berndreiss.zentodo.util;

import net.berndreiss.zentodo.OperationType;
import net.berndreiss.zentodo.data.OperationField;

import java.util.List;

public class Message {

    private OperationType type;
    private List<Object> arguments;

    public Message(){}
    public Message(OperationType type, List<Object> arguments){
        this.type = type;
        this.arguments = arguments;
    }

    public OperationType getType(){return type;}
    public void setType(OperationType type){this.type = type;}
    public List<Object> getArguments(){return arguments;}
    public void setArguments(List<Object> arguments){this.arguments = arguments;}

}
