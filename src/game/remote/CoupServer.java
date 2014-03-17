package game.remote;

import game.Game;
import game.Player;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CoupServer {
	
	private static Map<String,Integer> gameNameToNumberAvailableSeats = new HashMap<String,Integer>();
	private static Map<String,List<String>> gameNameToPlayerNames = new HashMap<String,List<String>>();
	private static Map<String,List<PrintWriter>> gameNameToPrintWriters = new HashMap<String,List<PrintWriter>>();
	private static Map<String,List<BufferedReader>> gameNameToBufferedReaders = new HashMap<String,List<BufferedReader>>();
	
	private static ExecutorService gameThreads = Executors.newCachedThreadPool();
	
	public static void main(String[] args){
        Set<Integer> availablePortNumbers = new HashSet<Integer>();
        int firstPort = 4445;
        for(int i = 0; i < 10; i++){
        	availablePortNumbers.add(firstPort + i);
        }
        
		int initialConnectionPort = 4444;

		ServerSocket initialConnectionServerSocket = null;
		try{
			initialConnectionServerSocket = new ServerSocket(initialConnectionPort);
		}catch(IOException e){
			System.out.println("Server could not start up.");
			e.printStackTrace();
			System.exit(1);
		}
    	while(true){
    		System.out.println("Listening for a connecting player");
    		try {
    			handleConnectingPlayers(availablePortNumbers, initialConnectionServerSocket);
    		} catch (IOException e) {
    			System.out.println("Exception caught when trying to listen on port or listening for a connection");
    			System.out.println(e.getMessage());
    			System.out.println("Will wait for next connecting user");
    			//FIXME Need to clean up any connections started in try block
    		}
        	System.out.println("Got a player connected");
    	}


	}

	private static void handleConnectingPlayers(
			Set<Integer> availablePortNumbers,
			ServerSocket initialConnectionServerSocket) throws IOException {
		
    	Socket initialConnectionClientSocket = initialConnectionServerSocket.accept();
		
		Set<String> currentlyAvailableGames = gameNameToNumberAvailableSeats.keySet();
		PrintWriter initialConnectionPrintWriter = new PrintWriter(initialConnectionClientSocket.getOutputStream(), true);
		initialConnectionPrintWriter.println("GameChoices+++"+currentAvailableGamesInfo(currentlyAvailableGames));
		//TODO list current players and available seats as well? -- have players enter names sooner?
		//TODO assign specific ports sooner?
		
		BufferedReader initialConnectionPrintReader = new BufferedReader(
				new InputStreamReader(initialConnectionClientSocket.getInputStream()));
		final String gameName = initialConnectionPrintReader.readLine();
		if(currentlyAvailableGames.contains(gameName)){
			joinExistingGame(availablePortNumbers,
					initialConnectionPrintWriter, gameName);
			
			if(gameNameToNumberAvailableSeats.get(gameName) == 0){
				startGame(gameName); //TODO regain ports after games end
			}
			
			
		}else{ //Creating new game
			createNewGame(availablePortNumbers, initialConnectionPrintWriter,
					initialConnectionPrintReader, gameName);
		}
	}

	private static String currentAvailableGamesInfo(
			Set<String> currentlyAvailableGames) {
		StringBuilder builder = new StringBuilder();
		for(String game : currentlyAvailableGames){
			builder.append("+++").append(game).append(" already has players ").append(gameNameToPlayerNames.get(game)).append(" and needs ").append(gameNameToNumberAvailableSeats.get(game)).append(" more players.");
			 
		}
		return builder.toString();
	}

	private static void createNewGame(Set<Integer> availablePortNumbers,
			PrintWriter initialConnectionPrintWriter,
			BufferedReader initialConnectionPrintReader, final String gameName)
			throws IOException {
		initialConnectionPrintWriter.println("NewGame");
		int numPlayersForGame = Integer.parseInt(initialConnectionPrintReader.readLine());
		System.out.println("Creating game for " + numPlayersForGame + " players.");
		
		gameNameToNumberAvailableSeats.put(gameName, numPlayersForGame - 1);
		gameNameToPrintWriters.put(gameName, new ArrayList<PrintWriter>());
		gameNameToBufferedReaders.put(gameName, new ArrayList<BufferedReader>());
		gameNameToPlayerNames.put(gameName, new ArrayList<String>());
		
		System.out.println("Sending connection info to client");
		createClientSocket(availablePortNumbers, initialConnectionPrintWriter, gameName);

	}

	private static void startGame(final String gameName) {
		System.out.println("Starting game " + gameName);
		final List<PrintWriter> printWriters = gameNameToPrintWriters.get(gameName);
		final List<BufferedReader> bufferedReaders = gameNameToBufferedReaders.get(gameName);
		final List<String> playerNames = gameNameToPlayerNames.get(gameName);
		
		gameThreads.execute(new Runnable(){

			@Override
			public void run() {
				try {
					playGame(printWriters, bufferedReaders, playerNames);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
		});
		
		gameNameToBufferedReaders.remove(gameName);
		gameNameToNumberAvailableSeats.remove(gameName);
		gameNameToPrintWriters.remove(gameName);
	}

	private static void joinExistingGame(Set<Integer> availablePortNumbers,
			PrintWriter initialConnectionPrintWriter, final String gameName)
			throws IOException {
		gameNameToNumberAvailableSeats.put(gameName, gameNameToNumberAvailableSeats.get(gameName) - 1);
		
		//Assign port
		createClientSocket(availablePortNumbers, initialConnectionPrintWriter, gameName);
	}

	private static void createClientSocket(Set<Integer> availablePortNumbers,
			PrintWriter initialConnectionPrintWriter, String gameName) throws IOException {
		int availablePort = availablePortNumbers.iterator().next();
		availablePortNumbers.remove(availablePort);
		System.out.println("Telling player to connect on port " + availablePort);
		initialConnectionPrintWriter.println("PORT:"+availablePort);
		//Above tells client which port to use now
		ServerSocket serverSocket = new ServerSocket(availablePort); //TODO need to end if there's a failure
		Socket clientSocket = serverSocket.accept();
		System.out.println("Established connection with player at port " + availablePort);
		PrintWriter printWriter = new PrintWriter(clientSocket.getOutputStream(), true);
		gameNameToPrintWriters.get(gameName).add(printWriter);
		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
		gameNameToBufferedReaders.get(gameName).add(bufferedReader);
		printWriter.println("Player Name?");
		gameNameToPlayerNames.get(gameName).add(bufferedReader.readLine());
	}

	private static void playGame(List<PrintWriter> playerWriters,
			List<BufferedReader> playerInputs, List<String> playerNames)
			throws IOException {
		List<PrintWriter> originalPlayerWriters = new ArrayList<PrintWriter>(playerWriters);
		List<BufferedReader> originalPlayerInputs = new ArrayList<BufferedReader>(playerInputs);
		List<String> originalPlayerNames = new ArrayList<String>(playerNames);
		
		List<Player> players = new ArrayList<Player>();
		for(int i = 0; i < playerNames.size(); i++){
			final String playerName = playerNames.get(i);
			final PrintWriter writeToPlayer = playerWriters.get(i);
			final BufferedReader readFromPlayer = playerInputs.get(i);
			players.add(new RemotePlayer(playerName,writeToPlayer,readFromPlayer));
		}
		Game g = new Game(players);
		g.deal();
		GameControllerServerSide gameController = new GameControllerServerSide(g, playerWriters,playerInputs);
		
		int nextPlayer = gameController.advanceToNextPlayer();
		while(nextPlayer != -1){
			String playerAction = playerInputs.get(nextPlayer).readLine();
			gameController.attemptToPerformAction(nextPlayer,playerAction);
			nextPlayer = gameController.advanceToNextPlayer();
		}
		
		for(int i = 0; i < originalPlayerWriters.size(); i++){
			originalPlayerWriters.get(i).println(Commands.GAME_OVER + "+++" + GameControllerServerSide.gameHistory);
		}
		System.out.println("History: " + GameControllerServerSide.gameHistory);
		
		//After game is over check if everyone wants to play again:
		for(int i = 0; i < originalPlayerInputs.size(); i++){
			String playAgain = originalPlayerInputs.get(i).readLine();
			if(!playAgain.equalsIgnoreCase(Responses.RESTART.toString())){
				throw new RuntimeException("Player responded invalidly about wanting to play again: " + playAgain);
			}
		}
		
		for(int i = 0; i < originalPlayerWriters.size(); i++){
			originalPlayerWriters.get(i).println(Responses.READY.toString());//get everyone ready
		}
		//If everyone agrees, then play again!
		playGame(originalPlayerWriters, originalPlayerInputs, originalPlayerNames);
	}


}
