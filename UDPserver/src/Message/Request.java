package Message;


public class Request extends Message {
    
    public int c; // client id
    public int s; // seq no
    public String op; // operation

    
    // returns 1 on error, 0 on success
    public int fromString(String[] data) {
        
        // check if there are enough elements to decode...
        if (data.length != 4) {
            
            return 1;
        }
        
        // ...if first is int
        try {
          c = Integer.parseInt(data[1]);
          
        } catch (NumberFormatException e) {
          
          return 1;
        }
        
        // ...if second is int
        try {
          s = Integer.parseInt(data[2]);
          
        } catch (NumberFormatException e) {
          
          return 1;
        }
        
        op = data[3];
        
        return 0;
    }
    
    public String toString() {
        
        
        return "REQUEST;" + c + ";" + s + ";" + op;
    }
}