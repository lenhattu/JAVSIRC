package ircserver;

import java.util.LinkedList;

//list of rooms, manage rooms(channel) in the server
public class RoomList extends LinkedList<Room> {
	
	//create room
	public synchronized Room createRoom(String roomName, ServerThread client){
		Room r = new Room(roomName, client);
		this.addLast(r);//add the new room into room list
		return r;
	}
	
	//remove room from room list
	public synchronized void removeRoom(String roomName){
		Room r = findRoom(roomName);
		if (r != null && r.getClientList().size()==0)
			this.remove(r);
	}
	
	//first version of findRoom,need send the error message right after found the result before it go to wait state
	public synchronized Room findRoom(String roomName, ServerThread client){
		for(int i =0;i<this.size();i++){
			if (this.get(i).getName().equals(roomName))
				return get(i);
		}
		client.send("Error: No such channel "+roomName);//ERR_NOSUCHCHANNEL
		return null;
	}
	
	//2nd version of findRoom, don't need send error message like JOIN
	public synchronized Room findRoom(String roomName){
		for(int i =0;i<this.size();i++){
			if (this.get(i).getName().equals(roomName))
				return get(i);
		}
		return null;
	}
	
	//list the name of the room
	public synchronized void listRoom(String roomName, ServerThread client){
		String message = "-----Channel : Name-----\n";
		
		Room r = findRoom(roomName,client);
		if (r != null){
			message += "Listing..." + roomName + "\n";
		}
		message += "------End of /LIST-------\n";
		client.send(message);
		
	}
	
	//list all of the rooms in the server
	public synchronized void listRoom(ServerThread client){
		String message = "-----Channel : Name-----\n";
		for (int i =0;i<this.size();i++){
			Room r = this.get(i);
			message += "Listing..." + r.getName() + "\n";
		}
		message += "------End of /LIST-------\n";
		client.send(message);
	}
	
	//handle join room of user
	//need 2 syn resources
	public synchronized void joinRoom(String roomName, ServerThread client){
		Room r = findRoom(roomName);
		if (r != null){//if room exist, then add client into CL
			RoomClientList roomCL = r.getClientList();
			if (roomCL.findClient(client.getNickName()) == null)
				roomCL.addClient(client);	
		}
		else {//if not create new room, add client to CL
			r = createRoom(roomName, client);
		}
		if (r == null)//can not create room
			   client.send("Error: Cannot join channel " + roomName + "\n");//ERR_CHANNELISFULL
		   else{//send list of active users in the channel
			   String message = r.getName() + "\n";
			   message += "Active users: " + r.getClientList().listClient() + "\n";//RPL_NAMEREPLY
			   client.getJoinedRoomList().addLast(r);//add join room into joined room list of client, client can manage room it has joined
			   client.send(message);
		   }
	}
	
	
}
