package Server;

// Fig. 24.12: ClientTest.java
// Tests the Client class.
import javax.swing.JFrame;

public class ClientTest
{
	public static void main( String args[] )
	{
		int clientId = 1;
		Client application1 = new Client( clientId ); // create client			
		application1.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
		application1.waitForPackets(); // run client application
		
		
	} // end main
} // end class ClientTest