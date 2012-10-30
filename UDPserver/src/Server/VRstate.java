package Server;

import java.util.List;
import java.util.Map;

import Message.Request;


public class VRstate {
	public String[][] config; //here addresses and ports (because we're testing on one computer)
	public int rep_number; //id of this replica
	public int view_number; // just view number
	public String status; // status: normal/view change/recovering
	public int op_number; //number assigned to the most recent request
	public int commit_number; // op_number of last committed request
	public Map<Integer,ClientTab> client_table; //list of client table objects
	public List<Request> log; //log of requests
	public Map<Integer,Integer> prepareOk_counter; // <op_number of the request, okcount>
	
	public VRstate(int id) { //add here the config, view_number etc.
		//config = ?
		rep_number = id;
		op_number = 0; // op_number starts from zero
		commit_number = 0; //// commit_number starts from zero
		view_number = 0; //view starts from zero
		status = "normal"; //normal status only in this phase of project
				
	}
	
	public class ClientTab {
		public int recent; //seq number of clients most recent request
		public int c_id; //id of client
		public String c_add; //client's address
		public boolean commited; // was this request committed?
		public ClientTab(){ //default constructor
			recent = 0;
			c_id = 0;
			c_add = "localhost";
			commited = false;
		}
	}
	
}