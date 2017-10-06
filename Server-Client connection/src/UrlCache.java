
/**
 * Author: Chi Nguyen
 * ID#: 10032932
 * 
 * CPSC 441 Assignment 1
 * UrlCache Class
 * 
 * This program opens a TCP connection between a client and server then creates a request 
 * for an object from the server, the program examines the response from the server as well as
 * a catalog, if it previously existed, to determine if it should download the object
 * 
 */

import java.io.*;
import java.util.*;
import java.net.*;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;

public class UrlCache {
	
	HashMap<String, String> catalog = null;
	
	/**
	 * Default constructor to initialize data structures used for caching/etc
	 * If the cache already exists then load it. If any errors then throw runtime exception.
	 *
	 * @throws IOException if encounters any errors/exceptions
	 */
	public UrlCache() throws IOException 
	{		
		File file = new File( System.getProperty("user.dir") + "/src/catalog.ser");

		try 
		{
			
			if(file.exists()) // read data from exisitng catalog
			{
				FileInputStream fIn = new FileInputStream(file);
				ObjectInputStream oIn = new ObjectInputStream(fIn);
				
				catalog = (HashMap) oIn.readObject();
				oIn.close();
				fIn.close();
			}
			else // create new catalog
			{
				catalog = new HashMap<String, String>();
				file.createNewFile();
			}
		}catch (Exception e) { throw new RuntimeException("Program Error"); }
	}
	
	
    /**
     * Downloads the object specified by the parameter url if the local copy is out of date.
	 *
     * @param url	URL of the object to be downloaded. It is a fully qualified URL.
     * @throws IOException if encounters any errors/exceptions
     */
	public void getObject(String url) throws IOException 
	{
		// parse the url by / and : to get the separate tokens
		String[] tokens = url.split("/|:");
		String hostName = tokens[0];
		String fileName = tokens[tokens.length -1];
		
		// checks if 2nd token is a specific port number and sets it, else use port 80
		int portNum = getPortNumber(tokens[1]); 
		
		// Open a TCP connection to server
		try
		{
			Socket socket = new Socket (hostName, portNum);
			OutputStream outStream = socket.getOutputStream();
			InputStream inStream = socket.getInputStream();
			byte[] bytes = new byte[20*1024];
			
			// CONDITIONAL GET determines which request we will send, conditional or not
			String HTTPrequest = getRequest(url, tokens);
			bytes = HTTPrequest.getBytes("US-ASCII");
			
			//send user input message to server. Flush to ensure message is sent
			outStream.write(bytes);
			outStream.flush();
			
			// reading the reply from server
			String response = "";
			bytes = new byte[1000*1024];
			int fileSize = 0;
			int i = 0;
			
			while((i = inStream.read(bytes)) != -1) 
			{
				fileSize += i;
				response += new String(bytes);
			}
			
			// Separating the header from the body
			String[] parts = response.split("\\r\\n\\r\\n", 2);
			String header = parts[0];
			boolean download = true;
			int headerSize = 0;
			
			//  process the header and return the size of the body
			int bodySize = processHeader(header, url, catalog);
			
			// Determine if file needs to be downloaded or not
			if (bodySize != -1) 
				headerSize = fileSize - bodySize;
			else
				download = false;

			// Start the download process
			if(download == true) 
			{
				try 
				{
					// Make the necessary directories and files, then write the body into the file
					File file = new File(System.getProperty("user.dir") +"\\src\\" + makeFileName(tokens)); // make directory in src directory
					file.getParentFile().mkdirs();
					file.createNewFile();
					
					FileOutputStream fOut = new FileOutputStream(file);
					
					// read the body as bytes and save into a byte array
					for(int index = headerSize; index < fileSize; index++)
					{	
						fOut.write(bytes[index]);
						fOut.flush();
					}
					fOut.close();
				} catch (Exception e){}
			}
			
			inStream.close();
			outStream.close();
			socket.close();
			
		}catch (Exception e){ System.out.println("My Error: " + e.getMessage()); }
	}
	
