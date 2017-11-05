package ircserver;

import java.util.LinkedList;

//base class
//purpose: manage server threads running at sockets on server.
public class ClientList extends LinkedList<ClientThread>{
	
	//find server thread in the connection with client who has nick name <nick>
	public synchronized ClientThread findClient(String nick){
		for (int i = 0; i < this.size() ; i++)//can replace by iterator
	         if (get(i).getNickName().equals(nick))
	            return get(i);
		
	      return null;
	}

	//remove from the list a pointer to the server thread which is in the connection with client who has nick name <nick>
	public synchronized ClientThread removeClient(String nick){
		ClientThread client = findClient(nick);
		if (client == null)
			return null;
        this.remove(client);
        
		return client;
		
	}
	
	//add a pointer to the server thread to the list
	public synchronized void addClient(ClientThread client){
		this.addLast(client);
	}
	
	//list all of nick name of clients in the list
	public synchronized String listClient(){
		String list = new String();
		for (int i = 0;i< this.size();i++){
			list+=get(i).getNickName()+" ";
		}
		return list;
	}

}
