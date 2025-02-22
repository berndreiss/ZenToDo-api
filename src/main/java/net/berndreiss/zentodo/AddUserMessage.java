package net.berndreiss.zentodo;

public class AddUserMessage {
    private String mail;

    public AddUserMessage(){}
    public AddUserMessage(String mail){this.mail = mail;}

    public String getMail(){return mail;}
    public void setMail(String mail) {this.mail = mail;}
}
