package Server;


import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import Message.DoViewChange;
import Message.Message;
import Message.Prepare;
import Message.PrepareOk;
import Message.Reply;
import Message.Request;
import Message.StartView;
import Message.StartViewChange;
import Message.Util;
import Server.VRstate.ClientTab;


//************************
//group number: tfd013
//************************

public class UDPserver extends JFrame {
	 
	private static final long serialVersionUID = 1L;
	private JTextArea displayArea; // displays packets received
	private JTextField enterField; // for entering messages
	private DatagramSocket socket; // socket to connect to client
	//config,min_quorum should be loaded from file
	//private String config[][]={{"localhost","1020"},{"localhost","1021"},{"localhost","1022"}};
	public String config[][]={{"localhost","1020"},{"localhost","1021"},{"localhost","1022"}};
	private int min_quorum=2;	//minimum quorum f+1=2
	private int[] ports = {1020,1021,1022};
	private VRstate state;
	
	 	 
	 public UDPserver(int id) {
		 
		 super( "Replica"+id );
		 
		 int rep_id=id; //id of the replica  id=0 indicates the primary
		 state = new VRstate(id);
		 state.client_table = new HashMap<Integer,ClientTab>();
		 state.log = new ArrayList<Request>();
		 state.prepareOk_counter = new HashMap<Integer,Vector<Integer>>();
		 //state.doViewChange_counter = new HashMap<Integer,Vector<Integer>>();
		 state.doViewChange_counter = new HashMap<Integer,Vector<DoViewChange>>();
		
		 enterField = new JTextField( "Type command here" );
		 enterField.addActionListener(
			new ActionListener()
				{
					public void actionPerformed( ActionEvent event )
					{
						//command;
						String command = event.getActionCommand();
						executeCommand(command);
//						String message = event.getActionCommand();		//just for test
//						try {											//just for test
//							sendStartView(message);						//just for test
//						} catch (IOException e) {						//just for test
//							// TODO Auto-generated catch block			//just for test
//							e.printStackTrace();						//just for test
//						}
					} // end actionPerformed
				} // end inner class
			); // end call to addActionListener
			
		 add( enterField, BorderLayout.NORTH );
		 
		 displayArea = new JTextArea(); 								// create displayArea
		 add( new JScrollPane( displayArea ), BorderLayout.CENTER );
		 setSize( 400, 300 ); 											// set size of window
		 setVisible( true ); 											// show window
		
		 try {	// create DatagramSocket for sending and receiving packets
			 //socket = new DatagramSocket( ports[rep_id] );
			 socket = new DatagramSocket( Integer.parseInt(config[rep_id][1]) );
		 }	// end try
		 catch ( SocketException socketException ) {
			 socketException.printStackTrace();
			 System.exit( 1 );
		 } // end catch
		 
		 this.waitForPackets(); //starts listening
	 }

	 // end UDPserver constructor
		
	 //***************************
	 //*	WAIT FOR PACKETS     *
	 //***************************
	 
