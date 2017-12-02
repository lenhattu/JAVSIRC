package ircserver;

import javax.swing.*;
import java.awt.*;
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
        JFrame frame = new JFrame("IRC Server");
        frame.setPreferredSize(new Dimension(200, 100));
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        JLabel label = new JLabel("");
        frame.getContentPane().add(label, BorderLayout.NORTH);
        Server server = null;
        if (args.length != 1)
            label.setText("Missing arguments...");
        else{
            server = new Server(Integer.parseInt(args[0]));
            label.setText("Listening on port: " + args[0]);
        }
        frame.pack();
        frame.setVisible(true);
    }

    @Override
    public void run() {
        while (waitingThread != null){
            try{
                if (Thread.interrupted())
                    break;
                System.out.println("Waiting for incoming connection request...");
                connectClient(server.accept());
            }
            catch(IOException e){
                System.out.println("Can't accept this request: " + e);
                waitingThread.interrupt();
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

    //create and start a connection with client
    private void connectClient(Socket socket)
    {
        ServerThread client = new ServerThread(this, socket);
        try{
            client.open();
            client.start();
        }
        catch(IOException e){
            System.out.println("Thread error : " + e.getMessage());
        }
    }
}
