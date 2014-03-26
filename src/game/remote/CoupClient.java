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
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javafx.application.Application;

public class CoupClient {

	private static BufferedReader initialInput;
	private static String hostName = "coup.matthew-steele.com";
	
	public static void main(String[] args){
		if(args.length < 1){
			System.out.println("Signing on to server coup.matthew-steele.com. ");
			System.out.println("(To specify a different server, exit the client and run again with the server name as a command line argument.) ");
		}else{
			hostName  = args[0];
		}
		establishNewConnection(null);
	}

	public static void establishNewConnection(final String defaultName) {
		Socket initialConnectSocket = getSocket(hostName , 4444); //initial connection port number
		try {
			System.out.println("Waiting for further input from server");
			initialInput = new BufferedReader(
					new InputStreamReader(initialConnectSocket.getInputStream()));
			PrintWriter initialOutput = new PrintWriter(initialConnectSocket.getOutputStream(), true);
			String firstReadLine = initialInput.readLine();
			System.out.println("Received input from server");
			
			String[] gameOptionsInfo = firstReadLine.split("\\+\\+\\+\\+\\+\\+"); //First it sends a list of available games.
			String[] allGames = null;
			if(gameOptionsInfo.length > 1){
				allGames = gameOptionsInfo[1].split("\\+\\+\\+");
			}
			CoupApplicationClientSide.gameOptions = allGames;
			CoupApplicationClientSide.initialOutput = initialOutput;
			CoupApplicationClientSide.initialInput = initialInput;
			if(CoupApplicationClientSide.launched()){
				CoupApplicationClientSide.showGameSelectionScreen(defaultName);
			}else{
				Application.launch(CoupApplicationClientSide.class);
			}
		} catch (IOException e1) {
			throw new RuntimeException("Could not get connection port from server");
		}
	}
	
	public static void startUp(String gameName, String userName){
		int portNum = -1;
		try {
			String serverResponse = initialInput.readLine();
			portNum = Integer.parseInt(serverResponse.split(":")[1]);
		} catch (IOException e1) {
			throw new RuntimeException(
					"Could not get connection port from server");
		}
		Socket coupSocket = getSocket(hostName, portNum);
		try {
			PrintWriter out = new PrintWriter(coupSocket.getOutputStream(),true);
			BufferedReader in = new BufferedReader(new InputStreamReader(
					coupSocket.getInputStream()));

			in.readLine();
			out.println(userName);
			System.out.println("Joining game " + gameName + " with user name " + userName);
			System.out.println("Game will start when all players have joined.");

			CoupClient.startNewGame(out, in);
			CoupApplicationClientSide.startNewGame(gameName);
		} catch (IOException e) {
			System.out.println("ERROR!");
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

	public static void startNewGame(PrintWriter out, BufferedReader in)
			throws IOException {
		String[] playerData = in.readLine().split(":");
		int numberPlayers = (playerData.length-1)/3;
		Map<String,Player> allPlayers = new ConcurrentHashMap<String,Player>();
		int thisPlayerIndex = Integer.parseInt(playerData[playerData.length - 1]);
		for(int i = 0; i < numberPlayers; i++){
			String playerName = playerData[3*i];
			Card firstCard = new Card(CardType.valueOf(playerData[3*i+1]));
			Card secondCard = new Card(CardType.valueOf(playerData[3*i+2]));
			Player player = new Player(playerName);
			player.receive(firstCard);
			player.receive(secondCard);
			allPlayers.put(player.toString(),player);
			if(i == thisPlayerIndex){
				CoupApplicationClientSide.playerForUi = player;
			}
		}
		
		
		String[] buttonLabels = in.readLine().split("\\+\\+");
		
		CoupApplicationClientSide.allPlayers = allPlayers;
		CoupApplicationClientSide.buttonLabels = Arrays.asList(buttonLabels);
		CoupApplicationClientSide.out = out;
		CoupApplicationClientSide.in = in;
		
	}
}
