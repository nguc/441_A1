import java.io.*;
import java.net.*;
import java.util.*;

/**
 * A Simple Echo Server
 */

public class TCPServer {

	public static void main(String[] args) {

		String s;
		Scanner inputStream;
		PrintWriter outputStream;
		ServerSocket serverSocket;

		// DataOuput/InputStream get/sends data in binary form

		try {
			// Listen on port 8888
			serverSocket = new ServerSocket(80);

			Socket socket = serverSocket.accept(); // program stops if client doesn't send anything'
			// Connected to client
			outputStream = new PrintWriter(new DataOutputStream(
					socket.getOutputStream()));
			inputStream = new Scanner(new InputStreamReader(
					socket.getInputStream()));

			// Respond to messages from the client
			while (true) {
				s = inputStream.nextLine(); // keeps reading until it reads a \n - it will get rid of the \n
				System.out.println(s);

				// exit if message from client is "bye", else keep looping
				if (s.equalsIgnoreCase("bye")) {
					outputStream.println("bye");
					outputStream.flush();
					break;
				}

				outputStream.println(s); // sending string to an output stream (file) adds a \n to end of the line
				outputStream.flush(); // forces program to send string asap
			}

			// closing the streams
			inputStream.close();
			outputStream.close();
		}
		catch (Exception e) {
			e.printStackTrace(System.out);
			System.out.println("Error: " + e.getMessage());
		}
	}
}
