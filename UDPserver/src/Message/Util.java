package Message;


public class Util {

	 // returns object
	 public static Message fromString(String message) {
        
        String[] data = message.split(";");
        
        Message obj;
        
        if (data[0].equals("REQUEST")) {
            
            obj = new Request();
            
        } else if (data[0].equals("PREPARE")) {
            
            obj = new Prepare();
        } else if (data[0].equals("PREPAREOK")) {
    
            obj = new PrepareOk();
        } else if (data[0].equals("REPLY")) {
            
            obj = new Reply();
        } else {
            
            obj = new Other();
        }
        
        if (obj.fromString(data) != 0) {
            
            obj = new Other();
        }
        
        return obj;
	 }
}