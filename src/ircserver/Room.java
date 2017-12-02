package ircserver;

//room(channel) class
public class Room {
	private String name = null;//name of room
	private RoomClientList CL = null;// client list, each room has a list of users who joined the room

	//create room, a room is created when the first user join the room
	public Room(String name, ServerThread client){
		this.name = name;
		CL = new RoomClientList();
		CL.addClient(client);
	}

	//get the list of users in the room 
	public RoomClientList getClientList(){
		return CL;
	}

	//set name of the room
	public void setName(String name) {
		this.name = name;
	}

	//get name of the room
	public String getName() {
		return name;
	}

}
