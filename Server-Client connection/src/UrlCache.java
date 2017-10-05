
/**
 * UrlCache Class
 * 
 *
 */

import java.io.*;
import java.util.*;
import java.net.*;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;

public class UrlCache {
	//check
	HashMap<String, String> catalog = null;
	
    /**
     * Default constructor to initialize data structures used for caching/etc
	 * If the cache already exists then load it. If any errors then throw runtime exception.
	 *
     * @throws IOException if encounters any errors/exceptions
     */
	public UrlCache() throws IOException {		
		//System.out.println("user.dir = " + System.getProperty("user.dir")+"\\src");
		
		File file = new File( System.getProperty("user.dir") + "/src/catalog.ser");
		//System.out.println("Parent file " + file.getParentFile());
		try {
			// load catalog from saved file if it exists
			if(file.exists()) {
				FileInputStream fIn = new FileInputStream(file);
				ObjectInputStream oIn = new ObjectInputStream(fIn);
				catalog = (HashMap) oIn.readObject();
				
				//System.out.println("using old catalog");
				oIn.close();
				fIn.close();
			}
			// create new catalog
			else {
				catalog = new HashMap<String, String>();
				file.createNewFile();
				//System.out.println("made a new catalog");
			}
		}
		catch (Exception e) {
			throw new RuntimeException("Program Error");
		}
	}
	

	
    /**
     * Downloads the object specified by the parameter url if the local copy is out of date.
	 *
     * @param url	URL of the object to be downloaded. It is a fully qualified URL.
     * @throws IOException if encounters any errors/exceptions
     */
	public void getObject(String url) throws IOException {
		
		String[] tokens = url.split("/|:");// parse the url by / and : to get the separate tokens
		String hostName = tokens[0];
		String fileName = tokens[tokens.length -1];
		int portNum = getPortNumber(tokens[1]); // checks if 2nd token is a specific port number and sets it, else use port 80
		
		// Open a TCP connection to server
		try{
			Socket socket = new Socket (hostName, portNum);
			OutputStream outStream = socket.getOutputStream();
			InputStream inStream = socket.getInputStream();
			byte[] bytes = new byte[20*1024];
			
			
			// CONDITIONAL GET determines which request we will send, conditional or not
			String HTTPrequest = getRequest(url, tokens);
			//System.out.println("Request: " + HTTPrequest);
			bytes = HTTPrequest.getBytes("US-ASCII");
			
			//send user input message to server. Flush to ensure message is sent
			outStream.write(bytes);
			outStream.flush();
			
			// reading the reply from server
			String response = "";
			int i = 0;
			//System.out.println("Response");
			bytes = new byte[10*1024];
			while((i = inStream.read(bytes)) != -1) 
			{
				response += new String(bytes);
			}
			//System.out.println("Response: " + response);
			// Separating the header from the body
			String[] parts = response.split("\\r\\n\\r\\n", 2);
			String header = parts[0];//System.out.println("headerString: " + header);
			//String body = parts[1];System.out.println("BodyString: " + body);
			//System.out.print("header\n" + header);
			
					
			// Process the header
			boolean download = readHeader(header, url, catalog);
			
			// Download file if necessary
			if(download == true) {
				// read the body as bytes and save into a byte array
				try {
					// Make the necessary directories and files, then write the body into the file
					File file = new File(System.getProperty("user.dir") +"\\src\\" + makeFileName(tokens)); // make directory in src directory
					file.getParentFile().mkdirs();
					file.createNewFile();
					
					FileOutputStream fOut = new FileOutputStream(file);
					byte[] bodyBuff = new byte[10*1024];
					
					bodyBuff = parts[1].getBytes();  //store body as bytes again
					try {
						fOut.write(bodyBuff);
						fOut.flush();
					}catch (Exception e) {}
					
					fOut.close();
					
				} catch (Exception e)	{}
				
			inStream.close();
			outStream.close();
			socket.close();
	
			}
		
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
		
		String lastMod = catalog.get(url);
		//System.out.println("lastMOd string is: " + lastMod);
		SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy hh:mm:ss zzz");
		Date date = format.parse(lastMod, new ParsePosition(0));
		millis = date.getTime();
		
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
		}
		return request;
	}
	
	
	public String makeFileName(String[] tokens) {
		String filename  = tokens[0] + getPathname(tokens);
		return filename;
	}
	
	
	public String getRequest(String url, String[] tokens) {
		boolean saved = inCatalog(url);
		String get = "GET " + getPathname(tokens)+ " HTTP/1.1\r\n";
		String host = "Host: " + tokens[0] + "\r\n";
		String cond = "If-Modified-Since: " + catalog.get(url)+ "\r\n\r\n";
		String request;
		
		if (saved == true)
			request = get + host + cond;
		else
			request = get + host + "\r\n";
		return request;
	}
	
	
	// checks if catalog contains an entry of the request
	public boolean inCatalog(String url) {
		String value = null;
		value = catalog.get(url);
		if (value != null)
			return true;
		return false;
	}
	
/*
 * reads and processes the header - returns an int depending on action
 * 0 set saved = true
 * 1 set download = true
 * -1 means do not download;
 */
public boolean readHeader(String s, String url, HashMap<String, String> catalog) {
	String[] lines = s.split("\r\n", 8);
	int i = 0;
	boolean saved = false; 
	boolean download = false;
	
	while(i < lines.length) {
		//System.out.println("line: " + lines[i]);
		// If request is OK, then check if catalog contains file 
		if(lines[i].contains("200 OK")) 
		{
			if(inCatalog(url)) 
			{
				saved = true; //System.out.println("saved = " + saved);
			}
				
		}
		else if (s.contains("304 OK"))
			saved = true;
		
		// process the last-modified line - check catalog
		else if(lines[i].contains("Last-Modified"))
		{
			String[] temp= lines[i].split(" ", 2);
			String lastMod = temp[1];
			//System.out.println("lastMod: " + lastMod);
			// No entry exists, save file and download OR entry exist and was updated, update mod date and download
			if(saved == false || (saved == true && !(catalog.get(url).equals(lastMod))))
			{
				catalog.put(url, lastMod);
				saveCatalog();
				download = true;
			}
			// entry exist but no updates, do not download!
			else  
			{
				//System.out.println("Do not download " + url);
				break;
			}	
		}
		i++;
	}
	return download;
}
	
	
	
	public void saveCatalog() {
		String curDir = System.getProperty("user.dir");
		File file = new File(curDir + "/src/catalog.ser");
		try {
			FileOutputStream fOut = new FileOutputStream(file);
			ObjectOutputStream oOut = new ObjectOutputStream(fOut);
			oOut.writeObject(catalog);
			oOut.close();
			fOut.close();
		}catch(IOException e) {e.printStackTrace();}
	}
	
}