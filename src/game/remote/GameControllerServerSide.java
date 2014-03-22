package game.remote;

import game.CardType;
import game.Game;
import game.Player;
import game.actions.Action;
import game.actions.ActionList;
import game.actions.Defense;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

//TODO Ideas for expansion/improvement:
//View most recent action in common UI
//FIXME Need to get "YOU LOSE" to display again
public class GameControllerServerSide {
	
	private final Game game;
    private final List<Player> players; //Current list of players - non-eliminated -- use to send/receive from non-eliminated players
    
    //FIXME will be a problem if multiple have same name -- need a way to fix that! Could we just map from player??
    //Or maybe even have a DTO that wraps a player, PrintWriter, and BufferedReader
    private final Map<String, PrintWriter> nameToOutWriter = new HashMap<String, PrintWriter>();
    private final HashMap<String, BufferedReader> nameToPlayerInput = new HashMap<String, BufferedReader>();
    
    private Player nextPlayer = null;
    public static String gameHistory = "";
    
	
	Map<String, Map<String,Action>> playerActionMaps = new HashMap<String, Map<String,Action>>();

	public GameControllerServerSide(Game g, List<PrintWriter> outWriters, List<BufferedReader> playerInputs) {
		gameHistory = "";
		this.game = g;
		players = g.getPlayers();
		String allPlayerReps = "";
		for(int i = 0; i < g.getPlayers().size(); i++){
			RemotePlayer player = (RemotePlayer) g.getPlayers().get(i);
			player.setGameController(this);
			allPlayerReps += (player+":"+player.getFirstCard()+":"+player.getSecondCard()) + ":";
		}
		for(int i = 0; i < g.getPlayers().size(); i++){
			PrintWriter writer = outWriters.get(i);
			writer.println(allPlayerReps + i);
		}
		
		for(int i = 0; i < g.getPlayers().size(); i++){
			
			Player player = g.getPlayer(i);
			
			nameToOutWriter.put(player.toString(), outWriters.get(i));
			nameToPlayerInput.put(player.toString(), playerInputs.get(i));
			
			RemoteCardChooser remoteCardChooser = new RemoteCardChooser(nameToOutWriter,nameToPlayerInput);
			ActionList playerActions = new ActionList(g,player,remoteCardChooser);
			Map<String,Action> actionStringToAction = new HashMap<String,Action>();
			String allActionStrings = "";
			for(Action action : playerActions.getAllActions()){
				String actionString = action.actionDescription();
				actionStringToAction.put(actionString, action);
				allActionStrings += actionString + "++";
			}
			allActionStrings = allActionStrings.substring(0,allActionStrings.length()-2);
			playerActionMaps.put(player.toString(),actionStringToAction);
			outWriters.get(i).println(allActionStrings);
		}
	}
	
	public void attemptToPerformAction(Player actingPlayer, String actionStringKey){
		Action action = playerActionMaps.get(actingPlayer.toString()).get(actionStringKey);
		
		gameHistory += actingPlayer + " attempted " + actionString(action) + ":::";
		
		CardType cardTypeRequired = action.cardTypeRequired();
		if(cardTypeRequired != null){
			for(Player nonEliminatedPlayer : players){
				if(!nonEliminatedPlayer.equals(actingPlayer)){
					System.out.println("Giving player " + nonEliminatedPlayer.toString() + " the option to call bluff");
					nameToOutWriter.get(nonEliminatedPlayer.toString()).println(Commands.CallBluff.toString() + "+++" + actingPlayer + ":" + cardTypeRequired
							+ ":" + singleTargetedPlayer(action));
					try {
						String response = nameToPlayerInput.get(nonEliminatedPlayer.toString()).readLine();
						Player bluffCaller = nonEliminatedPlayer;
						if(response.equals(Responses.AccuseOfBluff.toString())){
							if(actingPlayer.has(cardTypeRequired)){
								//bluff caller is wrong
								gameHistory += bluffCaller + " incorrectly accused " + actingPlayer + " of bluffing.:::";
								bluffCaller.revealACard("Sorry, you were wrong.  " + actingPlayer + " did have " + cardTypeRequired);
								//TODO need to reveal two if bluff caller is target of assassin?
								game.reshuffleCardAndDrawNewCard(actingPlayer, cardTypeRequired);
								updatePlayerCards();
								checkForBlockingAndThenPerformAction(actingPlayer, action);
							}else{
								if(cardTypeRequired.equals(CardType.assassin)){
									actingPlayer.takeActionAssassin();  //TODO still pay if attempting assassination??
								}
								actingPlayer.revealACard(bluffCaller + " called your bluff about having " + cardTypeRequired);
								gameHistory += bluffCaller + " correctly accussed " + actingPlayer + " of bluffing, thus ending the turn.::::::";
							}
							return;
						}
					} catch (IOException e) {
						throw new RuntimeException("Could not get player input",e);
					}
				}
			}
		}
		
		checkForBlockingAndThenPerformAction(actingPlayer, action);
	}

