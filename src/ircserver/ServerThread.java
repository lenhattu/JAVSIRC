package ircserver;

import java.io.*;
import java.net.Socket;

public class ServerThread extends Thread
{
    private Server server = null; //server thread, use to access shared resource
    private Socket socket = null; //socket used in this connection
    private String nickName  = null; //nick Name of the user in this connection
    private DataInputStream dataInputStream = null; //input stream
    private DataOutputStream dataOutputStream = null; //output stream
    private RoomList joinedRoomList = null;  //manage rooms are currently joined by user
    private String[] commands = {"NICK","QUIT","JOIN","LEAVE","LIST","USERS","SEND","WHISPER","FILE"}; //list of supported commands

    public ServerThread(Server server, Socket socket){
        super();
        this.server = server;
        this.socket = socket;
        joinedRoomList = new RoomList();
    }

    //receive messages from user
    //analyze the message, if the message in right format, run the corresponding tasks.
    public void run() {
        while (true){
            if (Thread.interrupted())
                break;
            try{
                handleCommand(dataInputStream.readUTF());//handle message
            }
            catch(IOException e){//if thread can not read input from input stream 
                System.out.println("Can not read input stream : " + e.getMessage());
                handleCorruptedConnection(); //then remove it from all of its joined room, and the list of users
            }
        }
    }

    //handle corrupted connection
    //first, leave all of the rooms joined by this user and notify the rooms
    //second, if the room is empty, remove from room list
    //third, remove it from list of active users
    //finally, stop the thread
    private void handleCorruptedConnection(){
        if (nickName != null){
            while (!joinedRoomList.isEmpty()){
                Room r = joinedRoomList.getFirst();
                RoomClientList roomCL = r.getClientList();
                roomCL.removeClient(this.nickName);//remove client from room
                roomCL.sendNotification(r.getName(), this.nickName + " crashed!\n");
                this.joinedRoomList.remove(r); //remove the room from joined rooms
                server.getRoomList().removeRoom(r.getName());//check if CL size = 0, then delete room
            }
            //now, the client is associated with no room, can remove it from the client list
            server.getClientList().quit(this.nickName);
        }
        System.out.println("Client " + nickName + " crash handled!");
        this.interrupt();
    }

    //handle messages received from a getClientList()
    private void handleCommand(String message) {
        //check the length of message , should <=510
        //extract command, params, text
        String text = null;
        String command = null;
        String param = null;
        //extract text
        String [] texts = message.split(":", 2);
        if (texts.length > 1)
            text = texts[1];
        //extract command, and params
        String [] command_and_params = message.split(" ", 0);
        command = command_and_params[0];
        //extract params from message, get the first param
        for (int i = 1; i < command_and_params.length; i++)
            if (!command_and_params[i].equals("")) {
                param = command_and_params[i]; //get param
                break;
            }

        //check whether this is message of inactive or active user
        if (nickName == null) {
            if (command.equals("NICK"))
                handleNickCommand(param);
            else if (command.equals("QUIT"))
                handleQuitCommand();
            else
                this.send("Error: you are not active user");
        }
        else {
            //only active users can use the commands below, check command, params, text
            switch (getCommandIndex(command)) {
                case 0:  handleNickCommand(param);//change nick
                    break;
                case 1:  handleQuitCommand();
                    break;
                case 2:  handleJoinCommand(param);
                    break;
                case 3:  handleLeaveCommand(param);
                    break;
                case 4:  handleListCommand(param);
                    break;
                case 5:  handleUsersCommand(param);
                    break;
                case 6:  handleSendCommand(param, text);
                    break;
                case 7:  handleWhisperCommand(param, text);
                    break;
                case 8:  handleFileCommand(param, text);
                    break;
                default: this.send("Error: not support command "+command);
            }
        }

    }

    //send response message to user
    public void send(String msg) {
        try {
            dataOutputStream.writeUTF(msg);
            dataOutputStream.flush();
        }
        catch(IOException e) {
            System.out.println("Can not use output stream : " + e.getMessage());
            handleCorruptedConnection();
        }
    }

    //get nick name of user
    public String getNickName(){
        return nickName;
    }

    //set nick name of user, user can change their nick name
    public void setNickName(String nick){
        nickName = nick;
    }

    //get list of joined rooms of user
    public RoomList getJoinedRoomList(){
        return this.joinedRoomList;
    }

    //get the index of a command
    private int getCommandIndex(String command) {
        for (int i = 0; i < 9; i++)
            if (commands[i].equals(command))
                return i;
        return -1;

    }

    //check format : param should contains only numbers and letters
    private boolean checkParamFormat(String param) {
        for (int i = 0;i < param.length(); i++)
            if (!Character.isLetterOrDigit(param.charAt(i)))
                return false;
        return true;
    }

    //handle NICK command
    private void handleNickCommand(String param) {
        if (param != null) {
            if (param.length() > 10) // A <nickname> has a maximum length of ten (10) characters.
                this.send("Error: exceed the maximum length of a nickname");
            else if (!checkParamFormat(param))
                this.send("Error: Erroneus nickname "+ param);//ERR_ERRONEUSNICKNAME
            else
                server.getClientList().nick(this, param);
        }
        else
            this.send("Error: No nickname given"); //ERR_NONICKNAMEGIVEN
    }

