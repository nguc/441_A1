import java.io.*;
import java.net.*;
import java.util.*;

/**
 * A Simple Client
 */

public class TCPClient {

	public static void main(String[] args) {

		String s, tmp;
		Scanner inputStream;
		PrintWriter outputStream;
		Scanner userinput;

		try {
			// connects to port server app listesing at port 8888 in the same
			// machine
			Socket socket = new Socket("localhost", 8888);
			// need to use port number > 1024. lower numbers reserved by OS
			// localhost- the machine the applications is running on. Usually use when want apps on same machine to talk to each other. 


			// Create necessary streams
			outputStream = new PrintWriter(new DataOutputStream(
					socket.getOutputStream()));
			inputStream = new Scanner(new InputStreamReader(
					socket.getInputStream()));
			userinput = new Scanner(System.in);

			// send/receive messages to/from server
			// wait while user types in data
			while (true) {
				System.out.println("Enter Text Message for Echo Server: ");
				tmp = userinput.nextLine();

				// Send user input message to server
				outputStream.println(tmp);
				// Flush to make sure message is send
				outputStream.flush();

				// get reply from the server
				s = inputStream.nextLine();
				System.out.println(s);

				// Exit if message from server is "bye"
				if (s.equalsIgnoreCase("bye"))
					break;

			}
			inputStream.close();
			outputStream.close();
		}
		catch (Exception e) {
			System.out.println("Error: " + e.getMessage());
		}
	}
}
