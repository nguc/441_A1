
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
		System.out.println("user.dir = " + System.getProperty("user.dir")+"\\src");
		
		File file = new File( System.getProperty("user.dir") + "/src/catalog.ser");
		System.out.println("Parent file " + file.getParentFile());
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
				System.out.println("made a new catalog");
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
		// checks if 2nd token is a specific port number and sets it, else use port 80
		int portNum = getPortNumber(tokens[1]);
		
		// Open a TCP connection to server
		try
		{
			boolean saved = false;
			boolean download = false;
			
			// CONDITIONAL GET determines which request we will send, conditional or not
			//if(inCatalog(url)) 
				//saved = true; System.out.println("saved = " + saved);
				
				
			Socket socket = new Socket (hostName, portNum);
			PrintWriter outputStream = new PrintWriter (socket.getOutputStream());
			BufferedReader inputStream = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			
			String HTTPrequest = getRequest(url, tokens, saved);
			System.out.println("Request: " + HTTPrequest);
			
			//send user input message to server. Flush to ensure message is sent
			outputStream.println(HTTPrequest);
			outputStream.flush();
				
			// reading the reply from server
			
			
			String tmp = "";
			String lastMod = "";
			
			
			// read and process the header
			//if (saved == false) {
				while((tmp=inputStream.readLine()) != null){
					System.out.println(tmp);
					if(tmp.isEmpty()) 
						break;
					
					// If request is OK, then check if catalog contains file 
					if(tmp.contains("200 OK")) {
						//System.out.println("checking cataloge");
						if(inCatalog(url)) 
							saved = true; //System.out.println("saved = " + saved);
					}
					
					// process the last-modified line
					if(tmp.contains("Last-Modified")){
						//check catalog
						String[] temp= tmp.split(" ", 2);
						lastMod = temp[1];
						System.out.println("lastmod = " + lastMod);
						// No entry exists, save file and download OR entry exist and was updated, update mod date and download
						if(saved == false || (saved == true && !(catalog.get(url).equals(lastMod)))){
							System.out.println("Adding new entry " + url + " " + lastMod);
							catalog.put(url, lastMod);
							download = true;
						}
						// entry exist but no updates, do not download!
						else {
							System.out.println("Do not download " + url);
							break;
						}		
					}	
				}
			//}
			
			// read and save the body to a file
			if(download == true) {
				byte[] bytes = new byte[(int) Math.pow(2, 16)];
				int s = 0;
				int bytesRead = 0;
				System.out.println("body");
				// read the body as bytes and save into a byte array
				do{
					s = inputStream.read();
					bytes[bytesRead] = (byte)s;			
					bytesRead++;

				}while (s != -1);
				
				try {
					// Make the necessary directories and files, then write the body into the file
					File file = new File(System.getProperty("user.dir") +"\\src\\" + makeFileName(tokens)); // make directory in src directory
					file.getParentFile().mkdirs();
					file.createNewFile();
					
					FileOutputStream fOut = new FileOutputStream(file);
					fOut.write(bytes);
					fOut.close();
					
				} catch (Exception e)	{
					
				}
			}
			
			//Save catalog to file and close the input and output streams
			saveCatalog();
			inputStream.close();
			outputStream.close();
			socket.close();
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
			//if (i < tokens.length)
				//request = request + "/";
				
		}
		return request;
	}
	
	public String makeFileName(String[] tokens) {
		String filename  = tokens[0] + getPathname(tokens);
		System.out.println("file name: " + filename);
		return filename;
	}
	
	
	public String getRequest(String url, String[] tokens, boolean saved) {
		String get = "GET " + getPathname(tokens)+ " HTTP/1.0\r\n";
		String host = "Host: " + tokens[0] + "\r\n";
		String cond = "If-Modified-Since: " + catalog.get(url);
		String request;
		
		if (saved == true)
			request = get + host + cond;
		else
			request = get;
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
