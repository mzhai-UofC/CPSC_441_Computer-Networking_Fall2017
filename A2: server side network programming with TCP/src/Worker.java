/*
 * Worker class
 * Cpsc 441 Fall 2017 Assignment 2
 * Muzhou, Zhai (10106810)  L02-T08
 * Limitation: when running the test for multi-threads. the order of the output sentences sometimes changes
 * references:http://www.java2s.com/Code/Java/Network-Protocol/ASimpleWebServer.htm
 * 			  http://cs.au.dk/~amoeller/WWW/javaweb/server.htmlS
 */

import java.io.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.net.Socket;
import java.lang.StringBuilder;
import java.util.concurrent.TimeUnit;
import java.util.Date;

public class Worker implements Runnable{
	//Initializing variables
	 BufferedReader reader;	//input stream
     DataOutputStream writer;	//output stream
     Socket soc = new Socket();	//new socket 
     String[] str;		//a buffer for req
     boolean goodReq = false;
     //boolean stop=false;
     boolean GET = false;
     boolean reqLength = false;
     String req = "";	//a string for request
     String msg = "";	//output msg
     
    //==========constructor for worker thread class  
    public Worker(Socket in){
    	soc = in;
    	try {
    	//create input and output stream 
	    reader = new BufferedReader(new InputStreamReader(soc.getInputStream()));
	    writer = new DataOutputStream(soc.getOutputStream());
    	}
    	catch (IOException e) {
    		e.getMessage();
    	}
    }

   //=============to receive and send msg to HTTP GET
    public void run(){
    System.out.println("connection built");
	System.out.println("Worker working ...");
	//while (true) {
    	try{
    	//read input
	    req = reader.readLine();
	    //break the req by the empty space
	    str = req.split(" ");
	    //=============check if req is in a good format
	    if(str.length == 3) {reqLength = true;};
	    if(str[0].equals("GET")) { GET = true;}
	    //checking if the format is correct based on the "GET", object, and the protocol
	    if(!reqLength || !GET || !(str[2].equals("HTTP/1.1") || str[2].equals("HTTP/1.0"))){
	    //get the current time
		Date date = new Date();
		//output the msg response to the req if the format is correct
		msg = "HTTP/1.1 400 Bad Request\r\n"  + "Date: " + date + "\r\n" + "Server: " + "Server_A2" + "\r\n" + "Connection: close" + "\r\n" ;
		writer.writeBytes(msg);
		//ending input and output stream
		writer.flush();
		writer.close();
		reader.close();
	    }
	    //===========check if the file exist
	    else{
	    //read the second part of the input req
		StringBuilder strBuilder = new StringBuilder(str[1]);
		//remove "/" from the each input
		if(strBuilder.charAt(0) == '/')
			strBuilder.deleteCharAt(0);
		//convert to string
		String tstr = strBuilder.toString();
		Path path = Paths.get(tstr);	//get the path
		
		// check if the file exists, if it does not exist then output a "File not found" msg		
		if(!Files.exists(path)){
		    Date date = new Date(); //current time
		    //output the msg of file not found
		    msg = "HTTP/1.1 404 Bad File Not Found\r\n"  + "Date: " + date + "\r\n" + "Server: " + "Server_A2" + "\r\n" + "Connection: close\r\n" + "\r\n";		    
		    writer.writeBytes(msg);
		    //input stream and output stream ending
		    writer.flush();
		    writer.close();
		    reader.close();
		}
		//=============Otherwise 200 OK
		else{
			//get last modified time
		    FileTime fTime = Files.getLastModifiedTime(path);
		    long time = fTime.to(TimeUnit.MILLISECONDS);
		    //get the current time
		    Date date = new Date(time);
		    Date current = new Date();
		    String type = Files.probeContentType(path);
		    //Sending the bytes of the file
		    byte[] buffer2 = Files.readAllBytes(path);
		    int length = buffer2.length;
		    byte[] buffer;			
			long count2=0;
		    //output the 200 ok response 
		    msg = "HTTP/1.1 200 OK\r\n" +"Date: " + current + "\r\n" + "Server: " + "Server_A2" + "\r\n" + "Last-Modified: " + 
		    date + "\r\n" + "Content-Length: " + length + "\r\n" + "Content-Type: " + type + "\r\n" + "Connection: close\r\n" + "\r\n";
		    //sending the bytes of the file
		    byte[] buffer1 = msg.getBytes();
		    //a buffer for the parallel threads
		    byte[] outputBytes = new byte[buffer1.length + buffer2.length];
		    //output all the bytes in the parallel threads
		    buffer=new byte[32768];
			count2+=length;
		    System.arraycopy(buffer1, 0, outputBytes, 0, buffer1.length);
		    System.arraycopy(buffer2, 0, outputBytes, buffer1.length, buffer2.length);
		    		   		
		    //output the msg for the file
		    writer.write(outputBytes);
		    //input stream and output stream ending
		    writer.flush();
		    writer.close();
		    reader.close();			
        	}   
	    }
	    System.out.println(msg);
	    soc.close();
	}  
    // close the socket after everything finished 
	catch(Exception e){
	     e.getMessage();
	    try{
		soc.close();
	    }
	    catch(Exception f){
	    }
	}
    }
}

