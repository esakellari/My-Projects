package observerexample;

import java.util.Observable;
import java.util.Observer;

//The class User plays the role of the Observer 
//in the Observer Pattern.
public class User implements Observer{
    
    private String username = null;

    User(String username){
       this.username = username;
    }
    
    String getUsername(){
        return username;
    }
    
    //Subscribe (or Log-in) to the chat.
    public void subscribe(Observable chat){
        chat.addObserver(this);
    }
    
    //Unsubscribe (or Log-out) from the chat.
    public void unsubscribe(Observable chat){
        chat.deleteObserver(this);
    }
    
    //Post a new message in the chat. 
    public void postMessage(ChatRoom chat, String text){
        Message message = new Message(this, text, chat);
        chat.addNewMessage(message);
    }
    
    //The update method of the Observer.
    public void update(Observable o, Object arg) {
       Message message = (Message) arg;
       
        System.out.println(this.username + " read the message \"" + message.getText() 
                + "\" from " + message.getUser().getUsername() + " in chat room \""
                + message.getChat().getChatRoomName() +"\".");
    }
}