	private String singleTargetedPlayer(Action action) {
		List<Player> targetedPlayers = action.targetedPlayers();
		if(targetedPlayers != null && targetedPlayers.size() == 1){
			return targetedPlayers.get(0).toString();
		}
		return "";
	}

	private void checkForBlockingAndThenPerformAction(Player actingPlayer, Action action) {
		if(game.getWinner() != null){
			return; //single winner already, no need to go one
		}
		
		List<Player> targetedPlayers = action.targetedPlayers();
		if(targetedPlayers != null){
			targetedPlayers = new ArrayList<Player>(targetedPlayers); //Making sure we have a list we can edit
			List<Player> alreadyEliminatedPlayers = new ArrayList<Player>();
			for(Player targetedPlayer : targetedPlayers){
				if(targetedPlayer.eliminated()){
					alreadyEliminatedPlayers.add(targetedPlayer);
				}
			}
			targetedPlayers.removeAll(alreadyEliminatedPlayers);
		}
		List<Defense> defensesThatCanBlock = action.defensesThatCanBlock();
		if(targetedPlayers != null && !targetedPlayers.isEmpty() && 
				defensesThatCanBlock != null && !defensesThatCanBlock.isEmpty()){
			Map<String,Defense> defenseStrToDefense = new HashMap<String,Defense>();
			String defensesThatCanBlockString = "";
			for(Defense defense : defensesThatCanBlock){
				String[] defensePackageStruct = defense.getClass().getName().split("\\.");
				String defenseDescript = defensePackageStruct[defensePackageStruct.length - 1];
				defensesThatCanBlockString += defenseDescript + ":";
				defenseStrToDefense.put(defenseDescript, defense);
			}
			String[] actionPackageStruct = action.getClass().getName().split("\\.");
			String actionStr = actionPackageStruct[actionPackageStruct.length - 1];
			for(Player defendingPlayer : targetedPlayers){
				if(players.contains(defendingPlayer)){
					nameToOutWriter.get(defendingPlayer.toString()).println(Commands.Block + "+++" + 
							actingPlayer + "++" + actionStr + "++" + 
							defensesThatCanBlockString.substring(0, defensesThatCanBlockString.length()));
					try {
						String response = nameToPlayerInput.get(defendingPlayer.toString()).readLine();
						if(response.startsWith(Responses.Block.toString())){
							Defense defense = defenseStrToDefense.get(response.split("\\+\\+\\+")[1]);
							gameHistory += defendingPlayer + " attempted to block with " + defense.cardTypeRequired() + ":::";
							
							for(Player nonEliminatedPlayer : players){
								if(!nonEliminatedPlayer.equals(defendingPlayer)){
									nameToOutWriter.get(nonEliminatedPlayer.toString()).println(Commands.CallBluff.toString() + "+++" + defendingPlayer + ":" + defense.cardTypeRequired());
									try {
										response = nameToPlayerInput.get(nonEliminatedPlayer.toString()).readLine();
										if(response.equals(Responses.AccuseOfBluff.toString())){
											Player bluffCaller = nonEliminatedPlayer;
											if(defendingPlayer.has(defense.cardTypeRequired())){
												//bluff caller is wrong
												bluffCaller.revealACard("Sorry, you were wrong.  " + defendingPlayer + " did have " + defense.cardTypeRequired());
												game.reshuffleCardAndDrawNewCard(defendingPlayer, defense.cardTypeRequired());
												updatePlayerCards();
												defense.defendAgainstPlayer(actingPlayer);
												gameHistory += bluffCaller + " incorrectly accused blocker of bluffing.  " + defendingPlayer + " successfully blocked, thus ending the turn.::::::";
												return;
											}else{
												defendingPlayer.revealACard(bluffCaller + " called your bluff about having " + defense.cardTypeRequired());
												gameHistory += bluffCaller + " correctly accused blocker " + defendingPlayer + " of bluffing.:::";
												performAction(actingPlayer, action); //block failed, player still gets to perform action
												return;
											}
										}
									} catch (IOException e) {
										throw new RuntimeException("Could not get player input",e);
									}
								}
							}
							
							defense.defendAgainstPlayer(actingPlayer);
							gameHistory += defendingPlayer + " successfully blocked, thus ending the turn.::::::";
							
							return;
						}
					} catch (IOException e) {
						throw new RuntimeException("Could not get player input",e);
					}
				}else{
					System.out.println("Targeted player has already been eliminated");
				}
			}
		}
		
		//If no blocking possible or no choice to block:
		performAction(actingPlayer, action);
	}

