/*
 * CPSC_441_ASSIGNMENT4_FALL_2017_DEC_01
 * Muzhou,Zhai_10106810_L02_T08 
 * */
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import cpsc441.a4.shared.*;

/**
 * Router Class
 * 
 * This class implements the functionality of a router
 * when running the distance vector routing algorithm.
 * 
 * The operation of the router is as follows:
 * 1. send/receive HELLO message
 * 2. while (!QUIT)
 *      receive ROUTE messages
 *      update mincost/nexthop/etc
 * 3. Cleanup and return
 * 
 *      
 * @author 	Majid Ghaderi
 * @version	3.0
 *
 */
public class Router {
	//Initializing variables	
	int id;
	int uTime;
	int port;	
	int[] linkCost;
	int[][] minCost;
	int[] nextHop;
	int numRouters;
	String sName;
	Timer timer;
	TimeOutHandler timeoutHandler;
	ObjectOutputStream out;
	ObjectInputStream in;
	ScheduledExecutorService sExecService;
	Future<?> future;
	RtnTable routingTable;	
    /**
     * Constructor to initialize the rouer instance 
     * 
     * @param routerId			Unique ID of the router starting at 0
     * @param serverName		Name of the host running the network server
     * @param serverPort		TCP port number of the network server
     * @param updateInterval	Time interval for sending routing updates to neighboring routers (in milli-seconds)
     */
	public Router(int routerId, String serverName, int serverPort, int updateInterval) {
		// to be completed
			//Initializing Router Object
			id = routerId;
			uTime = updateInterval;
			sName = serverName;
			port = serverPort;
	}

