
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
	
	HashMap<String, String> catalog;
	
	/**
	 * Default constructor to initialize data structures used for caching/etc
	 * If the cache already exists then load it. If any errors then throw runtime exception.
	 *
	 * @throws IOException if encounters any errors/exceptions
	 */
	public UrlCache() throws IOException 
	{		
		try 
		{
			//File file = new File( System.getProperty("user.dir") + "/src/catalog.ser");
			
			FileInputStream fIn = new FileInputStream("catalog.ser");
			ObjectInputStream oIn = new ObjectInputStream(fIn);
			catalog = (HashMap<String, String>) oIn.readObject();
			oIn.close();
			fIn.close();
		
		}	
		catch (FileNotFoundException e)
		{
				catalog = new HashMap<String, String>();
		}
		catch (Exception e) {
			System.out.println("Cache Error: " + e.getMessage()); 
		}
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
			
			// add CONDITIONAL GET to request
			String HTTPrequest = getRequest(url, tokens);
			//System.out.println(HTTPrequest);
			bytes = HTTPrequest.getBytes("US-ASCII");
			
			//send user input message to server. Flush to ensure message is sent
			outStream.write(bytes);
			outStream.flush();
			
			// reading the reply from server	
			bytes = new byte[2*1024];
			String response = "";
			int offset = 0;
			boolean readRN = false;
			int r = 0;
			
			// reading stream 1 byte at a time until we find the sequence 13 10 13 10 which corresponds to \r\n\r\n
			while((r = inStream.read(bytes, offset, 1)) != -1) 
			{
				if(readRN && bytes[offset] == 10) 
				{
					response = new String(bytes, "UTF-8");
					break;
				}
				
				if(readRN && bytes[offset] != 13)
					readRN = false;
					
				if(bytes[offset] == 10)	// \n was read
					readRN = true;
				
				offset++;
			}
			// Split header into its separate lines
			String[] headerParts = response.split("\r\n");
			int contentLen = 0;
			String lastMod = "";
			
			// if file not modified then do not download
			if(headerParts[0].contains("304")) 
			{
				System.out.println("Don't download");
			}	
			
			if(headerParts[0].contains("200")) 
			{
				String[] temp;
				// process the rest of the header
				for(int i = 1; i < headerParts.length; i ++)
				{
					if(headerParts[i].contains("Last-Modified"))
					{
						temp= headerParts[i].split(" ", 2);
						lastMod = temp[1];
					}
					if(headerParts[i].contains("Content-Length"))
					{
						temp = headerParts[i].split(" ",2);
						contentLen = Integer.parseInt(temp[1]);
					}
				}
				
				// update last mod date in catalog and start downloading file
				if(catalog.get(url) == null || !(catalog.get(url).equals(lastMod)))
				{
					catalog.put(url, lastMod);
					
					// Make the necessary directories and files, then write the body into the file
					//File file = new File(System.getProperty("user.dir") +"\\src\\" + makeFileName(tokens)); // make directory in src directory
					File file = new File(tokens[0] + getPathname(tokens));
					file.getParentFile().mkdirs();
					file.createNewFile();
					FileOutputStream fOut = new FileOutputStream(file);
					
					int count = 0;
					int bytesRead = 0;
					bytes = new byte[1024];
					
					// keep reading the stream until the whole body is read
					while(bytesRead != -1)
					{	
						if(count >= contentLen)
							break;
						try 
						{
							bytesRead = inStream.read(bytes);
							fOut.write(bytes);
							fOut.flush();
							fOut.getFD().sync();
							//System.out.println("bytesRead = " + bytesRead);
							count += bytesRead;
						}
						catch (Exception e)
						{ 
							System.out.println("file write error: " + e.getMessage()); 
						}
						
					}
					//System.out.println("count = " + count);
					fOut.close();
					
				}
			}
			saveCatalog();
			inStream.close();
			outStream.close();
			socket.close();
		}
		catch (Exception e)
		{ 
			System.out.println("getObject error: " + e.getMessage()); 
		}
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
		if (catalog.get(url) != null)
			return true;
		
		return false;
	}
	
	
	/*
	 * Serializes the data in the Hashmap and writes it to a .ser file
	 */
	public void saveCatalog() 
	{
		//String curDir = System.getProperty("user.dir");
		//File file = new File(curDir + "/src/catalog.ser");
		
		try {
			FileOutputStream fOut = new FileOutputStream("catalog.ser");
			ObjectOutputStream oOut = new ObjectOutputStream(fOut);
			oOut.writeObject(catalog);
			oOut.close();
			fOut.close();
		}catch(IOException e) {e.printStackTrace();}
	}
	
	
}