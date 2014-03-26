package game.ui.javafx;

import game.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class CommonKnowledgeUI extends Stage {

	private List<ExposedPlayerInfo> exposedPlayerUIs = new ArrayList<ExposedPlayerInfo>();
	private Text currentPlayer;
	
	private int ySpacing = 150;
	
	public CommonKnowledgeUI(Collection<Player> allPlayers){
		this(allPlayers,"");
	}
	
	public CommonKnowledgeUI(Collection<Player> allPlayers, String gameName){
		
		AnchorPane root = new AnchorPane();
		
		int xLoc = 10;
		int yLoc = 0;
		for(Player player : allPlayers){
			ExposedPlayerInfo playerInfoUI = new ExposedPlayerInfo(player);
			exposedPlayerUIs.add(playerInfoUI);
			playerInfoUI.setLayoutX(xLoc);
			playerInfoUI.setLayoutY(yLoc);
			xLoc += 250;
			if(xLoc >= 500){
				xLoc = 10;
				yLoc += ySpacing;
			}
			root.getChildren().add(playerInfoUI);
		}
		currentPlayer = new Text("Indication of whose turn it is will go here");
		currentPlayer.setLayoutX(5);
		currentPlayer.setLayoutY(ySpacing * ((allPlayers.size() + 1) / 2));
		root.getChildren().add(currentPlayer);
		
		Scene scene = new Scene(root, 500, 50 + ySpacing * ((allPlayers.size() + 1) / 2));
        this.setTitle("Common Knowledge - " + gameName);
        this.setResizable(true);
        this.setScene(scene);
        this.show();
	}
	
	public void refresh(){
		for(ExposedPlayerInfo exposedPlayerUI : exposedPlayerUIs){
			exposedPlayerUI.refresh();
		}
	}

	public void updateCurrentPlayer(String currentPlayerTurn) {
		System.out.println("Updating as turn for " + currentPlayerTurn);
		currentPlayer.setText("Current Player: " + currentPlayerTurn);
	}

	public void updateSelectedAction(String details) {
		// TODO Auto-generated method stub
		currentPlayer.setText(currentPlayer.getText() + "\r\n Has selected " + details);
	}
	
}
