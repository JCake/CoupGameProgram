package game.remote;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class GameSelectionUI extends Stage {

	private Pane pane;
	private static final int xPadding = 15;
	private TextField nameField;

	public GameSelectionUI(String[] allGameOptions, final PrintWriter initialOutput, final BufferedReader initialInput, String defaultName){
		super();
		AnchorPane root = new AnchorPane();
		pane = new Pane();
		
		int currentY = 15;
		
		final Text name = new Text("ENTER YOUR NAME HERE FIRST:");
		name.setLayoutX(xPadding);
		name.setLayoutY(currentY);
		pane.getChildren().add(name);
		
		nameField = new TextField();
		if(defaultName != null){
			nameField.setText(defaultName);
		}
		nameField.setLayoutX(200);
		nameField.setLayoutY(currentY - 15);
		nameField.textProperty().addListener(new ChangeListener<String>() {
	        private final int maxLength = 25;
			@Override
	        public void changed(final ObservableValue<? extends String> ov, final String oldValue, final String newValue) {
	            if (nameField.getText().length() > maxLength) {
	                String s = nameField.getText().substring(0, maxLength);
	                nameField.setText(s);
	            }
	        }
	    });
		pane.getChildren().add(nameField);
		currentY += 40;
		
		if(allGameOptions != null && allGameOptions.length > 0){
			Text joinText = new Text("Click the button for the existing game you'd like to join:");
			joinText.setLayoutX(xPadding);
			joinText.setLayoutY(currentY);
			pane.getChildren().add(joinText);
			
			currentY += 20;
			for(String gameOption : allGameOptions){
				final String[] nameAndDetails = gameOption.split(";;;;;");
				Button gameOptionButton = new Button(nameAndDetails[0]);
				gameOptionButton.setLayoutY(currentY - 5);
				gameOptionButton.setLayoutX(xPadding);
				gameOptionButton.setOnMouseClicked(new EventHandler<Event>(){
					@Override
					public void handle(Event arg0) {
						if(nameField.getText() == null || nameField.getText().isEmpty()){
							name.setFill(Color.RED);
						}else{
							initialOutput.println(nameAndDetails[0]);
							GameSelectionUI.this.hide();
							followThroughOnClientGameCreation(nameAndDetails[0]);
						}
					}
				});
				pane.getChildren().add(gameOptionButton);
				
				Text gameOptionDetail = new Text(nameAndDetails[1]);
				gameOptionDetail.setLayoutY(currentY + 10);
				//FIXME why is it so difficult to get these things lined up?
				gameOptionDetail.setLayoutX(3 * xPadding + 7 * nameAndDetails[0].length());
				pane.getChildren().add(gameOptionDetail);
				
				currentY += 30;
			}
			currentY += 10;
			Text orText = new Text("----OR----");
			orText.setLayoutX(xPadding);
			orText.setLayoutY(currentY);
			pane.getChildren().add(orText);
			currentY += 40;
		}
		Text newText = new Text("Enter a game name below to create a new game:");
		newText.setLayoutX(xPadding);
		newText.setLayoutY(currentY);
		pane.getChildren().add(newText);
		currentY += 20;
		
		final TextField newGameName = new TextField();
		newGameName.setLayoutX(xPadding);
		newGameName.setLayoutY(currentY);
		pane.getChildren().add(newGameName);
		currentY += 40;
		
		final Text numPlayersText = new Text("Total number of players for new game:");
		numPlayersText.setLayoutX(xPadding);
		numPlayersText.setLayoutY(currentY);
		pane.getChildren().add(numPlayersText);
		currentY += 10;
		
		final Slider numPlayersSlider = createNumberOfPlayersSlider(currentY);
		pane.getChildren().add(numPlayersSlider);
		
		Button newGameButton = new Button("Create New Game");
		newGameButton.setLayoutX(xPadding);
		newGameButton.setLayoutY(currentY + 50);
		newGameButton.setOnMouseClicked(new EventHandler<Event>(){
			//TODO handle player entering name of already existing game as the "new game"
			@Override
			public void handle(Event arg0) {
				if(nameField.getText() == null || nameField.getText().isEmpty()){
					name.setFill(Color.RED);
				}else{
					initialOutput.println("NEW GAME;;;;;" + newGameName.getText());
					GameSelectionUI.this.hide();
					try {
						initialInput.readLine(); //This will be the request for the player number
					} catch (IOException e) {
						e.printStackTrace();
					}
					initialOutput.println((int)numPlayersSlider.getValue());
					followThroughOnClientGameCreation(newGameName.getText());
				}
			}
		});
		pane.getChildren().add(newGameButton);
		
		root.getChildren().add(pane);
		
        Scene scene = new Scene(root, 500, 500);
        setTitle("Select A Game");
        setResizable(true);
        setScene(scene);
        this.show();
	}

	private Slider createNumberOfPlayersSlider(int currentY) {
		final Slider numPlayersSlider = new Slider();
		numPlayersSlider.setMin(2);
		numPlayersSlider.setMax(6);
		numPlayersSlider.setValue(3);
		numPlayersSlider.setShowTickLabels(true);
		numPlayersSlider.setShowTickMarks(true);
		numPlayersSlider.setMajorTickUnit(1);
		numPlayersSlider.setMinorTickCount(0);
		numPlayersSlider.setSnapToTicks(true);
		numPlayersSlider.setLayoutX(xPadding);
		numPlayersSlider.setLayoutY(currentY);
		return numPlayersSlider;
	}
	
	public void followThroughOnClientGameCreation(String gameName){
		CoupClient.startUp(gameName, nameField.getText());
	}
}
