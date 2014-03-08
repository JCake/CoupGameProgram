package game.ui.javafx;

import game.Player;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class CommonKnowledgeUI extends Stage {

	private List<ExposedPlayerInfo> exposedPlayerUIs = new ArrayList<ExposedPlayerInfo>();
	private Text currentPlayer;
	
	public CommonKnowledgeUI(List<Player> allPlayers){
		
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
				yLoc += 150;
			}
			root.getChildren().add(playerInfoUI);
		}
		currentPlayer = new Text("");
		root.getChildren().add(currentPlayer);
		
		Scene scene = new Scene(root, 500, 150 * ((allPlayers.size() + 1) / 2));
        this.setTitle("Common Knowledge");
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
		currentPlayer.setText("Current Player: " + currentPlayerTurn);
	}
	
}
