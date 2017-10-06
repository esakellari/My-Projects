package observerexample;

import java.util.Observable;

public class ChatRoom extends Observable {
    
    private String chatRoomName = null;
    
    ChatRoom(String chatRoomName){
        this.chatRoomName = chatRoomName;
    }
    
    public String getChatRoomName(){
        return chatRoomName;
    }
    
    public void addNewMessage(Message message){
        System.out.println(message.getUser().getUsername() + " posted a new message : \"" 
                + message.getText() + "\" in chat room \"" + chatRoomName + "\" .");
        this.setChanged();
        this.notifyObservers(message);
    }
}