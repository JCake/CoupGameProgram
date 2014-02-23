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

public class GameControllerServerSide {
	
	private final Game game;
    private final List<Player> players;
    private final List<PrintWriter> outWriters;
    private final List<BufferedReader> playerInputs;
    private int curPlayer = -1;
	
	List<Map<String,Action>> playerActionMaps = new ArrayList<Map<String,Action>>();

	public GameControllerServerSide(Game g, List<PrintWriter> outWriters, List<BufferedReader> playerInputs) {
		this.game = g;
		this.outWriters = outWriters;
		this.playerInputs = playerInputs;
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
			RemoteCardChooser remoteCardChooser = new RemoteCardChooser(players,outWriters,playerInputs);
			ActionList playerActions = new ActionList(g,player,remoteCardChooser);
			Map<String,Action> actionStringToAction = new HashMap<String,Action>();
			String allActionStrings = "";
			for(Action action : playerActions.getAllActions()){
				String actionString = action.actionDescription();
				actionStringToAction.put(actionString, action);
				allActionStrings += actionString + "++";
			}
			allActionStrings = allActionStrings.substring(0,allActionStrings.length()-2);
			playerActionMaps.add(actionStringToAction);
			outWriters.get(i).println(allActionStrings);
		}
	}
	
	public void attemptToPerformAction(int playerNum, String actionStringKey){
		Action action = playerActionMaps.get(playerNum).get(actionStringKey);
		Player actingPlayer = players.get(playerNum);
		
		CardType cardTypeRequired = action.cardTypeRequired();
		if(cardTypeRequired != null){
			for(int i = 0; i < outWriters.size(); i++){
				if(i != playerNum){
					outWriters.get(i).println(Commands.CallBluff.toString() + "+++" + actingPlayer + ":" + cardTypeRequired);
					try {
						String response = playerInputs.get(i).readLine();
						if(response.equals(Responses.AccuseOfBluff.toString())){
							if(actingPlayer.has(cardTypeRequired)){
								//bluff caller is wrong
								Player bluffCaller = players.get(i);
								bluffCaller.revealACard("Sorry, you were wrong.  " + actingPlayer + " did have " + cardTypeRequired);
								//TODO need to reveal two if bluff caller is target of assassin?
								game.reshuffleCardAndDrawNewCard(actingPlayer, cardTypeRequired);
								updatePlayerCards();
								checkForBlockingAndThenPerformAction(playerNum, action);
							}else{
								//TODO still pay if attempting assassination??
								actingPlayer.revealACard(players.get(i) + " called your bluff about having " + cardTypeRequired);
							}
							return;
						}
					} catch (IOException e) {
						throw new RuntimeException("Could not get player input",e);
					}
				}
			}
		}
		
		checkForBlockingAndThenPerformAction(playerNum, action);
	}

	private void checkForBlockingAndThenPerformAction(int playerNum, Action action) {
		List<Player> targetedPlayers = action.targetedPlayers();
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
				int targetedPlayerIndex = players.indexOf(defendingPlayer);
				if(targetedPlayerIndex != -1){
					outWriters.get(targetedPlayerIndex).println(Commands.Block + "+++" + 
							players.get(playerNum) + "++" + actionStr + "++" + 
							defensesThatCanBlockString.substring(0, defensesThatCanBlockString.length()));
					try {
						String response = playerInputs.get(targetedPlayerIndex).readLine();
						if(response.startsWith(Responses.Block.toString())){
							Defense defense = defenseStrToDefense.get(response.split("\\+\\+\\+")[1]);
							
							for(int i = 0; i < outWriters.size(); i++){
								if(i != targetedPlayerIndex){
									outWriters.get(i).println(Commands.CallBluff.toString() + "+++" + defendingPlayer + ":" + defense.cardTypeRequired());
									try {
										response = playerInputs.get(i).readLine();
										if(response.equals(Responses.AccuseOfBluff.toString())){
											if(defendingPlayer.has(defense.cardTypeRequired())){
												//bluff caller is wrong
												Player bluffCaller = players.get(i);
												bluffCaller.revealACard("Sorry, you were wrong.  " + defendingPlayer + " did have " + defense.cardTypeRequired());
												game.reshuffleCardAndDrawNewCard(defendingPlayer, defense.cardTypeRequired());
												updatePlayerCards();
												defense.defendAgainstPlayer(players.get(playerNum));
												return;
											}else{
												defendingPlayer.revealACard(players.get(i) + " called your bluff about having " + defense.cardTypeRequired());
												performAction(playerNum, action); //block failed, player still gets to perform action
												return;
											}
										}
									} catch (IOException e) {
										throw new RuntimeException("Could not get player input",e);
									}
								}
							}
							
							defense.defendAgainstPlayer(players.get(playerNum));
							
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
		performAction(playerNum, action);
	}

	private void performAction(int playerNum, Action action) {
		action.performAction(players.get(playerNum));
		String moneyString = "";
		for(Player player : players){
			moneyString += player.getCoins() + ":";
		}
		for(PrintWriter outWriter : outWriters){
			outWriter.println(Commands.UpdateCoins + "+++" + moneyString);
		}
		updatePlayerCards();
	}
	
	public int advanceToNextPlayer() {
		
		List<Integer> playersToRemove = new ArrayList<Integer>();
		for(int i = 0; i < players.size(); i++){
			if(players.get(i).eliminated()){
				playersToRemove.add(i);
			}
		}
		
		curPlayer = getNexPlayerIndex(curPlayer, playersToRemove);//uisIdsToRemove);
		
		
		if(players.size() == 1){
			outWriters.get(0).println(Commands.VICTORY);
			return -1;
		}
		else{
			for(int i = 0; i < outWriters.size(); i++){
				if(i == curPlayer){
					Player player = players.get(i);
					Map<String,Action> textToAction = playerActionMaps.get(i);
					String enabledActions = "";
					for(Entry<String,Action> actionEntry : textToAction.entrySet()){
						if(actionEntry.getValue().canPerformAction(player)){
							enabledActions += (actionEntry.getKey() + "++");
						}
					}
					outWriters.get(i).println(Commands.ActionsEnable.toString()+"+++"+enabledActions);
				}else{
					outWriters.get(i).println(Commands.ActionsDisable.toString());
				}
			}
			return curPlayer;
		}
		
	}

	/**
	 * 
	 * @param curPlayer index of current player
	 * @param allPlayerUis all non-eliminated players --> will be updated to remove "playersToRemove"
	 * @param playersToRemove indices of players to remove
	 * @return index of player who should go next
	 */
	public int getNexPlayerIndex(int curPlayer, List<Integer> playersToRemove) {
		curPlayer = (curPlayer + 1) % players.size();
		while(playersToRemove.contains(curPlayer)){ //FIXME should NEVER ask to eliminate all of them!
			curPlayer = (curPlayer + 1) % players.size();
		}
		Player nextPlayer = players.get(curPlayer);
		for(int i : playersToRemove){
			players.remove(i);
			outWriters.get(i).println(Commands.DEFEAT);
			outWriters.remove(i); //It's done now!
		}
		return players.indexOf(nextPlayer);
	}

	public void updatePlayerCards() {
		String cardsString = Commands.UpdateCards.toString() + "+++";
		for(Player player : players){
			cardsString += (player.getFirstCard().getType() + ":" + player.getFirstCard().isRevealed() +
					"::" + player.getSecondCard().getType() + ":" + player.getSecondCard().isRevealed() + "++");
		}
		for(int i = 0; i < players.size(); i++){
			outWriters.get(i).println(cardsString + i);
		}
		
	}



//	public void giveOtherPlayersChanceToCallBluff(PlayerWithChoices playerBeingCalled, ActionButton actionAttempting) {
//		for(IndividualPlayer playerUi : allPlayerUis){
//			if(!playerUi.forPlayer(playerBeingCalled)){
//				playerUi.giveChanceToCallBluff(actionAttempting);
//			}
//		}
//		
//	}
//
//	public void closeAllOtherPopups(PlayerWithChoices player) {
//		for(IndividualPlayer playerUi : allPlayerUis){
//			if(!playerUi.forPlayer(player)){
//				playerUi.hidePopup();
//			}
//		}
//	}
//
//	public int getNumberOfRemainingPlayers() {
//		return allPlayerUis.size();
//	}

}
