package game.remote;

import game.Card;
import game.CardType;
import game.Player;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javafx.application.Application;

public class CoupClient {

	public static void main(String[] args){
		if(args.length < 1){
			System.out.println("Improper usage.  Must enter arg: [host IP]");
			System.exit(1);
		}
		
        BufferedReader stdIn =
                new BufferedReader(new InputStreamReader(System.in));
		
		String hostName = args[0];
		Socket initialConnectSocket = getSocket(hostName, 4444); //initial connection port number
		int portNum = -1;
		try {
			System.out.println("Waiting for further input from server");
			BufferedReader initialInput = new BufferedReader(
					new InputStreamReader(initialConnectSocket.getInputStream()));
			PrintWriter initialOutput = new PrintWriter(initialConnectSocket.getOutputStream(), true);
			String firstReadLine = initialInput.readLine();
			System.out.println("Received input from server");
			
			String[] gameOptionsInfo = firstReadLine.split("\\+\\+\\+"); //First it sends a list of available games.
			if(gameOptionsInfo.length > 1){
				System.out.println("Existing games: " + gameOptionsInfo[1]);
				System.out.println("Choose an existing game above to join, or enter the name of a new game you'd like to start:");
			}else{
				System.out.println("No games currently waiting on players.  Enter the name of a new game to start.");
			}
			String gameNameFromUser = stdIn.readLine();
			
			//TODO receive updates from server every so often?? - more games added?
			
			//TODO might assign specific port first?
			
			initialOutput.println(gameNameFromUser); //Sending game name back
			
			String serverResponse = initialInput.readLine();
			if(serverResponse.startsWith("NewGame")){ //User is starting a new game
				int numPlayers = -1;
				while(numPlayers < 2 || numPlayers > 6){
					System.out.println("Enter number of players to play this game (2-6):");
					String numberFromUser = stdIn.readLine();
					try{
						numPlayers = Integer.parseInt(numberFromUser);
					}catch(NumberFormatException nfe){
						System.out.println("Not a valid number.  Try again.");
					}
				}
				initialOutput.println(numPlayers);
				serverResponse = initialInput.readLine();
			}
			
			portNum = Integer.parseInt(serverResponse.split(":")[1]);
			System.out.println("Server says to connect on port " + portNum);
		} catch (IOException e1) {
			throw new RuntimeException("Could not get connection port from server");
		}
		System.out.println("Attempting to connect on port " + portNum);
		Socket coupSocket = getSocket(hostName, portNum);
        try {
        	PrintWriter out = new PrintWriter(coupSocket.getOutputStream(), true);
        	BufferedReader in = new BufferedReader(
        			new InputStreamReader(coupSocket.getInputStream()));


            System.out.println(in.readLine());
            
            String fromUser = stdIn.readLine();
            if (fromUser != null) {
                System.out.println("Your Response: " + fromUser);
                out.println(fromUser);
            }
            
            startNewGame(out, in);
            Application.launch(CoupApplicationClientSide.class);
       } catch (IOException e) {
        	e.printStackTrace();
        	System.exit(1);
      }
            
            
	}

	private static Socket getSocket(String hostName, int portNumber) {
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

	public static void startNewGame(PrintWriter out, BufferedReader in)
			throws IOException {
		String[] playerData = in.readLine().split(":");
		int numberPlayers = (playerData.length-1)/3;
		List<Player> allPlayers = new ArrayList<Player>();
		for(int i = 0; i < numberPlayers; i++){
			String playerName = playerData[3*i];
			Card firstCard = new Card(CardType.valueOf(playerData[3*i+1]));
			Card secondCard = new Card(CardType.valueOf(playerData[3*i+2]));
			Player player = new Player(playerName);
			player.receive(firstCard);
			player.receive(secondCard);
			allPlayers.add(player);
		}
		int thisPlayerIndex = Integer.parseInt(playerData[playerData.length - 1]);
		
		
		String[] buttonLabels = in.readLine().split("\\+\\+");
		
		CoupApplicationClientSide.playerForUi = allPlayers.get(thisPlayerIndex);
		CoupApplicationClientSide.allPlayers = allPlayers;
		CoupApplicationClientSide.buttonLabels = Arrays.asList(buttonLabels);
		CoupApplicationClientSide.out = out;
		CoupApplicationClientSide.in = in;
		
	}
}
