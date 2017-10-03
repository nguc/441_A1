
/**
 * UrlCache Class
 * 
 *
 */

import java.io.*;
import java.util.*;
import java.net.*;

public class UrlCache {
	
	HashMap<String, String> catalog;
	
    /**
     * Default constructor to initialize data structures used for caching/etc
	 * If the cache already exists then load it. If any errors then throw runtime exception.
	 *
     * @throws IOException if encounters any errors/exceptions
     */
	public UrlCache() throws IOException {
		// initiallize catalog here!		
		
	}
	
    /**
     * Downloads the object specified by the parameter url if the local copy is out of date.
	 *
     * @param url	URL of the object to be downloaded. It is a fully qualified URL.
     * @throws IOException if encounters any errors/exceptions
     */
	public void getObject(String url) throws IOException {
		
		
		// parse the url by / and : to get the separate tokens
		String[] tokens = url.split("/|:");
		//for (int i =0; i < tokens.length; i++){System.out.println("tokens: " + tokens[i]);}
		String hostName = tokens[0];
		String fileName = tokens[tokens.length -1];
		// checks if 2nd token is a specific port number and sets it, else use port 80
		int portNum = getPortNumber(tokens[1]);
		//System.out.println("Port number is " + portNum);
		
		// check if filename in catalog. if not then dl, else check last mod date
		
		
		// Open a TCP connection to server
		try
		{
			Socket socket = new Socket (hostName, portNum);
			PrintWriter outputStream = new PrintWriter (socket.getOutputStream());
			BufferedReader inputStream = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			
			System.out.println("Sending Request");
			String HTTPrequest = getRequest(tokens);
			System.out.println("Request: " + HTTPrequest);
			
			//System.out.println("checking catalog\n");
			//System.out.println(checkCatalog(catalog, request));
			
			//send user input message to server. Flush to ensure message is sent
			outputStream.println(HTTPrequest);
			outputStream.flush();
			
			
			
			// reading the reply from server
			byte[] bytes = new byte[10*1024];
			int s = 0;
			int bytesRead = 0;
			String tmp = "";
			String lastMod = "";
			
			// read and process the header
			while((tmp=inputStream.readLine()) != null){
				//tmp=inputStream.readLine();
				System.out.println("header: " + tmp);
				if(tmp.isEmpty()) break;
				
				if(tmp.contains("200 OK")) {
					//check catalog
					System.out.println("checking cataloge");
					 //if (!inCatalog(fileName)) {
						 // save file name to catalog
					 //}
				}
				
				else if(tmp.contains("Last-Modified")){
					//check catalog
					lastMod = tmp;
					System.out.println("Updating last modified with: " + tmp);
				}

			}
			
			// read and save the body to a file
			System.out.println("body");
			do{
				
				s = inputStream.read();
				bytes[bytesRead] = (byte)s;
				tmp = new String(bytes);
				//System.out.println("body: " + tmp);
				bytesRead++;
				
				
			}while (s != -1);
			
			// close the input and output streams
			socket.close();
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

	/* 
	 * Returns the pathname of the object
	 */
	public String getPathname(String[] tokens) {
		String request = "";
		int i;
		
		if (hasPortNumber(tokens [1]))
			i = 2;
		else 
			i = 1;
		
		while (i < tokens.length)
		{
			request = request + "/" +tokens[i];
			i++;
			//if (i < tokens.length)
				//request = request + "/";
				
		}
		return request;
	}
	
	
	public String getRequest(String[] tokens) {
		return "GET " + getPathname(tokens)+ " HTTP/1.0\r\n\r\n";
	}
	
	// returns the catalog if one exists, else creates a new one
	//public boolean getCatalog() {
		
	//}
	// checks if catalog contains an entry of the request
	public boolean inCatalog(String filename) {
		// use the url cache method to read the catalog
		if (catalog.get(filename).equals(""))
			return false;
	
		return true;
	}
	
	public int readHeader(byte[] b, BufferedReader inputStream) throws IOException {
		int r;
		int i = 0;
		String s;
		
		while ((r = inputStream.read()) != -1) {
			b[i] = (byte)r;
			s = new String(b);
			System.out.println("s: " + s);
			if (s.equals("\r\n"))
				break;
			i++;
		}
		System.out.println("Done header. i = " + i);
		return i;
	}
	
}
