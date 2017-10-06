package observerexample;

public class Message {
    private User user = null;
    private String text = null;
    private ChatRoom chat = null;
    
    Message(User user, String text, ChatRoom chat){
        this.user = user;
        this.text = text;
        this.chat = chat;
    }
    
    public User getUser(){
        return user;
    }
    
    public String getText(){
        return text;
    }
    
    public ChatRoom getChat(){
        return chat;
    }
}