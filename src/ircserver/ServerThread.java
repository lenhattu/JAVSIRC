package ircserver;

import javax.crypto.Cipher;
import java.io.*;
import java.net.Socket;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;

public class ServerThread extends Thread
{
    private Server server = null; //server thread, use to access shared resource
    private Socket socket = null; //socket used in this connection
    private String nickName  = null; //nick Name of the user in this connection
    private DataInputStream dataInputStream = null; //input stream
    private DataOutputStream dataOutputStream = null; //output stream
    private RoomList joinedRoomList = null;  //manage rooms are currently joined by user
    private String[] commands = {"NICK","QUIT","JOIN","LEAVE","LIST","USERS","SEND","WHISPER","SECURE","FILE","KEYGEN","DECRYPT","CLEAR"}; //list of supported commands

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
        //extract command, params, text, public key
        String text = null;
        String command = null;
        String param = null;
        String publicKey = null;
        //extract text
        String [] texts = message.split(":", 2);
        if (texts.length > 1)
            text = texts[1];
        //extract command, and params
        String [] command_and_params = message.split(" ", 0);
        command = command_and_params[0];
        //extract param from message if any
        if (command_and_params.length >= 2)
            param = command_and_params[1];
        //extract public key if any
        if (command_and_params.length >= 3)
            publicKey = command_and_params[2];

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
                case 0:  handleNickCommand(param); //change nick
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
                case 8:  handleSecureCommand(param, publicKey, text);
                    break;
                case 9:  handleFileCommand(param, text);
                    break;
                case 10: handleKeygenCommand();
                    break;
                case 11: handleDecryptCommand(param, text);
                    break;
                case 12: handleClearCommand();
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
        for (int i = 0; i < 13; i++)
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
                this.send("Error: Exceed the maximum length of a nickname");
            else if (!checkParamFormat(param))
                this.send("Error: Invalid nickname "+ param);//ERR_ERRONEUSNICKNAME
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

    //handle SECURE command
    private void handleSecureCommand(String param, String publicKey, String text) {
        if (param == null || publicKey == null)
            this.send("Error: SECURE did not have enough parameters\n"); //ERR_NEEDMOREPARAMS
        else if (text == null || text.equals("")) //ERR_NOTEXTTOSEND
            this.send("Error: No text to send\n");
        else {
            //encrypt with public key and whisper
            try {
                String encryptedText = encrypt(text, publicKey);
                server.getClientList().whisper(this.nickName, param, encryptedText);
            } catch (Exception e) {
                this.send("Error: encryption " + e.getMessage());
            }
        }
    }

    //encryption
    private static String encrypt(String input, String publicKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1PADDING");
        cipher.init(Cipher.ENCRYPT_MODE, loadPublicKey(publicKey));
        byte[] inputBytes = input.getBytes();
        byte[] outputBytes = cipher.doFinal(inputBytes);
        return Base64.getEncoder().encodeToString(outputBytes);
    }

    //decryption
    private static String decrypt(String input, String privateKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1PADDING");
        cipher.init(Cipher.DECRYPT_MODE, loadPrivateKey(privateKey));
        byte[] inputBytes = Base64.getDecoder().decode(input);
        byte[] outputBytes = cipher.doFinal(inputBytes);
        return new String(outputBytes);
    }

    //convert String to PrivateKey
    public static PrivateKey loadPrivateKey(String input) throws GeneralSecurityException {
        byte[] clear = Base64.getDecoder().decode(input);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(clear);
        KeyFactory fact = KeyFactory.getInstance("RSA");
        PrivateKey priv = fact.generatePrivate(keySpec);
        Arrays.fill(clear, (byte) 0);
        return priv;
    }

    //convert String to PublicKey
    public static PublicKey loadPublicKey(String input) throws GeneralSecurityException {
        byte[] data = Base64.getDecoder().decode(input);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(data);
        KeyFactory fact = KeyFactory.getInstance("RSA");
        return fact.generatePublic(spec);
    }

    //convert PrivateKey to String
    public static String savePrivateKey(PrivateKey input) throws GeneralSecurityException {
        KeyFactory fact = KeyFactory.getInstance("RSA");
        PKCS8EncodedKeySpec spec = fact.getKeySpec(input,
                PKCS8EncodedKeySpec.class);
        byte[] packed = spec.getEncoded();
        String key64 = Base64.getEncoder().encodeToString(packed);

        Arrays.fill(packed, (byte) 0);
        return key64;
    }

    //convert PublicKey to String
    public static String savePublicKey(PublicKey input) throws GeneralSecurityException {
        KeyFactory fact = KeyFactory.getInstance("RSA");
        X509EncodedKeySpec spec = fact.getKeySpec(input,
                X509EncodedKeySpec.class);
        return Base64.getEncoder().encodeToString(spec.getEncoded());
    }

    //handle FILE command
    private void handleFileCommand(String param, String text) {
        if (param == null)
            this.send("Error: FILE did not have enough parameters\n"); //ERR_NEEDMOREPARAMS
        else if (text == null || text.equals("")) //ERR_NOTEXTTOSEND
            this.send("Error: Nothing to send\n");
        else {
            server.getClientList().fileTransfer(this.nickName, param, text);
        }
    }

    //handle KEYGEN command
    private void handleKeygenCommand() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(512);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();
            PublicKey publicKey = keyPair.getPublic();
            PrivateKey privateKey = keyPair.getPrivate();
            this.send("PrivateKey=" + savePrivateKey(privateKey) + "\n" + "PublicKey=" + savePublicKey(publicKey) + "\n");
        } catch (GeneralSecurityException e) {
            this.send("Error: Unable to generate key pair " + e.getMessage());
        }
    }

    //handle DECRYPT command
    private void handleDecryptCommand(String param, String text) {
        if (param == null)
            this.send("Error: DECRYPT did not have enough parameters\n"); //ERR_NEEDMOREPARAMS
        else if (text == null || text.equals("")) //ERR_NOTEXTTODECRYPT
            this.send("Error: No text to decrypt\n");
        else {
            //decryption
            try {
                String decryptedText = decrypt(text, param);
                this.send("Decrypted message = " + decryptedText + "\n");
            } catch (Exception e) {
                this.send("Error: decryption " + e.getMessage());
            }
        }
    }

    //handle CLEAR command
    private void handleClearCommand() {
        this.send("CLEAR");
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