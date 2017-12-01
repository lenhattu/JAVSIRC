package ircclient;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ChatSubWindow extends JPanel implements ActionListener {
	
	private JTextField input;
	private JTextArea dialog;
	private String ChatWindowName = null;
	private String newline = "\n";

	private Client thread = null;
	
	public ChatSubWindow(Client thread, String name) {
		this.thread = thread;
		ChatWindowName = name;
		createGui();
	}

	public void createGui() {
		input = new JTextField(25);
		input.addActionListener(this);
		
		dialog = new JTextArea(15, 25);
		dialog.setCaretPosition(dialog.getDocument().getLength());
		dialog.setEditable(false);
		JScrollPane scrollPane = new JScrollPane(dialog,
				JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
			        
		//gridBag is used to set up the layout of the panel
		GridBagLayout gridBag = new GridBagLayout();
		setLayout(gridBag);
		
		GridBagConstraints gridCons1 = new GridBagConstraints();
        gridCons1.gridwidth = GridBagConstraints.REMAINDER;
        gridCons1.fill = GridBagConstraints.HORIZONTAL;
        
        GridBagConstraints gridCons2 = new GridBagConstraints();
        gridCons2.weightx = 1.0;
        gridCons2.weighty = 1.0;
 
        //add text field, and text area into the panel
        add(scrollPane, gridCons1);
        add(input, gridCons2);
  	}
	
	//action performed when user hits enter
	public void actionPerformed(ActionEvent evt) {
		String text = input.getText();
		//keep select all previous input
		input.selectAll();
		//if user in console window
		if (this.ChatWindowName.equals("Console")){
			setDialog(text);//display the command entered by user in the console
			//handle LEAVE command entered by user
			//remove the chat window for the room specified in LEAVE command
			String [] parse = text.split(" ", 2);
			if (parse.length > 1 && parse[0].equals("LEAVE") && !parse[1].equals("Console")){//check it is the LEAVE command
				ChatSubWindow removingTab = thread.getClientChatWindow().findChatWindow(parse[1]);//find the chat window of specified room
				if (removingTab != null)// if there is a chat window for a specified room
					thread.getClientChatWindow().getTabbedwnd().remove(removingTab);//then delete the room
			}
			
			thread.send(text);//send the original message entered by user
		
		}
		else{//if not, add SEND <CHANNEL> before text, to create SEND command
			String sendingMessage = "SEND "+ChatWindowName+" :"+text;//send command 
			thread.getClientChatWindow().findChatWindow("Console").setDialog(sendingMessage);//add command to the console window
			thread.send(sendingMessage);//send command message
		}
	}
	
	//append new message to the dialog
	public void setDialog(String message){
		dialog.append(message+newline);
		dialog.setCaretPosition(dialog.getDocument().getLength());//force the panel scroll to the bottom of the text area
	}
	
}
