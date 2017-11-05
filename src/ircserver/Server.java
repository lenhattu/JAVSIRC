package ircserver;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server implements Runnable {
    private ServerClientList serverClientList = new ServerClientList();//list of active users
    private RoomList roomList = new RoomList();// list of rooms in server
    private ServerSocket server = null; // server's welcoming socket
    private Thread waitingThread = null; // server waitingThread

    public Server(int port){
        //create server welcoming socket
        try{
            server = new ServerSocket(port);
        }
        catch(IOException e){
            System.out.println("Can't use port " + port + ": " + e.getMessage());
        }
        //create and start server waitingThread
        waitingThread = new Thread(this);
        waitingThread.start();
    }

    public static void main(String[] args) {
        Server server = null;
        if (args.length != 1)
            System.out.println("Usage: java server.Server port");
        else
            server = new Server(Integer.parseInt(args[0]));
    }

    @Override
    public void run() {
        while (waitingThread != null){
            try{
                System.out.println("Waiting for incoming connection request...");
                connectClient(server.accept());
            }
            catch(IOException e){
                System.out.println("Can't accept this request: " + e);
                waitingThread.stop();
                waitingThread = null;
            }
        }
    }

    public ServerClientList getClientList(){
        return serverClientList;
    }

    public RoomList getRoomList(){
        return roomList;
    }

    //create,and start waitingThread serving a connection with client
    private void connectClient(Socket socket)
    {
        ClientThread client = new ClientThread(this, socket);
        try{
            client.open();
            client.start();
        }
        catch(IOException e){
            System.out.println("Thread error : " + e);
        }
    }
}
