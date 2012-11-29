	package Server;

import java.util.List;
import java.util.Map;
import java.util.Vector;

import Message.DoViewChange;
import Message.Request;


public class VRstate {
	
	//public String[][] config; 								//here addresses and ports (because we're testing on one computer)
	public int rep_number; 									//id of this replica
	public int view_number; 								// just view number
	public int last_norm_view; 								// just view number
	public String status; 									// status: normal/view change/recovering
	public int op_number; 									//number assigned to the most recent request
	public int commit_number; 								// op_number of last committed request
	public Map<Integer,ClientTab> client_table; 			//list of client table objects keys are clients ids
	public List<Request> log; 								//log of requests
	public Map<Integer,Vector<Integer>> prepareOk_counter;	//prepareOK counter
	//public Map<Integer,Vector<Integer>> doViewChange_counter;	//doviewchange counter
	public Map<Integer,Vector<DoViewChange>> doViewChange_counter;	//doviewchange counter
	//public List<Request> newlog;		//just for test
	
	public VRstate(int id) { //add here the config, view_number etc.
		//config = ?
		rep_number = id;
		op_number = 0; // op_number starts from zero
		commit_number = -1; //// commit_number starts from zero
		view_number = 0; //view starts from -1, zero will be the first commited
		status = "normal"; //normal status only in this phase of project
		last_norm_view=0;		
	}
	
	
	
		
	public class ClientTab {
		public int recent; //seq number of clients most recent request
		public int c_id; //id of client
		public String c_add; //client's address
		public boolean commited; // was this request committed?
		public int c_port; //port of client's address		//30-10-2012 - RO
		public ClientTab(){ //default constructor
			recent = 0;
			c_id = 0;
			c_add = "";								//31-10-2012 - RO
			c_port = 0;								//30-10-2012 - RO
			commited = false;
		}
	}
	
}