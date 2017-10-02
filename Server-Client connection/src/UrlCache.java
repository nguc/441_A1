
/**
 * UrlCache Class
 * 
 *
 */

import java.io.*;
import java.util.*;
import java.net.Socket;

public class UrlCache {
	
	HashMap<String, String> catalog = new HashMap<String, String>();
	
    /**
     * Default constructor to initialize data structures used for caching/etc
	 * If the cache already exists then load it. If any errors then throw runtime exception.
	 *
     * @throws IOException if encounters any errors/exceptions
     */
	public UrlCache() throws IOException {
				
		
	}
	
    /**
     * Downloads the object specified by the parameter url if the local copy is out of date.
	 *
     * @param url	URL of the object to be downloaded. It is a fully qualified URL.
     * @throws IOException if encounters any errors/exceptions
     */
	public void getObject(String url) throws IOException {
		Scanner inputStream;
		PrintWriter outputStream;
		Scanner userInput;
		String request, reply;
		
		// parse the url by / and : to get the separate tokens
		String[] tokens = url.split("/|:");
		//for (int i =0; i < tokens.length; i++){System.out.println("tokens: " + tokens[i]);}
		
		// checks if 2nd token is a specific port number and sets it, else use port 80
		int portNum = getPortNumber(tokens[1]);
		System.out.println("Port number is " + portNum);
		
		// check if filename in catalog. if not then dl, else check last mod date
		
		
		// Open a TCP connection to server
		try
		{
			Socket socket = new Socket ("localhost", portNum);
			outputStream = new PrintWriter (new DataOutputStream(socket.getOutputStream()));
			inputStream = new Scanner(new InputStreamReader(socket.getInputStream()));
			userInput = new Scanner(System.in);
			
			//while (true) {
				System.out.println("Sending Request");
				//request = userInput.nextLine();
				request = getRequest(tokens);
				System.out.println("Request: " + request);
				System.out.println("checking catalog\n");
				System.out.println(checkCatalog(catalog, request));
				
				//send user input message to server. Flush to ensure message is sent
				outputStream.println(request);
				outputStream.flush();
				
				//Get reply from server
				reply = inputStream.nextLine();
				System.out.println(reply);
				
				// Exit if message from server is "bye"
				//if (reply.equalsIgnoreCase("bye"))
					//break;
			//}
			inputStream.close();
			outputStream.close();
		}
		catch (Exception e)
		{
			System.out.println("My Error: " + e.getMessage());
		}
	}
	
    /**
     * Returns the Last-Modified time associated with the object specified by the parameter url.
	 *
     * @param url 	URL of the object 
	 * @return the Last-Modified time in millisecond as in Date.getTime()
     */
	public long getLastModified(String url) {
		long millis = 0;
		
		return millis;
	}
	
	/**
	 * Returns a boolean value based on if the String given specifies a port number or not
	 * 
	 * @param token a String parsed from the given url string
	 * @return true if string is an int else, returns false
	 */
	public boolean hasPortNumber(String token) {
		// return default port number 80 
		if (!Character.isDigit(token.charAt(0)))
			return false;
		else 
			return true;
	}
	/**
	* Returns the port number specified in the url, else return 80 as the default port number
	* 
	* @param tokens  list of tokens from parsed url
	* @return specified port number or default port number (80)
	*/
	public int getPortNumber(String token) {
		// return the specified port number
		if (hasPortNumber(token))
			return Integer.parseInt(token);
		// return default port number 80 
		else
			return 80;
	}

	public String getRequest(String[] tokens) {
		String request = "";
		int i;
		
		if (hasPortNumber(tokens [1]))
			i = 2;
		else 
			i = 1;
		
		while (i < tokens.length)
		{
			request = request + "/" + tokens[i];
			i++;
		}
		return request;
	}
	
	// returns the catalog if one exists, else creates a new one
	public boolean getCatalog() {
		
	}
	// checks if catalog contains an entry of the request
	public boolean checkCatalog(HashMap catalog, String url) {
		if (catalog.get(url).equals(""))
			return false;
	
		return true;
	}
	
	
	
}
