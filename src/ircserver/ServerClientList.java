package ircserver;

//extended from ClientList
//purpose : manage all of active users(users who already got the nick name)
public class ServerClientList extends ClientList{
	
	//handle NICK command
	public synchronized void nick(ServerThread requestClient, String nickName){
	   ServerThread client = findClient(nickName); //find the requested nick name
	   if (client == null || client == requestClient){ //if the nick name is not used by any users
		   requestClient.setNickName(nickName); //assign the nick name for the client
		   add(requestClient); //add the client into the list of active users
		   requestClient.send("Your nickname is accepted!\n");
	   }
	   else //send error message
		   requestClient.send("Error: Nickname " + nickName + " is already in use");//ERR_NICKNAMEINUSE
	}
	
	//handle QUIT command
	//remove the user from the list of all active users
	public synchronized void quit(String nick){
		removeClient(nick);
	}

	//handle KICK command
    //disconnect a specific user
    public synchronized void kick(String nick) {
	    ServerThread client = findClient(nick);
	    if (client != null) {
	        client.handleKickCommand();
        }
    }

    //handle WHISPER command
    //send private message between 2 users
    public synchronized void whisper(String sender, String receiver, String text) {
        ServerThread fromClient = findClient(sender);
        ServerThread toClient = findClient(receiver);
        if (toClient != null) {
            fromClient.send(sender + "-" + receiver + " " + sender + " : " + text);
            toClient.send(receiver + "-" + sender + " " + sender + " : " + text);
        } else
            fromClient.send(receiver + " does not exist\n");
    }

    //handle FILE command
    //transfer file between 2 users
    public synchronized void fileTransfer(String sender, String receiver, String text) {
        ServerThread fromClient = findClient(sender);
        ServerThread toClient = findClient(receiver);
        if (toClient != null) {
            fromClient.send(sender + "-" + receiver + " " + sender + " : " + "file...sent");
            toClient.send(receiver + "-" + sender + " " + sender + " File" + " : " + text);
        } else
            fromClient.send(receiver + " does not exist\n");
    }
}