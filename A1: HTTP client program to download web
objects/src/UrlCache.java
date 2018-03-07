
/**
 * UrlCache Class
 * 
 *
 */
import java.io.IOException;
import java.io.*;
import java.net.*;
import java.util.*;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

public class UrlCache {
	
    /**
     * Default constructor to initialize data structures used for caching/etc
	 * If the cache already exists then load it. If any errors then throw runtime exception.
	 *
     * @throws IOException if encounters any errors/exceptions
     */
	
    String path = "cache/";  // a folder for the downloaded files  
    HashMap<String, String> catalog = new HashMap<String,String>();	//initializing a hash map data structure for catalog
    SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy hh:mm:ss zzz");
	public UrlCache() throws IOException {
		//check if there is the folder "cache" already exists
		//if the cache does not exist or is not a path, then make a direction        
      
      HashMap<String, String> catalog = new HashMap<String,String>();
		//try{
			// FileInputStream in = new FileInputStream(c);
		try {
			//create an object for stream input
			ObjectInputStream sReader=new ObjectInputStream(new FileInputStream("catalog"));
			catalog=(HashMap<String, String>) sReader.readObject();	//read object	
			sReader.close();	//end reading
		} 
		catch (FileNotFoundException e) {
			catalog=new HashMap<String, String>();
		} 
		catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	//}
	//catch (FileNotFoundException e) {
			//e.printStackTrace();
		//}
	
	}
    /**
     * Downloads the object specified by the parameter url if the local copy is out of date.
	 *
     * @param url	URL of the object to be downloaded. It is a fully qualified URL.
     * @throws IOException if encounters any errors/exceptions
     */
	public void getObject(String url) throws IOException {
		int portNumber;
		String hostName;
		InputStream inputStream;
		PrintWriter outputStream;
		String updateLine = "";
		String responseLine = "";
		String temp = "";
		String currentLine;
		String lastModifiedDate;
		//============================check port number, get host name & url path
		//if the url does not contains port number
		if (!url.contains(":")) {
			hostName=url.substring(0, url.indexOf("/"));
			portNumber=80;
		}
		//if the url contains port number 
		 else {
			//get the port number between ":" to  "/"
			String urlPortNumber = url.substring((url.indexOf(":")+1), url.indexOf("/"));
			portNumber=Integer.parseInt(urlPortNumber);	//convert the port number from string to integer
			
			hostName=url.substring(0, url.indexOf(":"));
		}
		//URL pathis the substring after the first "/" for both casses
		String urlPath=url.substring(url.indexOf("/"), url.length());	
		//===========================connection
		try {
			Socket socket = new Socket(hostName, portNumber);
				
			//create input stream
		    inputStream=socket.getInputStream();
			//create output stream
			outputStream = new PrintWriter(new DataOutputStream(socket.getOutputStream()));					
			//check if the file already exists, if yes false
			boolean ifNew=false;
			//========================check if file already exists by its url		
			//if the url exist, that means we have retrieved the data(file) once, directly get last modified  
			if (catalog.containsKey(url)) {
				lastModifiedDate=catalog.get(url);
			}
			//else new files retrieve needed, set the status to true, last modified set to ""
			else {
				lastModifiedDate="";
				//new file needed
				ifNew=true;
			}
			//=========================print the stream status and file information
			String getRequest = "GET "+urlPath+" HTTP/1.1\r\n" + "Host: "+hostName+":" +  portNumber + "\r\n" + "If-modified-since: " + lastModifiedDate + "\r\n\r\n";
			outputStream.print(getRequest);
			System.out.println();
			outputStream.flush();
			//===========================read/ check/ update
			byte[] readByte=new byte[128];	//new buffer		
			int lineNum=0;	//counter of lines
			int num=0;	//loop counter
			int lineLength=0; //count the length of a line
			//if new file download needed
			while(!catalog.containsKey(url)) {
				readByte[num]=(byte) inputStream.read();
				temp=new String(readByte);
				num++;
				//get the address
				if (temp.contains("\r\n")) {
					currentLine=temp.substring(0, temp.lastIndexOf("\n")+1);	
					readByte=new byte[256];
					num=0;
				//First Line
				if (lineNum==0){ 	
					responseLine=temp;
				} 
				else if (currentLine.equals("\r\n")) {
					break;
				}
				//contains modified
				else if (currentLine.contains("Last-Modified")) {
					updateLine=currentLine.substring(currentLine.indexOf(":")+2, currentLine.indexOf("\r"));
				}
				//count & convert
				else if (currentLine.contains("Content-Length"))  { 
					lineLength=Integer.parseInt(currentLine.substring(currentLine.indexOf(":")+2, currentLine.indexOf("\r")));	
				} 
				System.out.print(currentLine);
				lineNum++;
			}
		}
		//other problems the file could have 
		if (!catalog.containsKey(url) && !responseLine.contains("404")||responseLine.contains("200")) {
			catalog.put(url, updateLine);	
		} 
		
		//=======================get objects
		byte[] buffer=new byte[lineLength];
		File cacheFile=new File(path+hostName+urlPath);
		//Download the contents of url if it is a new file
		if (ifNew) {
			int counter2=0;	
			//read line(ensure all the data used)
			while (true) {
				int readed;
				if (lineLength==0) {
					break;
				} else if (lineLength < 1024) {
					readed=inputStream.read(buffer, counter2, lineLength);
				} else {
					readed=inputStream.read(buffer, counter2, 1024);
				}
				lineLength=lineLength -readed;
				counter2=counter2 + readed;
			}
			//get object by output stream
			cacheFile.getParentFile().mkdirs();
			FileOutputStream fos=new FileOutputStream(cacheFile);
			fos.write(buffer);
			fos.close();
		}
			else {	
				//Retrieve file
				FileInputStream fileReader=new FileInputStream(cacheFile);
				fileReader.close();
			}
		//======================end/close connection
			ObjectOutputStream obWriter=new ObjectOutputStream(new FileOutputStream("catalog"));
			obWriter.writeObject(catalog);
			obWriter.flush();
			obWriter.close();
			inputStream.close();
			outputStream.close();
			socket.close();
		} catch (Exception e) {
			System.out.println("Error:" + e.getMessage());
		}
			}

    /**
     * Returns the Last-Modified time associated with the object specified by the parameter url.
	 *
     * @param url 	URL of the object 
	 * @return the Last-Modified time in millisecond as in Date.getTime()
     */
	public long getLastModified(String url) {
		if (catalog.containsKey(url)) {
			//get last modified date and time
			String lastModifiedDate0=catalog.get(url);	
			Date date = format.parse(lastModifiedDate0, new ParsePosition(0));
			long millis = date.getTime();
			return millis;	
		} 
		else {
			throw new RuntimeException();
		}
	}
	

}
