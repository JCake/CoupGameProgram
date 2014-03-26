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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CoupServer {
	
	private static Map<String,Integer> gameNameToNumberAvailableSeats = new HashMap<String,Integer>();
	private static Map<String,List<String>> gameNameToPlayerNames = new HashMap<String,List<String>>();
	private static Map<String,List<PrintWriter>> gameNameToPrintWriters = new HashMap<String,List<PrintWriter>>();
	private static Map<String,List<BufferedReader>> gameNameToBufferedReaders = new HashMap<String,List<BufferedReader>>();
	private static Map<String,List<ServerSocket>> gameNameToSockets = new HashMap<String,List<ServerSocket>>();
	
	private static List<Integer> availablePortNumbers = new CopyOnWriteArrayList<Integer>();
	static{
	    int firstPort = 4445;
	    for(int i = 0; i < 12; i++){
	    	availablePortNumbers.add(firstPort + i);
	    }
	}
	
	private static ExecutorService gameThreads = Executors.newCachedThreadPool();
	
	public static void main(String[] args){
        
        
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
    		} catch (Exception e) {
    			System.out.println("Exception caught when trying to listen on port or listening for a connection");
    			e.printStackTrace();
    			System.out.println("Will wait for next connecting user");
    			//FIXME Need to clean up any connections started in try block
    		}
        	System.out.println("Got a player connected");
    	}


	}

	private static void handleConnectingPlayers(
			Collection<Integer> availablePortNumbers,
			ServerSocket initialConnectionServerSocket) throws Exception {
		
    	Socket initialConnectionClientSocket = initialConnectionServerSocket.accept();
		
		Set<String> currentlyAvailableGames = gameNameToNumberAvailableSeats.keySet();
		PrintWriter initialConnectionPrintWriter = new PrintWriter(initialConnectionClientSocket.getOutputStream(), true);
		initialConnectionPrintWriter.println("GameChoices+++"+currentAvailableGamesInfo(currentlyAvailableGames));
		
		BufferedReader initialConnectionPrintReader = new BufferedReader(
				new InputStreamReader(initialConnectionClientSocket.getInputStream()));
		String gameNameInfo = initialConnectionPrintReader.readLine();
		
		if(gameNameInfo.equals("ClearAllInactivePorts")){
			clearAllInactiveGames(availablePortNumbers);
			return;
		}
		
		if(currentlyAvailableGames.contains(gameNameInfo)){
			joinExistingGame(availablePortNumbers,
					initialConnectionPrintWriter, gameNameInfo);
			
			if(gameNameToNumberAvailableSeats.get(gameNameInfo) == 0){
				startGame(gameNameInfo);
			}
			
			
		}else{ //Creating new game
			//First check to see if this was explicitly declared as a "new game" and strip off the "NEW GAME" part as needed:
			String[] definerAndName = gameNameInfo.split(";;;;;");
			if(definerAndName.length > 1){ //Specified as a "NEW GAME" explicitly
				gameNameInfo = definerAndName[1]; //TODO in future, might need to check index 0 to see if its "NEW GAME" or not
				while(currentlyAvailableGames.contains(gameNameInfo)){
					gameNameInfo += "I"; //Adding onto end of name to distinguish the games
				}
				
			}
			createNewGame(availablePortNumbers, initialConnectionPrintWriter,
					initialConnectionPrintReader, gameNameInfo);
		}
	}

	private static void clearAllInactiveGames(
			Collection<Integer> availablePortNumbers) {
		for(List<ServerSocket> gameSockets : gameNameToSockets.values()){
			for(ServerSocket socket : gameSockets){
				int portOpeningUp = socket.getLocalPort();
				try {
					socket.close();
					availablePortNumbers.add(portOpeningUp);
				} catch (IOException e) {
					e.printStackTrace();
					System.err.println("Could not reclaim port " + portOpeningUp);
				}
			}
		}
		gameNameToNumberAvailableSeats = new HashMap<String,Integer>();
		gameNameToPlayerNames = new HashMap<String,List<String>>();
		gameNameToPrintWriters = new HashMap<String,List<PrintWriter>>();
		gameNameToBufferedReaders = new HashMap<String,List<BufferedReader>>();
		gameNameToSockets = new HashMap<String,List<ServerSocket>>();
	}

	private static String currentAvailableGamesInfo(
			Set<String> currentlyAvailableGames) {
		StringBuilder builder = new StringBuilder();
		for(String game : currentlyAvailableGames){
			builder.append("+++").append(game).append(";;;;;").append(" already has players ").append(gameNameToPlayerNames.get(game)).append(" and needs ").append(gameNameToNumberAvailableSeats.get(game)).append(" more players.");
			 
		}
		return builder.toString();
	}

	private static void createNewGame(Collection<Integer> availablePortNumbers,
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
		gameNameToSockets.put(gameName, new ArrayList<ServerSocket>());
		
		System.out.println("Sending connection info to client");
		ServerSocket socket = createSocket(availablePortNumbers, initialConnectionPrintWriter, gameName);
		gameNameToSockets.get(gameName).add(socket);

	}

	private static void startGame(final String gameName) {
		System.out.println("Starting game " + gameName);
		final List<PrintWriter> printWriters = gameNameToPrintWriters.get(gameName);
		final List<BufferedReader> bufferedReaders = gameNameToBufferedReaders.get(gameName);
		final List<String> playerNames = gameNameToPlayerNames.get(gameName);
		final List<ServerSocket> gameSockets = gameNameToSockets.get(gameName);
		
		gameThreads.execute(new Runnable(){

			@Override
			public void run() {
				try {
					playGame(printWriters, bufferedReaders, playerNames, gameSockets);
				} catch (IOException e) {
					e.printStackTrace();
					terminateGame(printWriters, gameSockets);
				}
			}
			
		});
		
		gameNameToBufferedReaders.remove(gameName);
		gameNameToNumberAvailableSeats.remove(gameName);
		gameNameToPrintWriters.remove(gameName);
		gameNameToSockets.remove(gameName);
	}

	private static void joinExistingGame(Collection<Integer> availablePortNumbers,
			PrintWriter initialConnectionPrintWriter, final String gameName)
			throws IOException {
		gameNameToNumberAvailableSeats.put(gameName, gameNameToNumberAvailableSeats.get(gameName) - 1);
		
		//Assign port
		ServerSocket serverSocket = createSocket(availablePortNumbers, initialConnectionPrintWriter, gameName);
		gameNameToSockets.get(gameName).add(serverSocket);
	}

	private static ServerSocket createSocket(Collection<Integer> availablePortNumbers,
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
		return serverSocket;
	}

	private static void playGame(List<PrintWriter> playerWriters,
			List<BufferedReader> playerInputs, List<String> playerNames,
			List<ServerSocket> gameSockets)
			throws IOException {
		Map<String,Integer> nameToCountOfName = new HashMap<String,Integer>();
		for(String name : playerNames){
			if(nameToCountOfName.containsKey(name)){
				if(nameToCountOfName.get(name) == 0){
					nameToCountOfName.put(name, 2); //Now there are two - name is duplicated
				}else{
					nameToCountOfName.put(name, nameToCountOfName.get(name) + 1);
				}
			}
			else{
				nameToCountOfName.put(name, 0); //zero means 1 but no duplicates
			}
		}
		
		List<String> indexedPlayerNames = new ArrayList<String>();
		for(int i = 0; i < playerNames.size(); i++){
			String indexedName = playerNames.get(i);
			int index = nameToCountOfName.get(playerNames.get(i));
			if(index > 0){
				indexedName += " " + index;
				nameToCountOfName.put(playerNames.get(i), nameToCountOfName.get(playerNames.get(i)) - 1);
			}
			indexedPlayerNames.add(indexedName );
		}
		playerNames = indexedPlayerNames;
		
		List<PrintWriter> originalPlayerWriters = new ArrayList<PrintWriter>(playerWriters);
		List<BufferedReader> originalPlayerInputs = new ArrayList<BufferedReader>(playerInputs);
		List<String> originalPlayerNames = new ArrayList<String>(playerNames);
		Map<String, BufferedReader> nameToReader = new HashMap<String, BufferedReader>();
		List<Player> players = new ArrayList<Player>();
		for(int i = 0; i < playerNames.size(); i++){
			final String playerName = playerNames.get(i);
			final PrintWriter writeToPlayer = playerWriters.get(i);
			final BufferedReader readFromPlayer = playerInputs.get(i);
			nameToReader.put(playerName, readFromPlayer);
			players.add(new RemotePlayer(playerName,writeToPlayer,readFromPlayer));
		}
		Game g = new Game(players);
		g.deal();
		GameControllerServerSide gameController = new GameControllerServerSide(g, playerWriters,playerInputs);
		
		Player nextPlayer = gameController.advanceToNextPlayer();
		while(nextPlayer != null){
			String playerAction = nameToReader.get(nextPlayer.toString()).readLine();
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
				System.out.println("Not all players want to play again.  Terminating and making players start a new game.");
				terminateGame(originalPlayerWriters, gameSockets);
				return; //Done playing this game
			}
		}
		
		for(int i = 0; i < originalPlayerWriters.size(); i++){
			originalPlayerWriters.get(i).println(Responses.READY.toString());//get everyone ready
		}
		//If everyone agrees, then play again!
		playGame(originalPlayerWriters, originalPlayerInputs, originalPlayerNames, gameSockets);
	}

	private static void terminateGame(List<PrintWriter> originalPlayerWriters, List<ServerSocket> socketsToClose) {
		for(int i = 0; i < originalPlayerWriters.size(); i++){
			originalPlayerWriters.get(i).println(Responses.NOT_READY.toString());
		}
		for(ServerSocket socket : socketsToClose){
			int portOpeningUp = socket.getLocalPort();
			try {
				socket.close();
				availablePortNumbers.add(portOpeningUp);
			} catch (IOException e) {
				e.printStackTrace();
				System.err.println("Could not reclaim port " + portOpeningUp);
			}
			
		}
	}


}