	 // wait for packets to arrive, display data and echo packet to client
	 public void waitForPackets() {
		 while ( true ) {
			 try {	// receive packet, display contents, return copy to client
				 byte data[] = new byte[ 100 ]; // set up packet
					
				 displayMessage("\n\n--------------------------------------------------\nwaiting for packet");
				 
				 DatagramPacket receivePacket = new DatagramPacket( data, data.length );
				 
				 socket.receive( receivePacket ); // wait to receive packet
					
				 // display information from received packet
				 String received_msg = new String( receivePacket.getData(), 0, receivePacket.getLength() );
					
				 Message test = Util.fromString(received_msg); // creating object from message
					
				 displayMessage( "\nPacket received:" + 
						 "\nFrom host: "+ receivePacket.getAddress() +
						 "\nHost port: "+ receivePacket.getPort() +
						 "\nLength: "+ receivePacket.getLength() +
						 "\nContaining:\n\t" + received_msg +
						 "\nType:\n\t"+ test.getClass().getName());
					
				 //ACTION RECOGNITION
				 //checks the type of the received messages and runs appropriate Action
				 if(test.getClass().getName().equals("Message.Request")){
					 
					 displayMessage("\nRequest received!");
					 processRequest((Request) test, receivePacket.getAddress(), receivePacket.getPort());
						
					//sendPrepareToReplicas( receivePacket );
				}
				else if (test.getClass().getName().equals("Message.Prepare")){
						
					displayMessage("\nPrepare received!");
					processPrepare((Prepare) test);
						
				}
				else if (test.getClass().getName().equals("Message.PrepareOk")){
						
					displayMessage("\nPrepareOK received!");
					processPrepareOk((PrepareOk) test, receivePacket);		//30-10-2012 - RO
						
				}
				else if (test.getClass().getName().equals("Message.StartViewChange")){
					
					displayMessage("\nStartViewChange received!");
					processStartViewChange((StartViewChange) test);		// RO
						
				} 
				else if (test.getClass().getName().equals("Message.DoViewChange")){
					
					displayMessage("\nDoViewChange received!");
					processDoViewChange((DoViewChange) test);		// RO
						
				}
				else if (test.getClass().getName().equals("Message.StartView")){
					
					displayMessage("\nStartView received!");
					processStartView((StartView) test);		// RO
						
				} 
				else{
					displayMessage("\nInvaild message received!");  // if the message type is invalid
				}
			} // end try
			catch ( IOException ioException ){
				displayMessage( ioException.toString() + "\n" );
				ioException.printStackTrace();
			} // end catch
		} // end while
	}// end method waitForPackets
		
		 
	//********************************************************************* 
	//								SENDERS
	//********************************************************************* 

	// echo packet to client
	private void sendPacketToClient( DatagramPacket receivePacket ) throws IOException {
			
		displayMessage( "\n\nEcho data to client..." );
			
		//create packet to send
		DatagramPacket sendPacket = new DatagramPacket(
				receivePacket.getData(), receivePacket.getLength(),
				receivePacket.getAddress(), receivePacket.getPort() );
		
		socket.send( sendPacket );		// send packet to client
		displayMessage( "Packet sent\n" );
	
	} // end method sendPacketToClient
		
	//Send PREPARE to replicas
	private void sendPrepareToReplicas (Request received_req) throws IOException {
			
		displayMessage("\n\nSending Prepare: request to all replicas.");
		//for loop should be here
		//create prepare message
		Prepare prepare_msg = new Prepare();
		prepare_msg.r = received_req; //take message from the client 
		prepare_msg.v = state.view_number; //send view number
		prepare_msg.n = state.op_number; //and current op_number
		prepare_msg.k = state.commit_number;
			
		//generate message
		String message = prepare_msg.toString();
		displayMessage( "\nPrepare: message\n" );
		byte data[] = message.getBytes();
			
		DatagramPacket sendPacket1 = new DatagramPacket( 
				data, data.length,
				InetAddress.getByName("localhost"), ports[1] ); //should be taken from config
			
		socket.send( sendPacket1 ); // send packet to the first replica
		
		DatagramPacket sendPacket2 = new DatagramPacket(  
				data, data.length,
				InetAddress.getByName("localhost"), ports[2] ); //should be taken from config
			
		socket.send( sendPacket2 ); // send packet to the second replica
			
		displayMessage( "Prepare sent\n" );
	
	} //end prepare sender
		
	//Send PREPAREOK to primary
	private void sendPrepareOkToPrimary (Prepare prep_msg) throws IOException {
		
		displayMessage("\n\nSending PrepareOK  to primary.\n");
					
		//generate message
		PrepareOk prepareOk_msg = new PrepareOk();
		prepareOk_msg.i = state.rep_number; 	//id of replica
		prepareOk_msg.n = prep_msg.n;			//op number
		prepareOk_msg.v = state.view_number;	//view number
			
		String message = prepareOk_msg.toString();
		byte data[] = message.getBytes();
			
		DatagramPacket sendPacket1 = new DatagramPacket( data, data.length,
					InetAddress.getByName("localhost"), ports[0] ); //should be taken from config
			
		socket.send( sendPacket1 ); // send packet to the primary

		displayMessage( "\nPrepareOK sent\n" );
	
	} //end prepareOk sender
		
