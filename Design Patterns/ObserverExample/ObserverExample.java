package observerexample;

import java.util.Observable;
import java.util.Observer;

public class ObserverExample {
    public static void main(String[] args) {
        ChatRoom chatRoomProgramming = new ChatRoom("Programming");
        ChatRoom chatRoomSports = new ChatRoom("Sports");
        
        //Create some users but don't subscribe them.
        User user1 = new User("user1");
        User user2 = new User("user2");
        User user3 = new User("user3");
        User user4 = new User("user4");
        User user5 = new User("user5");
        User user6 = new User("user6");
        User user7 = new User("user7");
        
        //user1 subscribes in Programming and Sports chat rooms.
        user2.subscribe(chatRoomProgramming);
        user2.subscribe(chatRoomSports);
        
        //user1 subscribes in Programming.
        user1.subscribe(chatRoomProgramming);
        //user1 and user2 must receive this message.
        user1.postMessage(chatRoomProgramming, "Hello");    
        System.out.println("------------------------------------------------------");
        
        //user3 subscribes in Programming.
        user3.subscribe(chatRoomProgramming);
        //user1, user2, user3 must receive this message.
        user2.postMessage(chatRoomProgramming, "Hello everyome!");
        System.out.println("------------------------------------------------------");

        //only user2 must receive this message. 
        user2.postMessage(chatRoomSports, "Hello and here!");
        System.out.println("------------------------------------------------------");

        user4.subscribe(chatRoomProgramming);
        user4.subscribe(chatRoomSports);
        user4.postMessage(chatRoomSports, "What sports do you like?");
        System.out.println("------------------------------------------------------");
        
        user2.postMessage(chatRoomProgramming, "Welcome user4!");
        System.out.println("------------------------------------------------------");
        
        user3.unsubscribe(chatRoomProgramming);
        
        user1.postMessage(chatRoomProgramming, "Who knows C++?");
        System.out.println("------------------------------------------------------");
        
        user1.subscribe(chatRoomSports);
        user2.unsubscribe(chatRoomSports);
        user4.postMessage(chatRoomSports, "I like football!");
        System.out.println("------------------------------------------------------");
        
        user5.subscribe(chatRoomSports);
        user5.subscribe(chatRoomProgramming);
        user6.subscribe(chatRoomSports);
        user7.subscribe(chatRoomProgramming);
        
        user4.postMessage(chatRoomProgramming, "I code in C++!");
        System.out.println("------------------------------------------------------");      
    }
}