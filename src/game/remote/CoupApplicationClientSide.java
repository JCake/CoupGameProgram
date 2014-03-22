package game.remote;

import game.Card;
import game.CardType;
import game.Player;
import game.ui.javafx.CommonKnowledgeUI;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javafx.application.Application;
import javafx.stage.Stage;

public class CoupApplicationClientSide extends Application {
	
	public static List<Player> allPlayers;
	public static Player playerForUi;
	public static List<String> buttonLabels;
	public static PrintWriter out;
	public static BufferedReader in;
	
	private static PlayerUi playerUi;
	private static CommonKnowledgeUI commonUi;
	
	private static ExecutorService waitingTaskProcessor = Executors.newFixedThreadPool(1);

	public CoupApplicationClientSide(){
	}

	@Override
	public void start(Stage arg0) throws Exception {
		startNewGame();
	}

	public static void startNewGame() {
		playerUi = new PlayerUi(playerForUi,buttonLabels,out,in);
		commonUi = new CommonKnowledgeUI(allPlayers);
		
		CoupApplicationClientSide.processNextServerMessage(); //Wait for next command
	}
	
	public static void processNextServerMessage()  {
		waitingTaskProcessor.execute(new Runnable(){
			@Override
			public void run() {
				String nextAction;
				try {
					nextAction = in.readLine();
					System.out.println(nextAction);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
				if(!nextAction.startsWith("Update")){
					System.out.println("NEXT ACTION: " + nextAction);
				}
				if(nextAction.startsWith(Commands.ActionsDisable.toString())){
					playerUi.disableAllActions();
					String[] disableAndNextPlayer = nextAction.split(":");
					if(disableAndNextPlayer.length > 1){
						String currentPlayerTurn = disableAndNextPlayer[1];
						commonUi.updateCurrentPlayer(currentPlayerTurn);
					}
				}else if(nextAction.equals(Commands.RevealOnlyUnrevealedCard.toString())){
					playerForUi.revealACard(""); //TODO add reason to command?
					playerUi.updateCardLabels();
					processNextServerMessage();
				}else if(nextAction.equals(Commands.DEFEAT.toString())){
					playerUi.updateToDisplayerDefeat();
				}else if(nextAction.equals(Commands.VICTORY.toString())){
					playerUi.updateToDisplayerVictory();
				}else if(nextAction.equals(Responses.NOT_READY.toString())){
					playerUi.updateToDisplayGameDone();
				}
				else{
					/*
					 * Commands.Block + "+++" + 
							players.get(playerNum) + "++" + action + "++" + 
							defensesThatCanBlockString
					 */
					String[] actionAndDetails = nextAction.split("\\+\\+\\+");
					String action = actionAndDetails[0];
					String details = actionAndDetails[1];
					System.out.println(details);
					if(action.equals(Commands.GAME_OVER.toString())){
						playerUi.gameOver(details);
					}
					else if(action.equals(Commands.Block.toString())){
						String[] detailParts = details.split("\\+\\+");
						String attackingPlayer = detailParts[0];
						String actionAttempting = detailParts[1];
						List<String> defenseOptions = Arrays.asList(detailParts[2].split(":"));
						playerUi.checkIfWantToBlock(attackingPlayer, actionAttempting, defenseOptions);
					}
					else if(action.equals(Commands.CallBluff.toString())){
						String[] splitDetails = details.split(":");
						String playerAttempting = splitDetails[0];
						String actionAttempting = splitDetails[1];
						if(splitDetails.length > 2){
							String singlePlayerTargeted = details.split(":")[2];
							playerUi.checkIfWantToCallBluff(playerAttempting,actionAttempting,singlePlayerTargeted);
						}else{
							playerUi.checkIfWantToCallBluff(playerAttempting,actionAttempting,"");
						}
					}
					else if(action.equals(Commands.RevealCardChoice.toString())){
						playerUi.forceToReveal(details);
					}
					else if(action.equals(Commands.ActionsEnable.toString())){
						Set<String> buttonsToEnable = new HashSet<String>(Arrays.asList(details.split("\\+\\+")));
						playerUi.enableActions(buttonsToEnable);
						commonUi.updateCurrentPlayer("YOU!");
					}
					else if(action.equals(Commands.ChooseCards.toString())){
						playerUi.displayCardChooser(details);
					}
					else if(action.equals(Commands.UpdateCoins.toString())){
						String[] newCoinValues = details.split(":");
						for(int i = 0; i < allPlayers.size(); i++){ //FIXME was 'allPlayers' before
							allPlayers.get(i).setCoins(Integer.parseInt(newCoinValues[i]));
						}
						playerUi.updateMoneyLabelText();
						commonUi.refresh();
						processNextServerMessage();
					}
					else if(action.equals(Commands.UpdateCards.toString())){
						System.out.println("===GOT UPDATE CARDS ACTION===");
						String[] cardDetailsPerPlayer = details.split("\\+\\+");
						for(int i = 0; i < allPlayers.size(); i++){
							String[] cardDetailsPerCard = cardDetailsPerPlayer[i].split("::");
							String[] firstCardDetails = cardDetailsPerCard[0].split(":");
							String[] secondCardDetails = cardDetailsPerCard[1].split(":");
							
							allPlayers.get(i).replaceFirstCard(buildNewCard(firstCardDetails));
							allPlayers.get(i).replaceSecondCard(buildNewCard(secondCardDetails));
						}
						playerUi.updateCardLabels();
						commonUi.refresh();
						removeEliminatedPlayers(); //Now that cards are updated, we should know who is eliminated
						processNextServerMessage();
					}
				}
				
			}

			private void removeEliminatedPlayers() {
				List<Player> playersToRemove = new ArrayList<Player>();
				for(Player player : allPlayers){
					if(player.eliminated()){
						playersToRemove.add(player);
					}
				}
				allPlayers.removeAll(playersToRemove);
			}

			private Card buildNewCard(String[] cardDetails) {
				Card newCard = new Card(CardType.valueOf(cardDetails[0]));
				if(cardDetails[1].equalsIgnoreCase(Boolean.TRUE.toString())){
					newCard.reveal();
				}
				return newCard;
			}
		});
	}

}