	//Send REPLY to client
	private void sendReplyToClient (PrepareOk prepareOk_msg, Request request_msg) throws IOException {
		//reply sender - 30-10-2012 - RO
		String cli_add;
		int cli_port;
			
		//generate message
		Reply reply_msg = new Reply();
			 
		reply_msg.v = prepareOk_msg.v; 	//view number
		reply_msg.s = request_msg.s; 		//original seq from request
		reply_msg.x = request_msg.op + "- reply";		//????????
			
		String message = reply_msg.toString();
		displayMessage( "\nReply: message\n" );
		byte data[] = message.getBytes();
			
		cli_add=state.client_table.get(request_msg.c).c_add;
		cli_port=state.client_table.get(request_msg.c).c_port;
		//displayMessage( "\nIP: " + cli_add + "Port: " + cli_port );
		DatagramPacket sendPacket1 = null;
		
		sendPacket1 = new DatagramPacket( data, data.length, InetAddress.getByName(cli_add), cli_port );
			
		socket.send( sendPacket1 );
			
	}// end reply sender
	
	
	//Send REPLY to client
	private void sendDoViewChange () throws IOException {
					
		//generate message
		DoViewChange doViewChange_msg = new DoViewChange();
						 
		doViewChange_msg.v = state.view_number; 			//view number
		doViewChange_msg.l = state.log; 					//log
		doViewChange_msg.last_v = state.last_norm_view; 	//last view in normal state
		doViewChange_msg.n = state.op_number; 				//op number
		doViewChange_msg.k = state.commit_number; 			//commit number
		doViewChange_msg.i = state.rep_number; 				//replica number
			
				
		String message = doViewChange_msg.toString();
		displayMessage( "\nDoViewChange_msg:" + message + "\n" );
		byte data[] = message.getBytes();
			
		for (int r_id=0;r_id<config.length;r_id++){		
			DatagramPacket sendPacket1 = null;
			sendPacket1 = new DatagramPacket( data, data.length, InetAddress.getByName("localhost"), Integer.parseInt(config[r_id][1]) );
			socket.send( sendPacket1 );
		}	
				
	}// end reply sender
	
	
	//Send START VIEW to Servers
	private void sendStartView(DoViewChange doviewchange_msg) throws IOException{
	//private void sendStartView(String doviewchange_msg) throws IOException{
		//generate message
		StartView startView_msg = new StartView();
		
		startView_msg.v=doviewchange_msg.v;		//view number
		startView_msg.l=doviewchange_msg.l; 	//log
		startView_msg.n=doviewchange_msg.n; 	//current op_number
		startView_msg.k=doviewchange_msg.k; 	//current commit_number		
							
		String message = startView_msg.toString();
		displayMessage( "\nSTARTVIEW: message\n" );
		byte data[] = message.getBytes();
					
		displayMessage( "\nData: " + data );
		
		for (int r_id=0;r_id<config.length;r_id++){	
			DatagramPacket sendPacket1 = null;
			sendPacket1 = new DatagramPacket( data, data.length, InetAddress.getByName("localhost"), Integer.parseInt(config[r_id][1]) );
			
			socket.send( sendPacket1 ); // send packet to the second replica
		
			displayMessage( "\nStartView sent to Server " + r_id );
		}
	}
		
