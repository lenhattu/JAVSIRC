package ircclient;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientThread extends Thread
{ 
   private Socket           socket   = null;//socket used by client
   private Client       thread   = null;//the thread in Client
   private DataInputStream dataInputStream = null;//input stream (get responses from server)
   private ChatSubWindow console  = null;//console window in panel
   
   public ClientThread(Client thread, Socket socket){
	  this.thread   = thread;
      this.socket   = socket;
      try{
    	 dataInputStream = new DataInputStream(socket.getInputStream());
      }
      catch(IOException ioe){
    	 System.out.println("Error getting input stream: " + ioe);
    	 thread.terminateAll();
      }  
   }
   
   //tasks of this thread is to wait for the response from server
   //and handle the response
   public void run(){
	  while (true){//wait for the message from server
		 try{ 
			 handleMessage(dataInputStream.readUTF());
         }
         catch(IOException e){
        	thread.getConsole().setDialog(e.getMessage());
            thread.close();
            this.close();
            System.exit(0);
         }
      }
   }
   
   //handle message received from server
   public void handleMessage(String msg){
	  if (msg.equals("QUIT")){
		  thread.terminateAll();      
      }
	  else{ 
    	  ChatWindow ccw = thread.getClientChatWindow();//get the panel
    	  //first send response into console window
    	  if (console == null)
    		  console = ccw.findChatWindow("Console");
    	  console.setDialog(msg);
    	  //check if it is a message between users in a room
    	  //by check the first part of the response, if it starts with #, beginning of room name
    	  //next, is name of the user, next ":", then, we know this is the send message
    	  //so forward the message to correct chat window(tab)
    	  //create chat window if it did not exist
    	  String [] parse = msg.split(" ",4);
    	  if (parse.length >=3 && parse[0].charAt(0) == '#' && parse[2].equals(":")){
           	  ChatSubWindow chatwnd = ccw.findChatWindow(parse[0]);
        	  if (chatwnd == null)
        		  chatwnd = ccw.addChatWindow(parse[0]);
        	  chatwnd.setDialog(parse[1]+" : "+parse[3]);//change the dialog of the chat window
    	  }
      }
   }
   
   //close resources used by this thread
   public void close(){
	  try{
	   	  if (dataInputStream != null) dataInputStream.close();//close stream
      }
      catch(IOException e){
    	  thread.getConsole().setDialog(e.getMessage());
      }
   }
   
}