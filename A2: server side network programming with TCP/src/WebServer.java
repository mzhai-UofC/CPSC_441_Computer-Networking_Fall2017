
/**
 * WebServer Class
 *  Cpsc 441 Fall 2017 Assignment 2
 *  Muzhou, Zhai (10106810)  L02-T08
 */

import java.util.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetAddress;
import java.util.concurrent.*;
import java.io.File;
import java.io.IOException;

@SuppressWarnings("unused")
public class WebServer extends Thread {

    ServerSocket server;
    ExecutorService executor;
    int portNumber;
    boolean ifOnline;
    
    /**
     * Default constructor to initialize the web server
     * 
     * @param port 	The server port at which the web server listens > 1024
     * 
     */
    public WebServer(int port) {
    	if(!(port > 1024) || !(port < 65536)) {
    		System.out.println("Invalid port number, please enter the port number between 1024 and 65536");
    		System.exit(0);
    	}
    	else{
    		portNumber = port;
    	}
    	
    	try {
			server=new ServerSocket(port);	
			server.setSoTimeout(1000);
			executor=Executors.newFixedThreadPool(8);
			ifOnline=true;
		} catch (IOException e) {
			
		}
    }

	
    /**
     * The main loop of the web server
     *   Opens a server socket at the specified server port
     *   Remains in listening mode until shutdown signal
     * 
     */
    public void run() {
    	Socket socket=new Socket();
    	while (ifOnline) {		
			try {
				socket=server.accept();
			
				Worker worker=new Worker(socket);
				executor.execute(new Thread(worker));
			} catch (IOException e) {
				
			}
		}  	
    }  
    
    /**
     * Signals the server to shutdown.
     *
     */
    public void shutdown() {
    	try {	
    		executor.shutdown();
			ifOnline=false;
				executor.shutdownNow();
		} 
    	catch (Exception e) {
			executor.shutdownNow();
			ifOnline=false;
		}
    }
    
    
    /**
     * A simple driver.
     */
    public static void main(String[] args) {
    	int serverPort = 2225;
	
    	// parse command line args
    	if (args.length == 1) {
    		serverPort = Integer.parseInt(args[0]);
    	}
	
    	if (args.length >= 2) {
    		System.out.println("wrong number of arguments");
    		System.out.println("usage: WebServer <port>");
    		System.exit(0);
    	}
	
    	System.out.println("starting the server on port " + serverPort);
	
    	WebServer server = new WebServer(serverPort);
	
    	server.start();
    	System.out.println("server started. Type \"quit\" to stop");
    	System.out.println(".....................................");
	
    	Scanner keyboard = new Scanner(System.in);
    	while ( !keyboard.next().equals("quit") );
	
    	System.out.println();
    	System.out.println("shutting down the server...");
    	server.shutdown();
    	keyboard.close();
    	System.out.println("server stopped");
    }
}