	//********************************************************************* 
	//							END OF SENDERS
	//*********************************************************************	
		
		
	//********************************************************************* 
	//							MESSAGE PROCESSORS
	//*********************************************************************	

	
	//request processor - processes request message and sends Prepare messages
	//private void processRequest(Request req_msg){		//30-10-2012 - RO
	private void processRequest(Request req_msg, InetAddress cli_add, int cli_port){	
		
		Request request = req_msg;
		ClientTab cli_tab = state.new ClientTab();			//initialize client table
			
		if(state.client_table.containsKey(request.c)){ 		//checks if client table exists for this client
			cli_tab =  state.client_table.get(request.c);	//loads client table
		}
		else{ 		//if not put new client tab into state
			cli_tab.c_id = request.c;
			cli_tab.c_add=cli_add.getHostName();   
			cli_tab.c_port=cli_port;
			state.client_table.put(request.c, cli_tab);
		}
			
		if(request.s>cli_tab.recent){ //if the request has newer sequence number than the most recent 
				
			state.log.add(request); //add request to the log
			cli_tab.recent = request.s; //updates the sequence number in the client table
			state.client_table.put(request.c, cli_tab); // update the VR state
			try{
				sendPrepareToReplicas(request); //sendPrepareToReplicas caused by the request
			}
			catch ( IOException ioException ){
				displayMessage( ioException.toString() + "\n" );
				ioException.printStackTrace();
			}
			
			state.op_number++; //increment the op_number
			
		}
		else{
			//drop request, resent reply
		}
	} //end request processor
		
	//Prepare processor
	private void processPrepare(Prepare prep_msg){
		
		Request request = prep_msg.r;  // gets request from the prepare message
		ClientTab cli_tab = state.new ClientTab();			//initialize client table
		
		if(prep_msg.n==state.op_number){ //Checks if the op-number is correct
			
			
			state.op_number++; //increment op_number
						
			//update log
			state.log.add(request);
			
			
			//update client table
			if(state.client_table.containsKey(request.c)){ 		//checks if client table exists for this client
				cli_tab =  state.client_table.get(request.c);	//loads client table
			}
			else{ 		//if not put new client tab into state
				cli_tab.c_id = request.c;
				//cli_tab.c_add=cli_add.getHostName(); // does the replicas need the clients addresses and port?
				//cli_tab.c_port=cli_port;
				state.client_table.put(request.c, cli_tab);
			}
			
			state.log.add(request); //add request to the log
			cli_tab.recent = request.s; //updates the sequence number in the client table
			state.client_table.put(request.c, cli_tab); // update the VR state
			
			try{
				sendPrepareOkToPrimary(prep_msg); //sendPrepareToReplicas caused by the request
			}
			catch ( IOException ioException ){
				displayMessage( ioException.toString() + "\n" );
				ioException.printStackTrace();
			}
		}
		else{
				//wait for the previous Prepare messages
		}
	} // end prepare processor
		
		
	//PrepareOk processor
		
	private void processPrepareOk(PrepareOk prepOk_msg, DatagramPacket receivePacket){
		
		Vector<Integer> rep_counter;
		PrepareOk prepareOk = prepOk_msg;
		int nr_ok=0;
		
		if ((state.prepareOk_counter.isEmpty()) || (!state.prepareOk_counter.containsKey(prepareOk.n))){
			rep_counter = new Vector<Integer>();
			rep_counter.addElement(prepareOk.i);
			state.prepareOk_counter.put(prepareOk.n,rep_counter);
		}
		else {
			rep_counter=state.prepareOk_counter.get(prepareOk.n);
			rep_counter.addElement(prepareOk.i);
			state.prepareOk_counter.put(prepareOk.n,rep_counter);
		}
		
		for (int r_id=0;r_id<config.length;r_id++){			//Looks for the prepareOk msg from each replica 
			if (!(state.prepareOk_counter.get(prepareOk.n).lastIndexOf(r_id)==-1)){
				nr_ok++;
			}
		}
		
		//check if there are enough prepareOk's
		//if we just need one, send immediately and ignore the second
		displayMessage( "\nr_ok="+nr_ok+" commit="+ state.commit_number+" n="+prepareOk.n);
		if ((nr_ok+1>=min_quorum) && (state.commit_number<prepareOk.n)) {	
			try {
				state.commit_number=prepareOk.n;							//update commit number
				sendReplyToClient(prepareOk, getReply(prepareOk.n));		//sendReply
			} catch (IOException e) {
				e.printStackTrace();
			}
					
		}
	}	// end prepareOk processor
	
	
	
