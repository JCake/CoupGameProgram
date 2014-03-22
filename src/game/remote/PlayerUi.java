package game.remote;

import game.Card;
import game.Player;
import game.actions.Action;
import game.actions.Defense;
import game.ui.javafx.PlayerWithChoices;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class PlayerUi extends Stage{

	private final Player player;
	private static final int COMP_VERT_SPACE = 30;
	
	private List<Button> allActionButtons = new ArrayList<Button>();
	private Text moneyLabel;
	private Text card1Label;
	private Text card2Label;
	private Pane pane;
	private Scene scene;
	private Button card1RevealButton;
	private Button card2RevealButton;
	private Color color;
	private Text infoLabel;
	private PrintWriter printToServer;
	private Button quitButton;
	private Text cannotPlayAgainText;

	public PlayerUi(final Player player, List<String> buttonLabels, 
			final PrintWriter printToServer, final BufferedReader inFromServer){
		super();
		
		this.player = player;
		this.printToServer = printToServer;
		this.setX(0);
		this.setY(0);
		
		int sceneR = randomColorVal();
		int sceneG = randomColorVal();
		int sceneB = randomColorVal();
		color = Color.rgb(sceneR, sceneG, sceneB);
		
		Color labelColor = Color.BLACK;
		
		AnchorPane root = new AnchorPane();
		pane = new Pane();
		
		infoLabel = new Text("");
		infoLabel.setLayoutX(20);
		infoLabel.setLayoutY(10);
		pane.getChildren().add(infoLabel);
		
		card1Label = new Text(getCardDisplay(player.getFirstCard(),1));
		pane.getChildren().add(card1Label);
		card1Label.setFill(labelColor);
		card1Label.setLayoutY(20+2 * COMP_VERT_SPACE/3);
		
		card1RevealButton = new Button("Reveal Card 1");
		card1RevealButton.setVisible(false);
		card1RevealButton.setLayoutY(20);
		card1RevealButton.setLayoutX(240);
		card1RevealButton.setOnMouseClicked(new EventHandler<Event>(){
			@Override
			public void handle(Event arg0) {
				player.getFirstCard().reveal();
				printToServer.println(Responses.FirstCard);
				updateFollowingCardReveal();
			}

		});
		pane.getChildren().add(card1RevealButton);
		
		card2Label = new Text(getCardDisplay(player.getSecondCard(),2));
		pane.getChildren().add(card2Label);
		card2Label.setLayoutY(20+4 * COMP_VERT_SPACE/3);
		card2Label.setFill(labelColor);
		
		card2RevealButton = new Button("Reveal Card 2");
		card2RevealButton.setLayoutY(20+COMP_VERT_SPACE);
		card2RevealButton.setLayoutX(210);
		card2RevealButton.setOnMouseClicked(new EventHandler<Event>(){
			@Override
			public void handle(Event arg0) {
				player.getSecondCard().reveal();
				printToServer.println(Responses.SecondCard);
				updateFollowingCardReveal();
			}
		});
		card2RevealButton.setVisible(false);
		pane.getChildren().add(card2RevealButton);
		
		moneyLabel = new Text(getCoinDisplay(player));
		pane.getChildren().add(moneyLabel );
		moneyLabel.setLayoutY(20+COMP_VERT_SPACE * 2);
		moneyLabel.setFill(labelColor);
		int buttonNumber = 1;
		int currentYPos = 0;
		for(final String actionLabel: buttonLabels){
			Button actionButton = new Button();
			actionButton.setText(actionLabel);
			actionButton.setOnMouseClicked(new EventHandler<Event>(){

				@Override
				public void handle(Event arg0) {
					printToServer.println(actionLabel);
					disableAllActions();
				}
			});
			actionButton.setLayoutX(0);
			currentYPos = 20+(buttonNumber + 2) * COMP_VERT_SPACE;
			actionButton.setLayoutY(currentYPos);
			buttonNumber++;
			actionButton.setDisable(true);
			pane.getChildren().add(actionButton);
			allActionButtons.add(actionButton);
		}
		root.getChildren().add(pane);
		
        scene = new Scene(root, 500, 500);
        setTitle(player.toString());
        setResizable(true);
		scene.setFill(color);
        setScene(scene);
        this.show();
        
        winnerText = new Text("YOU WIN!");
		winnerText.setFont(Font.font("Verdana", 20));
		winnerText.setLayoutX(0);
		winnerText.setLayoutY(40);
		winnerText.setFill(Color.WHITE);
		winnerText.setVisible(false); //hide until end of game
		pane.getChildren().add(winnerText );
		
		loserText = new Text("YOU LOSE! :(");
		loserText.setFont(Font.font("Verdana", 20));
		loserText.setLayoutX(0);
		loserText.setLayoutY(45);
		loserText.setFill(Color.WHITE);
		loserText.setVisible(false); //hide until end of game
		pane.getChildren().add(loserText);
		
		gameHistoryPane = new ScrollPane();
		gameHistoryPane.setLayoutX(10);
		gameHistoryPane.setLayoutY(60);
		gameHistoryPane.setMinSize(400,400);
		gameHistoryPane.setVisible(false);
		gameHistoryText = new Text("Game history will go here");
		gameHistoryText.setVisible(false);
		gameHistoryPane.setContent(gameHistoryText);
		pane.getChildren().add(gameHistoryPane);
		
		cannotPlayAgainText = new Text("Other players declined, choose: ");
		cannotPlayAgainText.setLayoutY(15);
		cannotPlayAgainText.setVisible(false);
		pane.getChildren().add(cannotPlayAgainText);
		
		playAgainButton = new Button("Play again with same players");
		playAgainButton.setVisible(false);
		playAgainButton.setOnMouseClicked(new EventHandler<Event>(){
			@Override
			public void handle(Event arg0) {
				playAgainButton.setDisable(true);
				printToServer.println(Responses.RESTART);
				try {
					String ready = inFromServer.readLine();
					if(ready.equals(Responses.READY.toString())){
						CoupClient.startNewGame(printToServer, inFromServer);
						CoupApplicationClientSide.startNewGame();
					}else if(ready.equals(Responses.NOT_READY.toString())){
						updateToDisplayGameDone();
					}else{
						throw new RuntimeException("Invalid server response: " + ready);
					}
				} catch (IOException e) {
					throw new RuntimeException("No response from server",e);
				}
			}
		});
		pane.getChildren().add(playAgainButton);
		
		quitButton = new Button("Quit");
		quitButton.setVisible(false);
		quitButton.setLayoutX(180);
		quitButton.setOnMouseClicked(new EventHandler<Event>(){
			@Override
			public void handle(Event arg0) {
				printToServer.println(Responses.QUIT);
				System.exit(1);
			}
		});
		pane.getChildren().add(quitButton);
		
		findNewGameButton = new Button("Find a new game");
		findNewGameButton.setVisible(false);
		findNewGameButton.setLayoutX(260);
		findNewGameButton.setOnMouseClicked(new EventHandler<Event>(){
			@Override
			public void handle(Event arg0) {
				printToServer.println(Responses.QUIT);
				CoupClient.establishNewConnection(player.toString());
			}
		});
		pane.getChildren().add(findNewGameButton);
		
		
		cardChooserUI = new CardChooserUI(color, printToServer, pane);
		cardChooserUI.setVisible(false);
		root.getChildren().add(cardChooserUI);
		
		root.getChildren().add(buildBluffCallingPane(currentYPos + 20));
		bluffCallingPane.setVisible(false);
		
		root.getChildren().add(buildBlockingPane(currentYPos + 20));
		blockingPane.setVisible(false);
	}

	private void updateFollowingCardReveal() {
		card1RevealButton.setVisible(false);
		card2RevealButton.setVisible(false);
		infoLabel.setText("");
		updateCardLabels();
		CoupApplicationClientSide.processNextServerMessage();
	}
	
	private String getCardDisplay(Card card, int cardNum) {
		return "Card " + cardNum + " of type " +  card.getType() + " is " + (card.isRevealed() ? "" : "NOT ") + "revealed";
	}

	private String getCoinDisplay(final Player player) {
		return "coins: " + player.getCoins();
	}
	
	private int randomColorVal(){
		return 100 + (int) (Math.random() * 155);
	}
	
	public void disableAllActions() {
		for(Button actionButton : allActionButtons){
			actionButton.setDisable(true);
		}
		CoupApplicationClientSide.processNextServerMessage(); //Wait for next command
	}
	
	public void enableActions(Set<String> buttonsToEnable) {
		for(Button actionButton : allActionButtons){
			actionButton.setDisable(!buttonsToEnable.contains(actionButton.getText()));
		}
	}

	public void updateMoneyLabelText() {
		moneyLabel.setText(this.getCoinDisplay(player));
	}

	public void updateCardLabels() {
		System.out.println("===player has cards:  " + player.getFirstCard() + " and " + player.getSecondCard());
		card1Label.setText(getCardDisplay(player.getFirstCard(),1));
		card2Label.setText(getCardDisplay(player.getSecondCard(),2));
	}

	public void updateToDisplayerVictory() {
		scene.setFill(Color.GREEN);
		for(Node node : pane.getChildren()){
			node.setVisible(false);
		}
		this.winnerText.setVisible(true);
		CoupApplicationClientSide.processNextServerMessage();
	}

	public void updateToDisplayerDefeat() {
		scene.setFill(Color.RED);
		for(Node node : pane.getChildren()){
			node.setVisible(false);
		}
		this.loserText.setVisible(true);
		CoupApplicationClientSide.processNextServerMessage();
	}
	
	public void forceToReveal(String reason) {
		this.toFront();
		infoLabel.setText(reason + "  You must reveal a card");
		card1RevealButton.setVisible(true);
		card2RevealButton.setVisible(true);
	}

	public Paint getColor() {
		return color;
	}
	
	public String getPlayerName(){
		return player.toString();
	}

	public boolean forPlayer(PlayerWithChoices playerBeingCalled) {
		return player.equals(playerBeingCalled);
	}
	
	private Text winnerText;
	private Text loserText;
	
	private ScrollPane gameHistoryPane;
	private Text gameHistoryText;
	
	private CardChooserUI cardChooserUI;
	
	private Text bluffCallingText;
	private Pane bluffCallingPane;
	
	private Text blockingText;
	private Pane blockingPane;
	private List<Button> allBlockingButtons;

	public void checkIfWantToBlock(final String playerAttemptingAction,
			final String actionToBlock, List<String> possibleDefenses) {
		String blockingTextStr = playerAttemptingAction + " is attempting to use " + actionToBlock 
				+" (against you).  Would you like to block? Possible blocking actions are:";
		currentDefenseOptions = possibleDefenses;
		for(int i = 0; i < possibleDefenses.size(); i++){
			blockingTextStr += "\r\n" + (i+1) + ": " + possibleDefenses.get(i);
			allBlockingButtons.get(i).setVisible(true);
		}
		blockingText.setText(blockingTextStr);
		blockingPane.setVisible(true);
		
	}

	private Pane buildBlockingPane(int yOffset) {
		blockingPane = new Pane();
		int yLoc = 20 + yOffset;
		
		blockingText = new Text("Blocking message will go here");
		blockingText.setLayoutY(yLoc);
		blockingPane.getChildren().add(blockingText);
		yLoc += 50;
		
		Button doNotBlockButton = new Button("Click to NOT block");
		doNotBlockButton.setOnMouseClicked(new EventHandler<Event>(){
			@Override
			public void handle(Event arg0) {
				for(Button blockButton : allBlockingButtons){
					blockButton.setVisible(false);
				}
				blockingPane.setVisible(false);
				printToServer.println(Responses.DoNotBlock);
				CoupApplicationClientSide.processNextServerMessage();
			}
		});
		doNotBlockButton.setLayoutY(yLoc);
		yLoc += 30;
		blockingPane.getChildren().add(doNotBlockButton);
		
		allBlockingButtons = new ArrayList<Button>();
		for(int i = 0; i < 5; i++){ //We figure there could never be more than 5 possible blocks...
			final int iCopy = i;
			Button blockButton = new Button("Click to block with defense option #" + (i+1));
			blockButton.setOnMouseClicked(new EventHandler<Event>(){
				@Override
				public void handle(Event arg0) {
					for(Button blockButton : allBlockingButtons){
						blockButton.setVisible(false);
					}
					blockingPane.setVisible(false);
					printToServer.println(Responses.Block+"+++"+getChosenDefense(iCopy));
					CoupApplicationClientSide.processNextServerMessage();
				}
			});
			blockButton.setLayoutY(yLoc);
			yLoc += 30;
			blockButton.setVisible(false);
			blockingPane.getChildren().add(blockButton);
			allBlockingButtons.add(blockButton);
		}
		
		return blockingPane;
		
	}
	
	List<String> currentDefenseOptions;
	private Button playAgainButton;
	private Button findNewGameButton;

	protected String getChosenDefense(int i) {
		return currentDefenseOptions.get(i);
	}

	public Pane buildBluffCallingPane(int currentYPos){
		bluffCallingPane = new Pane();
		
		bluffCallingText = new Text("Bluff calling message will go here");
		bluffCallingText.setLayoutY(currentYPos + 20);
		bluffCallingPane.getChildren().add(bluffCallingText);
		
		Button callBluffButton = new Button("Click to accuse player of bluffing about having this card ");
		callBluffButton.setOnMouseClicked(new EventHandler<Event>(){
			@Override
			public void handle(Event arg0) {
				bluffCallingPane.setVisible(false);
				printToServer.println(Responses.AccuseOfBluff);
				CoupApplicationClientSide.processNextServerMessage();
			}
		});
		callBluffButton.setLayoutY(currentYPos + 40);
		bluffCallingPane.getChildren().add(callBluffButton);
		
		Button doNotCallButton = new Button("Click to NOT accuse player of bluffing about having this card ");
		doNotCallButton.setOnMouseClicked(new EventHandler<Event>(){
			@Override
			public void handle(Event arg0) {
				bluffCallingPane.setVisible(false);
				printToServer.println(Responses.DoNotAccuseOfBluff);
				CoupApplicationClientSide.processNextServerMessage();
			}
		});
		doNotCallButton.setLayoutY(currentYPos + 70);
		bluffCallingPane.getChildren().add(doNotCallButton);
		
		return bluffCallingPane;
	}
	

	public void checkIfWantToCallBluff(String playerAttempting,
			String cardUsedForActionAttempting, String singlePlayerTargeted) {
		bluffCallingText.setText(playerAttempting + " is claiming to have " + cardUsedForActionAttempting
				+ (singlePlayerTargeted.isEmpty() ? "" : (" and is targeting " + singlePlayerTargeted)));
		bluffCallingPane.setVisible(true);
	}

	public void checkIfWantToCallBluff(final Action actionAttempting, 
			final PlayerWithChoices blockingPlayer, final Defense defenseUsing) {
		String[] actionAttemptingNameArray = actionAttempting.getClass().getName().split("\\.");
		bluffCallingText.setText(blockingPlayer + " claiming to have " + defenseUsing.cardTypeRequired() 
				+ " and using it to block " + actionAttemptingNameArray[actionAttemptingNameArray.length - 1]);
	}

	public void displayCardChooser(String cardChoiceDetails) {
		pane.setVisible(false);
		this.cardChooserUI.updateWithChoices(cardChoiceDetails);
		cardChooserUI.setVisible(true);
	}

	public void gameOver(String details) {
		details = details.replaceAll(":::", "\r\n");
		gameHistoryText.setText(details);
		gameHistoryText.setVisible(true);
		gameHistoryPane.setVisible(true);
		playAgainButton.setVisible(true);
		quitButton.setVisible(true);
		findNewGameButton.setVisible(true);
	}

	public void updateToDisplayGameDone() {
		playAgainButton.setVisible(false);
		cannotPlayAgainText.setVisible(true);
		quitButton.setVisible(true);
		findNewGameButton.setVisible(true);
		
		gameHistoryText.setText(gameHistoryText.getText());
	}


	
}
