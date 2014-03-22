package game.remote;

import game.Card;
import game.CardType;
import game.Player;
import game.ui.javafx.CommonKnowledgeUI;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

public class CoupApplicationClientSide extends Application {
	
	public static Map<String, Player> allPlayers;
	public static Player playerForUi;
	public static List<String> buttonLabels;
	public static PrintWriter out;
	public static BufferedReader in;
	
	public static String[] gameOptions; //TODO encapsulate better
	private static PlayerUi playerUi;
	private static CommonKnowledgeUI commonUi;
	
	public static PrintWriter initialOutput;
	public static BufferedReader initialInput;

	public CoupApplicationClientSide(){
	}

	private static boolean launched = false;
	
	@Override
	public void start(Stage arg0) throws Exception {
		showGameSelectionScreen(null);
		launched = true;
	}

	public static void startNewGame() {
		Platform.runLater(new Runnable(){

			@Override
			public void run() {
				commonUi = new CommonKnowledgeUI(allPlayers.values());
				playerUi = new PlayerUi(playerForUi,buttonLabels,out,in);
				
			}
			
		});
		
		CoupApplicationClientSide.processNextServerMessage(); //Wait for next command
	}
	
	public static void processNextServerMessage()  {
		String nextAction;
				try {
					nextAction = in.readLine();
				} catch (IOException e) {
					e.printStackTrace();
					throw new RuntimeException(e);
				}
				if(!nextAction.startsWith("Update")){
					System.out.println("NEXT non-update ACTION: " + nextAction);
				}
				if(nextAction.startsWith(Commands.ActionsDisable.toString())){
					final String[] disableAndNextPlayer = nextAction.split(":");
					Platform.runLater(new Runnable(){

						@Override
						public void run() {
							playerUi.disableAllActions();
							if(disableAndNextPlayer.length > 1){
								String currentPlayerTurn = disableAndNextPlayer[1];
								commonUi.updateCurrentPlayer(currentPlayerTurn);
							}
						}
						
					});
				}else if(nextAction.equals(Commands.RevealOnlyUnrevealedCard.toString())){
					Platform.runLater(new Runnable(){

						@Override
						public void run() {
							playerForUi.revealACard(""); //TODO add reason to command?
							playerUi.updateCardLabels();
						}
						
					});
					processNextServerMessage();
				}else if(nextAction.equals(Commands.DEFEAT.toString())){
					Platform.runLater(new Runnable(){

						@Override
						public void run() {
							playerUi.updateToDisplayerDefeat();
						}
						
					});
				}else if(nextAction.equals(Commands.VICTORY.toString())){
					Platform.runLater(new Runnable(){

						@Override
						public void run() {
							playerUi.updateToDisplayerVictory();
							
						}
						
					});
				}else if(nextAction.equals(Responses.NOT_READY.toString())){
					Platform.runLater(new Runnable(){

						@Override
						public void run() {
							playerUi.updateToDisplayGameDone();
							
						}
						
					});
				}
				else{
					
					String[] actionAndDetails = nextAction.split("\\+\\+\\+");
					String action = actionAndDetails[0];
					final String details = actionAndDetails[1];
					if(action.equals(Commands.GAME_OVER.toString())){
						Platform.runLater(new Runnable(){
							@Override
							public void run() {
								playerUi.gameOver(details);
								
							}
						});
					}
					else if(action.equals(Commands.Block.toString())){
						String[] detailParts = details.split("\\+\\+");
						final String attackingPlayer = detailParts[0];
						final String actionAttempting = detailParts[1];
						final List<String> defenseOptions = Arrays.asList(detailParts[2].split(":"));
						Platform.runLater(new Runnable(){
							@Override
							public void run() {
								playerUi.checkIfWantToBlock(attackingPlayer, actionAttempting, defenseOptions);
								
							}
						});
					}
					else if(action.equals(Commands.CallBluff.toString())){
						final String[] splitDetails = details.split(":");
						final String playerAttempting = splitDetails[0];
						final String actionAttempting = splitDetails[1];
						Platform.runLater(new Runnable(){
							@Override
							public void run() {
								if(splitDetails.length > 2){
									String singlePlayerTargeted = details.split(":")[2];
									playerUi.checkIfWantToCallBluff(playerAttempting,actionAttempting,singlePlayerTargeted);
								}else{
									playerUi.checkIfWantToCallBluff(playerAttempting,actionAttempting,"");
								}
								
							}
						});
					}
					else if(action.equals(Commands.RevealCardChoice.toString())){
						Platform.runLater(new Runnable(){

							@Override
							public void run() {
								playerUi.forceToReveal(details);
							}
							
						});
					}
					else if(action.equals(Commands.ActionsEnable.toString())){
						final Set<String> buttonsToEnable = new HashSet<String>(Arrays.asList(details.split("\\+\\+")));
						Platform.runLater(new Runnable(){

							@Override
							public void run() {
								playerUi.enableActions(buttonsToEnable);
								commonUi.updateCurrentPlayer("YOU!");
								
							}
							
						});
					}
					else if(action.equals(Commands.ChooseCards.toString())){
						Platform.runLater(new Runnable(){

							@Override
							public void run() {
								playerUi.displayCardChooser(details);
							}
							
						});
					}
					else if(action.equals(Commands.UpdateCoins.toString())){
						String[] newCoinValues = details.split(":");
						for(int i = 0; i < allPlayers.size(); i++){ //Last spot is just blank
							String[] nameAndValue = newCoinValues[i].split("\\+\\+");
							allPlayers.get(nameAndValue[0]).setCoins(Integer.parseInt(nameAndValue[1]));
						}
						Platform.runLater(new Runnable(){

							@Override
							public void run() {
								playerUi.updateMoneyLabelText();
								commonUi.refresh();
							}
							
						});
						processNextServerMessage();
					}
					else if(action.equals(Commands.UpdateCards.toString())){
						System.out.println("===GOT UPDATE CARDS ACTION===");
						String[] cardDetailsPerPlayer = details.split("\\+\\+");
						for(int i = 0; i < allPlayers.size(); i++){
							String[] cardDetailsPerCard = cardDetailsPerPlayer[i].split("::");
							String playerName = cardDetailsPerCard[0];
							String[] firstCardDetails = cardDetailsPerCard[1].split(":");
							String[] secondCardDetails = cardDetailsPerCard[2].split(":");
							
							allPlayers.get(playerName).replaceFirstCard(buildNewCard(firstCardDetails));
							allPlayers.get(playerName).replaceSecondCard(buildNewCard(secondCardDetails));
						}
						Platform.runLater(new Runnable(){

							@Override
							public void run() {
								playerUi.updateCardLabels();
								commonUi.refresh();
								removeEliminatedPlayers(); //Now that cards are updated, we should know who is eliminated
							}
							
						});
						
						processNextServerMessage();
					}
				}
				

	}


	private static void removeEliminatedPlayers() {
		for(Player player : allPlayers.values()){
			if(player.eliminated()){
				allPlayers.remove(player.toString());
			}
		}
	}


	private static Card buildNewCard(String[] cardDetails) {
		Card newCard = new Card(CardType.valueOf(cardDetails[0]));
		if(cardDetails[1].equalsIgnoreCase(Boolean.TRUE.toString())){
			newCard.reveal();
		}
		return newCard;
	}

	public static void showGameSelectionScreen(final String defaultName) {
		Platform.runLater(new Runnable(){
			@Override
			public void run() {
				// FIXME there is a memory leak with creating a new UI each time - need to figure out what to do with old UIs
				new GameSelectionUI(gameOptions, initialOutput, initialInput, defaultName);
				if(commonUi != null){
					commonUi.hide();
				}
				if(playerUi != null){
					playerUi.hide();
				}
			}
		});
	}

	public static boolean launched() {
		return launched;
	}

}
