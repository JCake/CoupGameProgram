package game.remote;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

public class AdminClient {

	private static final String hostName = "coup.matthew-steele.com";

	public static void main(String[] args){
		Socket initialConnectSocket = getSocket(hostName , 4444); //initial connection port number
		try {
			PrintWriter initialOutput = new PrintWriter(initialConnectSocket.getOutputStream(), true);
			initialOutput.println("ClearAllInactivePorts");
			System.out.println("Told server to clear all inactive ports.");
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public static Socket getSocket(String hostName, int portNumber) {
		Socket coupSocket = null;
        try {
        	coupSocket = new Socket(hostName, portNumber);
        } catch (UnknownHostException e) {
        	System.err.println("Don't know about host " + hostName +". Trying again in two seconds.");
        	waitTwoSeconds();
        	return getSocket(hostName,portNumber);
        } catch (IOException e) {
        	System.err.println("Couldn't get I/O for the connection to " +
        			hostName + ".  Trying again in two seconds.");
        	waitTwoSeconds();
        	return getSocket(hostName,portNumber);
        }
        System.out.println("Got connection to server!");
		return coupSocket;
	}

	private static void waitTwoSeconds() {
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e1) {
			System.out.println("Interrupted");
			System.exit(1);
		}
	}
}