    /**
     * Returns the Last-Modified time associated with the object specified by the parameter url.
	 *
     * @param url 	URL of the object 
	 * @return the Last-Modified time in millisecond as in Date.getTime()
     */
	public long getLastModified(String url) 
	{
		long millis = 0;
		String lastMod = catalog.get(url);
		SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy hh:mm:ss zzz");
		Date date = format.parse(lastMod, new ParsePosition(0));
		millis = date.getTime();
		
		return millis;
	}
	

	
	/**
	 * Returns a boolean value based on if the String given specifies a port number or not
	 * 
	 * @param token  a string, parsed from the given url string
	 * @return true if string is an int else, returns false
	 */
	public boolean hasPortNumber(String token) 
	{
		if (!Character.isDigit(token.charAt(0)))
			return false;
		else 
			return true;
	}
	
	
	/**
	* Returns the port number specified in the url, else return 80 as the default port number
	* 
	* @param token  a string from the parsed url
	* @return specified port number or default port number (80)
	*/
	public int getPortNumber(String token)
	{
		if (hasPortNumber(token))	// return the specified port number
			return Integer.parseInt(token);
		
		else						// return default port number 80 
			return 80;
	}
	
	
	/* 
	 * Returns the pathname of the object
	 * 
	 * @param tokens  a string array containing parts of a parsed url 
	 * @return a string that contains the pathname of the object
	 */
	public String getPathname(String[] tokens) 
	{
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
	
	
	/*
	 * returns a name used to create a file
	 * 
	 * @param tokens  a string array containing parts of a parsed url
	 * @return a string containing the hostname and pathname of the object
	 */
	public String makeFileName(String[] tokens) 
	{
		String filename  = tokens[0] + getPathname(tokens);
		return filename;
	}
	
	
	/*
	 * returns a string containing an HTTP request
	 * 
	 * @param url a string containing the url of an object
	 * @param tokens an array of strings 
	 * @return a string containing the GET, Host, and condition parts of the HTTP request
	 */
	public String getRequest(String url, String[] tokens) 
	{
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
	
	
	/*
	 *  Checks if the catalog contains an entry of the request object
	 *  
	 *  @param url a string containing the url of the object
	 *  @return a boolean that indicates if object entry exist in the catalog
	 */
	public boolean inCatalog(String url) 
	{
		String value = null;
		
		value = catalog.get(url);
		
		if (value != null)
			return true;
		return false;
	}
	
	
	/*
	 * Serializes the data in the Hashmap and writes it to a .ser file
	 */
	public void saveCatalog() 
	{
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
	
	
	/*
	 * reads and processes the header information
	 * 
	 * @param s a string containing all the header information
	 * @param url a string containing the url of an object
	 * @param catalog a hashmap that contains strings for url and last-modified 
	 * 
	 * @return a value corresponding to the size of the body or -1, which means do not download file
	 */
	public int processHeader(String s, String url, HashMap<String, String> catalog)
	{
		String[] lines = s.split("\r\n", 8);
		String[] temp;
		boolean saved = false;
		boolean download = false;
		int i = 0;
		int count = -1;
		
		while(i < lines.length) 
		{
			// If request is OK, then check if catalog contains file 
			if(lines[i].contains("200 OK")) 
			{
				if(inCatalog(url)) 		
					saved = true;
			}
			
			else if (s.contains("304 OK"))
				saved = true;
			
			else if(lines[i].contains("Last-Modified"))
			{
				temp= lines[i].split(" ", 2);
				String lastMod = temp[1];
				
				// No entry exists, save file and download OR entry exist and was updated, update mod date and download
				if(saved == false || (saved == true && !(catalog.get(url).equals(lastMod))))
				{
					catalog.put(url, lastMod);
					saveCatalog();
					download = true;
				}
				else  		// entry exist but no updates, do not download!
					break;
			}
			else if (lines[i].contains("Content-Length") && download == true) 
			{
				temp = lines[i].split(" ",2);
				count = Integer.parseInt(temp[1]);
			}
			i++;
		}
		return count;
	}
	
}