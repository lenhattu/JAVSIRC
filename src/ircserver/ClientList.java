package ircserver;

import java.util.LinkedList;

//base class
//purpose: manage server threads running at sockets on server.
public class ClientList extends LinkedList<ServerThread>{
	
	//find server thread in the connection with client who has nick name <nick>
	public synchronized ServerThread findClient(String nick){
		for (int i = 0; i < this.size() ; i++)
	         if (get(i).getNickName().equals(nick))
	            return get(i);
        return null;
	}

	//remove from the list a pointer to the server thread which is in the connection with client who has nick name <nick>
	public synchronized void removeClient(String nick){
		ServerThread client = findClient(nick);
		if (client != null)
		    this.remove(client);
	}
	
	//add a pointer to the server thread to the list
	public synchronized void addClient(ServerThread client){
		this.addLast(client);
	}
	
	//list all of nick name of clients in the list
	public synchronized String listClient(){
		String list = "";
		for (int i = 0; i < this.size(); i++){
			list += get(i).getNickName() + " ";
		}
		return list;
	}
}