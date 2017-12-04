package ircclient;

import javax.swing.*;
import java.awt.*;

public class ChatWindow extends JPanel{
    
	private JTabbedPane tabbedWindows = null;
	private Client thread = null;
	
	public ChatWindow(Client thread) {
    	
    	//create tabbedPane tabbedWindows
    	//all of Chat windows are tabs in tabbedWindows
    	tabbedWindows = new JTabbedPane();
    	this.thread = thread;
    	
        tabbedWindows.addTab("Console",null, new ChatSubWindow(thread,"Console"));
        tabbedWindows.setSelectedIndex(0);

        //Add the tabbed pane to this panel.
        setLayout(new GridLayout(1, 1)); 
        add(tabbedWindows);
    }

	public JTabbedPane getTabbedwnd(){
		return tabbedWindows;
	}
	
	public ChatSubWindow addChatWindow(String roomName){
		ChatSubWindow cw = new ChatSubWindow(thread,roomName);
		tabbedWindows.addTab(roomName,null,cw);
		return cw;
	}
	
	public ChatSubWindow findChatWindow(String roomName){
		for (int i=0; i< tabbedWindows.getTabCount();i++){
			if (roomName.equals(tabbedWindows.getTitleAt(i)))
				return (ChatSubWindow)tabbedWindows.getComponentAt(i);
		}
		return null;
	}
	
}
