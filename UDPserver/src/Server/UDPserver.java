package Server;


import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.awt.BorderLayout;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import Message.Message;
import Message.Prepare;
import Message.PrepareOk;
import Message.Request;
import Message.Util;
import Server.VRstate.ClientTab;


//group number: tfd013

public class UDPserver extends JFrame
	 {
	 private JTextArea displayArea; // displays packets received
	 private DatagramSocket socket; // socket to connect to client
	 //private String config[][]={{"localhost","1024"},{"localhost","1025"},{"localhost","1026"}};
	 private int[] ports = {1020,1021,1022};
	 private VRstate state;
	 
	 
	 	 
	 public UDPserver(int id)
	 {
		 
		 
		 super( "Replica"+id );
		 
		 int rep_id=id; //id of the replica  id=0 indicates the primary
		 state = new VRstate(id);
		 state.client_table = new HashMap<Integer,ClientTab>();
		 state.log = new ArrayList();
		 
		 
		 displayArea = new JTextArea(); // create displayArea
		 add( new JScrollPane( displayArea ), BorderLayout.CENTER );
		 setSize( 400, 300 ); // set size of window
		 setVisible( true ); // show window
		
		 try // create DatagramSocket for sending and receiving packets
		 {
			 socket = new DatagramSocket( ports[rep_id] );
		 } // end try
		 catch ( SocketException socketException )
		 {
			 socketException.printStackTrace();
			 System.exit( 1 );
		 } // end catch
		 
		 this.waitForPackets(); //starts listening
	 }
	 
		 

	 // end UDPserver constructor
		
	 //WAIT FOR PACKETS
		 // wait for packets to arrive, display data and echo packet to client
		 public void waitForPackets()
		 {
			 while ( true )
			 {
				 try // receive packet, display contents, return copy to client
				 {
					byte data[] = new byte[ 100 ]; // set up packet
					
					displayMessage("\n\n--------------------------------------------------\nwaiting for packet");
					DatagramPacket receivePacket =
					new DatagramPacket( data, data.length );
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
		//checking the type of the received messages and running appropriate Action
					if(test.getClass().getName().equals("Message.Request")){
						
						displayMessage("\nRequest received!");
						processRequest((Request) test);
						
						//sendPrepareToReplicas( receivePacket );
					}
					else if (test.getClass().getName().equals("Message.Prepare")){
						
						displayMessage("\nPrepare received!");
						processPrepare((Prepare) test);
						
												
					}
					else if (test.getClass().getName().equals("Message.PrepareOk")){
						
						//TODO processPrepareOK() method -- Ricardo
						//process prepareOK -- check numbers of prepareOK then invoke sendReplyToClient
						displayMessage("\nPrepareOK received!");
						
					}
					else{
						sendPacketToClient( receivePacket ); // send packet to client
				 		}
					} // end try
				 catch ( IOException ioException )
				 {
					 displayMessage( ioException.toString() + "\n" );
					 ioException.printStackTrace();
				 } // end catch
			} // end while
		}// end method waitForPackets
		
		 
		 
		 //SENDERS
		 
		// echo packet to client
		private void sendPacketToClient( DatagramPacket receivePacket )
			throws IOException
		{
			displayMessage( "\n\nEcho data to client..." );
			
			//create packet to send
			DatagramPacket sendPacket = new DatagramPacket(
					receivePacket.getData(), receivePacket.getLength(),
					receivePacket.getAddress(), receivePacket.getPort() );
			socket.send( sendPacket ); // send packet to client
			displayMessage( "Packet sent\n" );
		} // end method sendPacketToClient
		
		//Send PREPARE to replicas
		private void sendPrepareToReplicas (Request received_req) throws IOException {
			displayMessage("\n\nSending Prepare: request to all replicas.");
			//for loop should be here
			//create prepare message
			Prepare prepare_msg = new Prepare();
			prepare_msg.m = received_req.op; //take message from the client 
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
			//create packet to send
			
			//generate message
			PrepareOk prepareOk_msg = new PrepareOk();
			prepareOk_msg.i = state.rep_number; //id of replica
			prepareOk_msg.n = prep_msg.n;
			prepareOk_msg.v = state.view_number;
			
			String message = prepareOk_msg.toString();
			byte data[] = message.getBytes();
			
			DatagramPacket sendPacket1 = new DatagramPacket( 
					data, data.length,
					InetAddress.getByName("localhost"), ports[0] ); //should be taken from config
			
			socket.send( sendPacket1 ); // send packet to the primary

			displayMessage( "\nPrepareOK sent\n" );
		} //end prepareOk sender
		
		//Send REPLY to client
		private void sendReplyToClient (){
			//TODO reply sender --Ricardo
		}// end reply sender
		
		
		
		
		// manipulates displayArea in the event-dispatch thread
		private void displayMessage( final String messageToDisplay )
		{
			SwingUtilities.invokeLater(
				new Runnable()
				{
					public void run() // updates displayArea
					{
						displayArea.append( messageToDisplay ); // display message
					} // end method run
				} // end anonymous inner class
			); // end call to SwingUtilities.invokeLater
		} // end method displayMessage
		
		
		
		
		
		
		//MESSAGE PROCESSORS
		
		//request processor - processes request message and sends Prepare messages
		private void processRequest(Request req_msg){
			Request request = req_msg;
			ClientTab cli_tab = state.new ClientTab(); //initialize client table
			
			if(state.client_table.containsKey(request.c)){ //checks if client table exists for this client
				cli_tab =  state.client_table.get(request.c); //loads client table
			}
			else{ //if not put new client tab into state
				cli_tab.c_id = request.c;
				
				state.client_table.put(request.c, cli_tab);
				
			}
			
			if(request.s>cli_tab.recent){ //if the request has newer sequence number than the most recent 
				
				state.log.add(request); //add request to the log
				cli_tab.recent = request.s; //updates the sequence number in the client table
				state.client_table.put(request.c, cli_tab); // update the VR state
				try{
					sendPrepareToReplicas(request); //sendPrepareToReplicas caused by the request
				}
				catch ( IOException ioException )
				{
					 displayMessage( ioException.toString() + "\n" );
					 ioException.printStackTrace();
				}
				state.op_number++; //increment the op_number
			}
			else{
				//drop request, resent reply
			}
			
		} //end request processor
		
		//prepare processor
		private void processPrepare(Prepare prep_msg){
			if(prep_msg.n==state.op_number){ //cheks if the op-number is correct
				state.op_number++;
				//TODO
				//update log
				//update client-table
				try{
					sendPrepareOkToPrimary(prep_msg); //sendPrepareToReplicas caused by the request
				}
				catch ( IOException ioException )
				{
					 displayMessage( ioException.toString() + "\n" );
					 ioException.printStackTrace();
				}
			}
			else{
				//wait for the previous Prepare messages
			}
		} // end prepare processor
		
		
		//TODO processPrepareOk()
		
		private void processPrepareOk(PrepareOk prepOk_msg){
			//TODO processPrepareOk()
			//update prepareOk counter
			//check if there are enough prepareOk's
			//sendReply
			
			
		}

		
}

		

		
	 
		