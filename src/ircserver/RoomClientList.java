package ircserver;

//extended from client list
//purpose: manage all of users in one room
public class RoomClientList extends ClientList{
	
	//send message from one user to all of users in one room
	public synchronized void sendMessage(String roomName, String nick, String input, ServerThread client){
		if (findClient(nick) == null)//not in this channel
			   client.send("Error: Cannot send to channel "+roomName);//ERR_CANNOTSENDTOCHAN
		else{
			for (int i = 0; i < size(); i++)
		          get(i).send(roomName+" "+nick + " : " + input);
		}
		
	}
	
	//find a user which has nick name <nick>
	public synchronized ServerThread findClient(String nick, Room r, ServerThread client){
		for (int i = 0; i < this.size() ; i++)//can replace by iterator
	         if (get(i).getNickName().equals(nick))
	            return get(i);
		client.send("Error: You're not on that channel "+r.getName());  
	    return null;
	}
	
	
}