	//StartViewChange processor
	
	private void processStartViewChange(StartViewChange StartViewChange_msg){
			
		StartViewChange startViewChange = StartViewChange_msg;
			
		if (state.view_number==startViewChange.v) {
			try {
				sendDoViewChange();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
			
	}
	
		
	//DoViewChange processor
		
	private void processDoViewChange(DoViewChange doviewchange_msg){
			
		//Vector<Integer> rep_counter;
		Vector<DoViewChange> rep_counter;
		DoViewChange doViewchange = doviewchange_msg;
		int nr_ok=0;
		int mapvectorsize;
		
		if ((state.doViewChange_counter.isEmpty()) || (!state.doViewChange_counter.containsKey(doViewchange.v))){
			rep_counter = new Vector<DoViewChange>();
			rep_counter.addElement(doViewchange);
			state.doViewChange_counter.put(doViewchange.v,rep_counter);
		}
		else {
			rep_counter=state.doViewChange_counter.get(doViewchange.v);
			rep_counter.addElement(doViewchange);
			state.doViewChange_counter.put(doViewchange.v,rep_counter);
		}
		mapvectorsize=state.doViewChange_counter.get(doViewchange.v).size();	
		for (int r_id=0;r_id<config.length;r_id++){				//Looks for the doViewChange msg from each server
			for (int v_pos=0;v_pos<mapvectorsize;v_pos++){			
				if ((state.doViewChange_counter.get(doViewchange.v).elementAt(v_pos).i==r_id)){
					nr_ok++;
				}
			}
		}
		
		
//		if ((state.doViewChange_counter.isEmpty()) || (!state.doViewChange_counter.containsKey(doViewchange.v))){
//			rep_counter = new Vector<Integer>();
//			rep_counter.addElement(doViewchange.i);
//			state.doViewChange_counter.put(doViewchange.v,rep_counter);
//		}
//		else {
//			rep_counter=state.doViewChange_counter.get(doViewchange.v);
//			rep_counter.addElement(doViewchange.i);
//			state.doViewChange_counter.put(doViewchange.v,rep_counter);
//		}
//			
//		for (int r_id=0;r_id<config.length;r_id++){			//Looks for the doViewChange msg from each server 
//			if (!(state.doViewChange_counter.get(doViewchange.v).lastIndexOf(r_id)==-1)){
//				nr_ok++;
//			}
//		}
			
		if (nr_ok>=min_quorum) {					//check if there are enough doViewChange msg
			state.view_number=doViewchange.v;		//update view number
			try {
				sendStartView(bestDoViewChange(doViewchange.v));
			} catch (IOException e) {
				e.printStackTrace();
			}		//send ViewChange msg
						
		}
	}	// end ViewChange process
	
	
	//StartView processor
	
	private void processStartView(StartView StartView_msg){
		StartView startView = StartView_msg;
		
		state.view_number=startView.v;
		state.op_number=startView.n;
		state.commit_number=startView.k;
		state.log.clear();
		state.log=startView.l;
		
		state.status="normal";
		state.last_norm_view=startView.v;
		
		//update client table
		
		//*************************
		//confuse about what to do
		//*************************

		//*************************
		//look for uncommited msgs
		//*************************

		//k<last entry in received log???
		//how do i know that something in the log is not commited? 
		
	}
	
	//********************************************************************* 
	//							UTIL METHODS
	//*********************************************************************	
		
	// manipulates displayArea in the event-dispatch thread
	private void displayMessage( final String messageToDisplay ){
			SwingUtilities.invokeLater( new Runnable() {
				public void run() { 		// updates displayArea
					displayArea.append( messageToDisplay ); // display message
				} // end method run
			} // end anonymous inner class
			); // end call to SwingUtilities.invokeLater
	} // end method displayMessage
		
	//returns the request message of an op_number
	private Request getReply(int log_idx){
		
		return state.log.get(log_idx);
		
	}
	
	//executes commands given to the server
	private void executeCommand(String command) {
		switch (command){
			case "op": // display op-number
				displayArea.append( "\n Current op-number is:" + state.op_number +"\n" ); 
				break;
			case "log": //displays the log
				displayLog(); 
				break;
			case "client-tab":
				displayClientTable();
				break;
			case "info":
				displayMessage( "\nState:" + state.status +
						 		"\nView number: "+ state.view_number +
						 		"\nReplica number: "+ state.rep_number +
						 		"\nOp number: "+ state.op_number + "\n");
				break;
			default:
				displayArea.append("\nCommand not supported\n");
				break;
		}
		
		
	}
	
	//displays client table
	private void displayClientTable() {
		displayArea.append("\nCurrent client table:\n");
		for (ClientTab value : state.client_table.values()) {
			displayMessage( "\nClient:" + value.c_id +
			 		"\nRecen request: "+ value.recent +
			 		"\nCommited: "+ value.commited +
			 		"\nAddress&port: "+ value.c_add + ":" + value.c_port + "\n");
		}   
		
	}

	//method that displays current log
	private void displayLog() {
		
		displayArea.append( "\n Current log:\n" );
		
		Iterator it = state.log.iterator(); //initialize iterator for log list
		int i=0; //
		while (it.hasNext()) {
			
			
		    Request val = (Request) it.next(); //get next request from log
		    //display all information about the request
		    displayMessage( "\nIndex:" + i +
					 		"\nClient: "+ val.c +
					 		"\nSequence no: "+ val.s +
					 		"\nOperation: "+ val.op + "\n");
		    i++;
		}
		
	}
	
	//method that returns this machine IP Address
	
	protected String getIpAddress(){
	    
		InetAddress thisIp;
		String thisIpAddress=null;

	    try{
	    	thisIp = InetAddress.getLocalHost();
	    	thisIpAddress = thisIp.getHostAddress().toString();
	    }
	    catch(Exception e){}
		
	    return thisIpAddress;
	}
	
	
	//method that returns the DoViewChange message with best v'
	
	private DoViewChange bestDoViewChange(int my_view){
		
		int mapvectorsize;
		int best_pos=0;
		int best_last_v=0;
		int best_n=0;
		
		mapvectorsize=state.doViewChange_counter.get(my_view).size();
		
		best_last_v=state.doViewChange_counter.get(my_view).elementAt(0).last_v;
		best_n=state.doViewChange_counter.get(my_view).elementAt(0).n;
		
		for (int r_id=0;r_id<config.length;r_id++){			//Looks for the doViewChange msg from each server
			for (int v_pos=0;v_pos<mapvectorsize;v_pos++){			//Looks for the doViewChange msg from each server
				if ((state.doViewChange_counter.get(my_view).elementAt(v_pos).last_v>best_last_v)){
					best_pos=v_pos;
					best_last_v=state.doViewChange_counter.get(my_view).elementAt(v_pos).last_v;
					best_n=best_last_v=state.doViewChange_counter.get(my_view).elementAt(v_pos).n;
				}
				else if ((state.doViewChange_counter.get(my_view).elementAt(v_pos).last_v==best_last_v)){
					if ((state.doViewChange_counter.get(my_view).elementAt(v_pos).n>best_n)){
						best_pos=v_pos;
						best_last_v=state.doViewChange_counter.get(my_view).elementAt(v_pos).last_v;
						best_n=state.doViewChange_counter.get(my_view).elementAt(v_pos).n;
					}
				}
			}
		}
		
		return state.doViewChange_counter.get(my_view).elementAt(best_pos);
	}
	
	//********************************************************************* 
	//							END UTIL METHODS
	//*********************************************************************	
		
}

		

		
	 
	