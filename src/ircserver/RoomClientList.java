package ircserver;

//extended from client list
//purpose: manage all of users in one room
public class RoomClientList extends ClientList{
	
	//send message from one user to all users in one room
	public synchronized void sendMessage(String roomName, String nick, String input, ServerThread client) {
		if (findClient(nick) == null) //not in this room
			   client.send("Error: Cannot send to room " + roomName); //ERR_CANNOTSENDTOROOM
		else {
			for (int i = 0; i < size(); i++)
		          get(i).send(roomName + " " + nick + " : " + input);
		}
	}

	//send message from server to a room
    public synchronized void sendNotification(String roomName, String input){
	    for (int i = 0; i < size(); i++)
	        get(i).send(roomName + " " + "server" + " : " + input);
    }
	
	//find a user which has nick name <nick>
	public synchronized ServerThread findClient(String nick, Room r, ServerThread client){
		for (int i = 0; i < this.size() ; i++) //can replace by iterator
	         if (get(i).getNickName().equals(nick))
	            return get(i);
		client.send("Error: You're not in room " + r.getName());
	    return null;
	}
}
