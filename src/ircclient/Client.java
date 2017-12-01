package ircclient;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

public class Client implements Runnable {

    private Socket socket = null;//socket used by clientThread threads
    private Thread thread = null;//this thread handle input from user to server
    private DataInputStream dataInputStream = null;//input stream
    private DataOutputStream dataOutputStream = null;//output stream
    private ClientThread clientThread = null;//this thread display message sent from server to user

    private ChatWindow clientWnd = null;
    private ChatSubWindow console = null;

    public Client(String serverName, int serverPort){
        try{
            socket = new Socket(serverName, serverPort);
            System.out.println("Connected!");
            //set up input stream from user, and output stream to server
            dataInputStream = new DataInputStream(System.in);
            dataOutputStream = new DataOutputStream(socket.getOutputStream());
            //create 2 threads
            clientThread = new ClientThread(this, socket);// clientThread thread handle display output received from server
            thread = new Thread(this);                  // thread thread handle get input from user and send to server
            clientThread.start();
            thread.start();
        }
        catch(UnknownHostException e){
            System.out.println("Unknown host: " + e.getMessage());
        }
        catch(IOException e){
            System.out.println("Unexpected exception: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        Client client = null;
        if (args.length != 2)
            System.out.println("Usage: java client.ChatClient host port");
        else
            client = new Client(args[0], Integer.parseInt(args[1]));
    }

    //get the reference to tabbed panel
    public ChatWindow getClientChatWindow(){
        return clientWnd;
    }

    //get the reference to console window
    public ChatSubWindow getConsole(){
        return console;
    }

    //purpose of this thread is waiting for command from user
    //then handle the command, and send command to server
    @Override
    public void run(){

        //create a frame contains a tabbed panel
        JFrame frame = new JFrame("Chat Client");
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {//event listener for closing window
                terminateAll();
            }
        });

        //create panel
        clientWnd = new ChatWindow(this);//create tabbed panel
        console = (ChatSubWindow)clientWnd.getTabbedwnd().getComponentAt(0);//get the console Window
        //add panel to the frame
        frame.getContentPane().add(clientWnd, BorderLayout.CENTER);
        frame.pack();
        frame.setVisible(true);

    }

    //send the message entered by user to server
    public void send(String message){
        try{
            dataOutputStream.writeUTF(message);//read input from clientThread, send to server
            dataOutputStream.flush();
        }
        catch(IOException e){
            console.setDialog("Can not send : " + e.getMessage());
            terminateAll();
        }
    }

    //close used resources by this thread
    public void close()
    {
        try
        {  if (dataInputStream != null)  dataInputStream.close();
            if (dataOutputStream != null)  dataOutputStream.close();
            if (socket    != null)  socket.close();
        }
        catch(IOException e){
            console.setDialog(e.getMessage());
        }
    }

    public void terminateAll(){
        //close resources used by 2 threads
        close();
        clientThread.close();
        //exit
        System.exit(0);
    }
}