	private void performAction(Player actingPlayer, Action action) {
		action.performAction(actingPlayer);
		gameHistory += actingPlayer + " successfully completed " + actionString(action);
		if(action.targetedPlayers() != null && action.targetedPlayers().size() == 1){
			gameHistory += " against " + action.targetedPlayers().get(0);
		}
		gameHistory += " thus ending the turn.:::::";
		String moneyString = "";
		for(Player player : players){
			moneyString += player.toString() + "++" + player.getCoins() + ":";
		}
		for(Player player : players){
			nameToOutWriter.get(player.toString()).println(Commands.UpdateCoins + "+++" + moneyString);
		}
		updatePlayerCards();
	}
	
	private String actionString(Action action) {
		String[] actionPackageParts = action.getClass().getName().split("\\.");
		return actionPackageParts[actionPackageParts.length - 1];
	}

	public Player advanceToNextPlayer() {
		
		List<Player> playersToRemove = new ArrayList<Player>();
		for(Player player : players){
			if(player.eliminated()){
				playersToRemove.add(player);
				nameToOutWriter.get(player.toString()).println(Commands.DEFEAT);
			}
		}
		
		nextPlayer = getNextPlayer(playersToRemove);
		
		
		if(players.size() == 1){
			nameToOutWriter.get(players.get(0).toString()).println(Commands.VICTORY);
			gameHistory += players.get(0) + " WON THE GAME!";
			return null;
		}
		else{
			for(Player player : players){
				if(player.toString().equals(nextPlayer.toString())){
					Map<String,Action> textToAction = playerActionMaps.get(player.toString());
					String enabledActions = "";
					for(Entry<String,Action> actionEntry : textToAction.entrySet()){
						if(actionEntry.getValue().canPerformAction(player)){
							enabledActions += (actionEntry.getKey() + "++");
						}
					}
					nameToOutWriter.get(player.toString()).println(Commands.ActionsEnable.toString()+"+++"+enabledActions);
				}else{
					nameToOutWriter.get(player.toString()).println(Commands.ActionsDisable.toString() + ":" + nextPlayer);
				}
			}
			return nextPlayer;
		}
		
	}


	public Player getNextPlayer(List<Player> playersToRemove) {
		Player currentPlayer = players.get(0);
		players.remove(0);
		players.add(currentPlayer); //Move current player to back
		players.removeAll(playersToRemove); //Remove all eliminated players
		return players.get(0); //Player now at front of list is next
	}

	public void updatePlayerCards() {
		String cardsString = Commands.UpdateCards.toString() + "+++";
		for(Player player : players){
			cardsString += player.toString() + "::" + (player.getFirstCard().getType() + ":" + player.getFirstCard().isRevealed() +
					"::" + player.getSecondCard().getType() + ":" + player.getSecondCard().isRevealed() + "++");
		}
		for(Player player : players){
			nameToOutWriter.get(player.toString()).println(cardsString + player.toString());
		}
		
	}
}
