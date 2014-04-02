import java.io.IOException;
import java.net.ServerSocket;

public class Server
{
	public static String key;
	
	public static void main(String[] args) throws IOException
	{
		//TODO: use a config
		int port = 8123;
		key = "INSERT YOUR KEY HERE";
        
		@SuppressWarnings("resource")
		ServerSocket servsock = new ServerSocket(port, 1100);
		System.out.println("Solder server started on port "+port);
		System.out.println("Current API key:");
		System.out.println(key);
		System.out.println();
		while(true)
		{
			ConnHandle c = new ConnHandle();
			c.socket = servsock.accept();
			new Thread(c).start();
		}
	}
}