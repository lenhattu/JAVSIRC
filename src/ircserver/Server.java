package ircserver;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server implements Runnable {
    private ServerClientList serverClientList = new ServerClientList();//list of active users
    private RoomList roomList = new RoomList();// list of rooms in server
    private ServerSocket serverSocket = null; // server's welcoming socket
    private Thread waitingThread = null; // server waitingThread

    public Server(int port){
        //create server welcoming socket
        try{
            serverSocket = new ServerSocket(port);
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

        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEADING));

        JLabel label = new JLabel("");
        panel.add(label);

        frame.getContentPane().add(panel);
        if (args.length != 1)
            label.setText("Missing port argument...");
        else{
            Server server = new Server(Integer.parseInt(args[0]));
            label.setText("Listening on port: " + args[0]);
            JTextField textField = new JTextField();
            textField.setPreferredSize(new Dimension(90, 30));
            panel.add(textField);

            JButton button = new JButton("Kick");
            button.setPreferredSize(new Dimension(60, 30));
            button.setModel(new DefaultButtonModel());
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    server.serverClientList.kick(textField.getText());
                }
            });
            panel.add(button);
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
                connectClient(serverSocket.accept());
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
