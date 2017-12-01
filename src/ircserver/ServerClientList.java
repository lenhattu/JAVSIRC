package ircserver;

//extended from ClientList
//purpose : manage all of active users(users who already got the nick name)
public class ServerClientList extends ClientList{
	
	//handle NICK command
	public synchronized void nick(ServerThread requestClient, String nickName){
	   ServerThread client = findClient(nickName);//find the requested nick name
	   if (client == null || client == requestClient){// if the nick name is not used by any users
		   requestClient.setNickName(nickName);// assign the nick name for the client
		   add(requestClient);// add the client into the list of active users
		   requestClient.send("Your nickname is accepted!\n");
	   }
	   else//send error message
		   requestClient.send("Error: Nickname " +nickName+ " is already in use");//ERR_NICKNAMEINUSE  
	}
	
	//handle QUIT command
	//remove the user from the list of all active users
	public synchronized void quit(String nick){
		removeClient(nick);
		
	}
}