    //handle QUIT command
    private void handleQuitCommand() {
        if (nickName != null){
            //At first, leave all of the rooms joined by this getClientList()
            while (!joinedRoomList.isEmpty()){
                this.handleLeaveCommand(joinedRoomList.getFirst().getName());
            }
            //then the client is associated with no room, can remove it from the client list
            server.getClientList().quit(this.nickName);
        }
        try {
            //after performing all of tasks for QUIT command, send "QUIT" back to client
            //now client thread can close its connection and stop
            this.send("QUIT");
            //close all of streams
            if (dataInputStream != null)
                dataInputStream.close();
            if (dataOutputStream != null)
                dataOutputStream.close();
            if (socket != null)
                socket.close();
        }
        catch(IOException e) {
            System.out.println("Error: closing thread " + e);
        }
        this.interrupt();
    }

    //handle JOIN command
    private void  handleJoinCommand(String param){
        if (param == null)
            this.send("Error: JOIN did not have enough parameters");//ERR_NEEDMOREPARAMS
        else if (param.charAt(0)!='#' || !checkParamFormat(param.substring(1)))
            this.send("Error: No such room " + param + "\n");//ERR_NOSUCHROOM
        else if (param.length()>254)
            this.send("Error: exceed the maximum length of a room");//ERR_EXCEEDCHANNELMAXLENGTH
        else
            server.getRoomList().joinRoom(param, this);//succeed
    }

    //handle LEAVE command
    private void handleLeaveCommand(String param){
        if (param == null)
            this.send("Error: LEAVE did not have enough parameters");//ERR_NEEDMOREPARAMS
        else if (param.charAt(0) != '#' || !checkParamFormat(param.substring(1)))
            this.send("Error: No such room " + param + "\n");//ERR_NOSUCHROOM
        else{
            Room r = server.getRoomList().findRoom(param,this);
            if (r != null){
                RoomClientList roomCL = r.getClientList();
                if (roomCL.findClient(this.nickName, r,this) != null){ //if client in this channel
                    roomCL.removeClient(this.nickName);//remove client from room list
                    this.joinedRoomList.remove(r); //for manage joined rooms by client, remove one joined room
                    this.send("You left " + r.getName() + "\n");
                    server.getRoomList().removeRoom(param);//check if CL size = 0, then delete room
                }
            }
        }
    }

    //handle LIST command
    private void handleListCommand(String param){
        if (param == null)
            server.getRoomList().listRoom(this);
        else if (param.charAt(0)!='#' || !checkParamFormat(param.substring(1)))
            this.send("Error: No such room " + param + "\n");//ERR_NOSUCHCHANNEL
        else
            server.getRoomList().listRoom(param,this);
    }

    //handle USERS command
    private void handleUsersCommand(String param) {
        if (param == null)
            this.send("Error: USERS did not have enough parameter");//ERR_NEEDMOREPARAMS
        else if (param.charAt(0)!='#' || !checkParamFormat(param.substring(1)))
            this.send("Error: No such room " + param + "\n");//ERR_NOSUCHCHANNEL
        else {
            Room r = server.getRoomList().findRoom(param,this);
            if (r != null) { //room exists
                RoomClientList roomCL = r.getClientList();
                String message = "List of users: " + roomCL.listClient() + "\n";
                this.send(message);
            }
        }
    }

    //handle SEND command
    private void handleSendCommand(String param, String text) {
        if (param == null)
            this.send("Error: SEND did not have enough parameters\n"); //ERR_NEEDMOREPARAMS
        else if (param.charAt(0) != '#' || !checkParamFormat(param.substring(1)))
            this.send("Error: No such room " + param + "\n"); //ERR_NOSUCHROOM
        else if (text == null || text.equals("")) //ERR_NOTEXTTOSEND
            this.send("Error: No text to send\n");
        else {
            Room r = server.getRoomList().findRoom(param,this);
            if (r != null) {
                RoomClientList roomCL = r.getClientList();
                roomCL.sendMessage(param, this.nickName, text, this);
            }
        }
    }

    //handle WHISPER command
    private void handleWhisperCommand(String param, String text) {
        if (param == null)
            this.send("Error: WHISPER did not have enough parameters\n"); //ERR_NEEDMOREPARAMS
        else if (text == null || text.equals("")) //ERR_NOTEXTTOSEND
            this.send("Error: No text to send\n");
        else {
            server.getClientList().whisper(this.nickName, param, text);
        }
    }

    //handle FILE command
    private void handleFileCommand(String param, String text) {
        if (param == null)
            this.send("Error: WHISPER did not have enough parameters\n"); //ERR_NEEDMOREPARAMS
        else if (text == null || text.equals("")) //ERR_NOTEXTTOSEND
            this.send("Error: Nothing to send\n");
        else {
            server.getClientList().fileTransfer(this.nickName, param, text);
        }
    }

    //handle KICK from server
    public void handleKickCommand() {
        if (nickName != null) {
            //At first, leave all of the rooms joined by this getClientList()
            while (!joinedRoomList.isEmpty()) {
                this.handleLeaveCommand(joinedRoomList.getFirst().getName());
            }
            //then the client is associated with no room, can remove it from the client list
            server.getClientList().quit(this.nickName);
        }
        try {
            //notify
            this.send("\nYou have been kicked by server...");
            //close all of streams
            if (dataInputStream != null)
                dataInputStream.close();
            if (dataOutputStream != null)
                dataOutputStream.close();
            if (socket != null)
                socket.close();
        }
        catch(IOException e) {
            System.out.println("Error: closing thread " + e);
        }
        this.interrupt();
    }

    //create input stream, output stream buffer
    public void open() throws IOException
    {
        dataInputStream = new DataInputStream(new
                BufferedInputStream(socket.getInputStream()));
        dataOutputStream = new DataOutputStream(new
                BufferedOutputStream(socket.getOutputStream()));
    }
}