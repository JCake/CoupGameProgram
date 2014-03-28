package game.remote;

import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class WaitScreenUI extends Stage {

	private Pane pane;
	private Text displayText;
	
	private static String[] messages = new String[]{
		"The Coup card game was originally designed by Rikki Tahta.",
		"Recent printing of Coup was funded by kickstarter.",
		"The Coup kickstarter acquired 5,476 backers contributing a total of $166,390.",
		"A 'coup' is \"the sudden and illegal seizure of a government, usually instigated by a \r\n"
				+ "small group of the existing state establishment ... to depose the established government \r\n"
				+ "and replace it with a new ruling body, civil or military\" - wikipedia",
		"A 'coup' is also known as a coup d'état, a putsch, or an overthrow",
		"From wikipedia:  \"The phrase coup d'État (French pronunciation: ​[ku deta]) is French, \r\n"
				+ "literally meaning a \"stroke of state\" or in practice a \"blow against the state\"",
		"From 1806 through 1991, 25 successful coups have been carried out in Haiti",
		"The United States has experienced only one successful coup, which was a \r\n"
				+ "result of the Wilmington Race Riot of 1898 carried out in Wilmington, North Carolina."
	};

	private int factIndex = 0;
	
	public WaitScreenUI(String gameName, String yourName) {
		super();
		AnchorPane root = new AnchorPane();
		pane = new Pane();
		root.getChildren().add(pane);
		
		displayText = new Text(
				"Waiting on other players for game " + gameName + ".\r\n" +
				"Current Players:\r\n");
		displayText.setLayoutX(5);
		displayText.setLayoutY(15);
		pane.getChildren().add(displayText);
		
		Text headerForFacts = new Text("Enjoy some fun coup facts while you wait!");
		headerForFacts.setLayoutX(5);
		headerForFacts.setLayoutY(130);
		pane.getChildren().add(headerForFacts);
		
		final Text factText = new Text(messages[factIndex]);
		factText.setLayoutX(5);
		factText.setLayoutY(160);
		pane.getChildren().add(factText);
		
		Button advanceFact = new Button("Next Fact");
		advanceFact.setOnMouseClicked(new EventHandler<Event>(){
			@Override
			public void handle(Event arg0) {
				factIndex = (factIndex + 1) % messages.length;
				factText.setText(messages[factIndex]);
			}
		});
		advanceFact.setLayoutX(20);
		advanceFact.setLayoutY(210);
		pane.getChildren().add(advanceFact);
		
		
		Scene scene = new Scene(root, 500, 500);
		setTitle("Waiting for players for game " + gameName);
		setResizable(true);
		setScene(scene);
		this.show();
	}

	public void addPlayer(String newPlayer) {
		displayText.setText(displayText.getText() + newPlayer + "\r\n");
	}

}
