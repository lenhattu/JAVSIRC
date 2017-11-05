package ircserver;

//room(channel) class
public class Room {
	private String name = null;//name of room
	private String topic = "No topic is set";//topic of room
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
	
	//set the topic of the room
	public  void setTopic(String topic) {
		synchronized (topic) {// a thread need to get lock before trying to set the topic
			this.topic = topic;	
		}
	}
	
	//get the topic of the room
	public  String getTopic() {
		synchronized (topic) {// a thread need to get lock before trying to get the topic
			return topic;
		}
	}
	//handle command TOPIC, need to be sync with setTopic, and getTopic
	public void topic(String room, String text, ServerThread client){
		synchronized (topic) {// topic command need to get lock of topic before it can set, and return the new topic to user
			if (text != null && !text.equals(""))// if <topic> given, set topic of the channel
				setTopic(text);
			client.send(room+" "+getTopic());//RPL_TOPIC or RPL_NOTOPIC
		}
		
	}
	
	
}