    /**
     * starts the router 
     * 
     * @return The forwarding table of the router
     */
	public RtnTable start() {	
		try {	
			//Opening socket connection		
			Socket socket = new Socket(sName, port);
			//Opening streams
			out = new ObjectOutputStream(socket.getOutputStream());
			in = new ObjectInputStream(socket.getInputStream());
			//Indicating info	
			System.out.println("Connecting to server: " + sName);
			System.out.println("Port number: " + port);	
			System.out.println("Sending Hello packet.");
			//Sending HELLO to server, should be ID to SERVER with HELLO type
			DvrPacket hello = new DvrPacket(id, DvrPacket.SERVER, DvrPacket.HELLO);
			out.writeObject(hello);
			out.flush();
			//Now receive HelloContains link cost vector of router		
			System.out.println("Receiveing Hello Packet.");
			DvrPacket serverResponse = (DvrPacket) in.readObject();
			numRouters = serverResponse.mincost.length;
			initializeVectors(serverResponse);
			//====Next hop
			System.out.println("Next hop");
			for (int i = 0; i < nextHop.length; i++) {
			System.out.println("INDEX[" + i + "] : " + nextHop[i]);
			}	
			//====Timmer
			System.out.println("Starting timer...");		
			sExecService = Executors.newScheduledThreadPool(1);
			future = sExecService.scheduleAtFixedRate(new TimeOutHandler(this), (long)uTime, (long)uTime, TimeUnit.MILLISECONDS);
			DvrPacket receive;
			//=====if a "quit" is input, close connection, else start receive packet
			do{
				System.out.println("Received packet");
				receive = (DvrPacket) in.readObject();
				processDVR(receive);			
			}
			while(receive.type != DvrPacket.QUIT);		
			System.out.println("Received Quit Packet");
			System.out.println("Closing socket, cancelling timer");
			sExecService.shutdown();
			socket.close();
			}
		catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}	
		return new RtnTable(minCost[id], nextHop);
	}
	
	//========Method for processing timeout 
	synchronized void processTimeOut() {
		//System.out.println("-----Timeout-----");
		for(int i = 0; i < numRouters; i++) {		
			//dpnt send dvrpackets to self or non-neighbors
			if(linkCost[i] == DvrPacket.INFINITY || i == id)
				continue;
			//send to neighbors only
			DvrPacket sendPacket = new DvrPacket(id, i, DvrPacket.ROUTE, minCost[id]);				
			try {
				out.writeObject(sendPacket);
				out.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}			
	}
	
	//Method for processing DVR
	synchronized void processDVR(DvrPacket dvr) {		
		if(dvr.type == DvrPacket.QUIT)
			return;	
		int senderId = dvr.sourceid;		
		if(senderId == DvrPacket.SERVER) {
			initializeVectors(dvr);	
		}
		//if not changed, do not need to recompute distance
		else {	
			System.out.println("Received new mincost from Router" + senderId);
			minCost[senderId] = dvr.mincost;		
			boolean Changed = false;
			//Check if i is ITSELF 
			for(int i = 0; i < numRouters; i++) {		
				if(i == id) {
					continue;
				}
				//check if the min cost has changed
				if(minCost[id][i] > linkCost[senderId] + minCost[senderId][i]) {
					minCost[id][i] = linkCost[senderId] + minCost[senderId][i];
					Changed = true;
					nextHop[i] = senderId;
				}					
			}
			//when the min cose does not changed
			if(Changed) {	
				for(int i = 0; i < numRouters; i++) {
					//send packets to self or non-neighbors
					if(i == id || linkCost[i] == DvrPacket.INFINITY){
						System.out.println("Router " + id + " skipping send to non-neighbor " + i);
						continue;
					}
					//send to neighbors only
					DvrPacket sendPacket = new DvrPacket(id, i, DvrPacket.ROUTE, minCost[id]);
						//System.out.print("Received Server packet: ");
				/*for (int i : dvr.getMinCost()) {
					System.out.print(i + " ");
				}
				System.out.print("\n");*/
					try {
						out.writeObject(sendPacket);
						out.flush();
					} catch (IOException e) {
						e.printStackTrace();
					}		
				}
				System.out.println("local minCost vector changed, reset timmer");
				future.cancel(true);
				future = sExecService.scheduleAtFixedRate(new TimeOutHandler(this), (long)uTime, (long)uTime, TimeUnit.MILLISECONDS);
			}
		}
	}
	//====Method for computing min cost
	synchronized void initializeVectors(DvrPacket dvr) {
		System.out.println("Topology has been updated");	
		linkCost = new int[numRouters];
		linkCost = dvr.mincost;
		System.out.println("Link Cost");
		//print the link cost one by one
		for (int i = 0; i < linkCost.length; i++) {
			System.out.println("INDEX[" + i + "] : " + linkCost[i]);
		}	
		//get the min cost
		minCost = new int[numRouters][numRouters];
		minCost[id] = linkCost.clone();	
		System.out.println("Min cost received");
		//print the link cost
		for (int i = 0; i < linkCost.length; i++) {
			System.out.println("INDEX[ " + i + "] : " + linkCost[i]);
		}	
	
		nextHop = new int[numRouters];		
		//initializing next hop routers (will only be neighbors at this point)
		for(int i = 0 ; i < numRouters; i++) {
			if(minCost[id][i] != 999) {
				nextHop[i] = i; //if mincost[id][i] exists 
			}
			else {
				nextHop[i] = -1;
			}
		}
	}
			
    /**
     * A simple test driver
     * 
     */
	public static void main(String[] args) {
		// default parameters
		int routerId = 0;
		String serverName = "localhost";
		int serverPort = 2227;
		int updateInterval = 1000; //milli-seconds
		
		if (args.length == 4) {
			routerId = Integer.parseInt(args[0]);
			serverName = args[1];
			serverPort = Integer.parseInt(args[2]);
			updateInterval = Integer.parseInt(args[3]);
		} else {
			System.out.println("incorrect usage, try again.");
			System.exit(0);
		}
		
		System.out.println("Classpath");
		System.getProperty("java.classpath");
		
		// print the parameters
		System.out.printf("starting Router #%d with parameters:\n", routerId);
		System.out.printf("Relay server host name: %s\n", serverName);
		System.out.printf("Relay server port number: %d\n", serverPort);
		System.out.printf("Routing update intwerval: %d (milli-seconds)\n", updateInterval);
		
		// start the router
		// the start() method blocks until the router receives a QUIT message
		Router router = new Router(routerId, serverName, serverPort, updateInterval);
		RtnTable rtn = router.start();
		System.out.println("Router terminated normally");
		
		// print the computed routing table
		System.out.println();
		System.out.println("Routing Table at Router #" + routerId);
		System.out.print(rtn.toString());
	}



}
