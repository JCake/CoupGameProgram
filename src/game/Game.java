package game;

import game.actions.Action;
import game.actions.BluffCallerOption;
import game.actions.Defense;
import game.actions.DefenseChooser;

import java.util.ArrayList;
import java.util.List;

public class Game {
	
	private Deck deck = new Deck();
	private final int numPlayers;
	private final List<Player> players;
	
	public Game(List<Player> players) {
		this.players = players;
		this.numPlayers = players.size();
	}

	public Game(int numPlayers) {
		players = new ArrayList<Player>();
		for(int i = 0; i < numPlayers; i++){
			players.add(new Player());
		}
		this.numPlayers = numPlayers;
	}

	public Deck getDeck() {
		return deck;
	}

	public void deal() {
		for(int i = 0; i < numPlayers; i++){
			players.get(i).receive(deck.deal());
			players.get(i).receive(deck.deal());
		}
	}

	//FIXME Probably should not expose this list... use the "getPlayer" method instead
	public List<Player> getPlayers() {
		return players;
	}

	public void playerAssassinatesOtherPlayer(Player player,
			Player anotherPlayer) {
		if(player.takeActionAssassin()){
			player.takeActionAssassin(); //FIXME this should not be here...
			anotherPlayer.revealACard();
		}
		
	}

	public void playerCoupsOtherPlayer(Player player, Player anotherPlayer) {
		if(player.takeActionCoup()){
			anotherPlayer.revealACard();
		}
	}

	private List<Card> currentAmbassadorCardChoices;
	private ActionChooser actionChooser;

	//NOTE:  Player can choose # cards returned - 2
	public List<Card> playerCanChooseFromTheseCardsAsAmbassador(Player player) {
		List<Card> cardChoices = new ArrayList<Card>();
		if(!player.getFirstCard().isRevealed()){
			cardChoices.add(player.getFirstCard());
		}
		if(!player.getSecondCard().isRevealed()){
			cardChoices.add(player.getSecondCard());
		}
		for(int i = 0; i < 2; i++){
			cardChoices.add(deck.deal());
		}
		currentAmbassadorCardChoices = new ArrayList<Card>(cardChoices);
		return cardChoices;
	}

	public void playerChoosesTheseCardsAsAmbassador(Player player, Card card,
			Card card2) {
		player.replaceFirstCard(card);
		player.replaceSecondCard(card2);
		for(Card choice : currentAmbassadorCardChoices){
			if(!choice.equals(card) && !choice.equals(card2)){
				deck.takeCardBack(choice);
			}
		}
		
	}
	

	public void playerChoosesTheseCardsAsAmbassador(Player player,
			CardPair chosenCards) {
		player.setCards(chosenCards);
		for(Card choice : currentAmbassadorCardChoices){
			if(!choice.equals(chosenCards.getFirstCard()) && !choice.equals(chosenCards.getSecondCard())){
				deck.takeCardBack(choice);
			}
		}
	}

	public boolean isPlayerRightAboutOtherPlayerNotHavingCard(int accusingPlayerNumber, int accussedPlayerNumber,
			CardType cardAccussedPlayerClaimedToHave) {
		return !players.get(accussedPlayerNumber).has(cardAccussedPlayerClaimedToHave);
	}
	
	private boolean isPlayerRightAboutOtherPlayerNotHavingCard(Player accusingPlayer, Player accussedPlayer,
			CardType cardAccussedPlayerClaimedToHave) {
		return !accussedPlayer.has(cardAccussedPlayerClaimedToHave);
	}

	public void playerAccussesOtherPlayerOfBluffing(int accusingPlayerNumber, int accussedPlayerNumber,
			CardType cardAccussedPlayerClaimedToHave) {
		
		if(!isPlayerRightAboutOtherPlayerNotHavingCard(accusingPlayerNumber, accussedPlayerNumber, cardAccussedPlayerClaimedToHave)){
			players.get(accusingPlayerNumber).revealACard();
			reshuffleCardAndDrawNewCard(accussedPlayerNumber,cardAccussedPlayerClaimedToHave);
		}
		else{
			players.get(accussedPlayerNumber).revealACard();
		}
	}

	private void reshuffleCardAndDrawNewCard(int accussedPlayerNumber,
			CardType cardAccussedPlayerClaimedToHave) {
		Player accussedPlayer = players.get(accussedPlayerNumber);
		reshuffleCardAndDrawNewCard(accussedPlayer, cardAccussedPlayerClaimedToHave);
	}

	public void reshuffleCardAndDrawNewCard(
			Player accussedPlayer, CardType cardAccussedPlayerClaimedToHave) {
		if(!accussedPlayer.getFirstCard().isRevealed() && 
				accussedPlayer.getFirstCard().getType().equals(cardAccussedPlayerClaimedToHave)){
			Card cardAccusedOfNotHaving = accussedPlayer.getFirstCard();
			deck.takeCardBack(cardAccusedOfNotHaving);
			accussedPlayer.replaceFirstCard(deck.deal());
		}else{
			Card cardAccusedOfNotHaving = accussedPlayer.getSecondCard();
			deck.takeCardBack(cardAccusedOfNotHaving);
			accussedPlayer.replaceSecondCard(deck.deal());
		}
	}

	public Player getPlayer(int i) {
		return players.get(i);
	}

	public void setActionChooser(ActionChooser actionChooser) {
		this.actionChooser = actionChooser;
	}

	
	private int currentPlayerNum = 0;
	private DefenseChooser defenseChooser;
	private BluffCallerOption bluffCallerOption;
	public void oneTurn() {
		Player currentPlayer = getPlayer(currentPlayerNum);
		Action chosenAction = actionChooser.chooseAction(currentPlayer);
		
		for(int i = 0; i < players.size(); i++){
			if(chosenAction.cardTypeRequired() != null && bluffCallerOption.callBluff(getPlayer(i), currentPlayer, chosenAction.cardTypeRequired())){
				if(isPlayerRightAboutOtherPlayerNotHavingCard(1,currentPlayerNum,chosenAction.cardTypeRequired())){
					currentPlayer.revealACard();
					currentPlayerNum = (currentPlayerNum + 1) % players.size();
					return;
				}else{
					getPlayer(i).revealACard();
					//FIXME Need to reshuffle one of the cards??
					break;
				}
			}
		}
		
		//FIXME should NOT be able to/have to defend after bluff is called successfully?  What if bluff called unsuccessfully?
		if(chosenAction.targetedPlayers() != null){
			//FIXME need to give defense options based on chosen action...
			boolean blocked = false;
			for(Player playerWhoCanBlock : chosenAction.targetedPlayers()){
				Defense chosenDefense = this.defenseChooser.chooseDefense(playerWhoCanBlock);
				
				//FIXME should allow other players to call bluff on block too??  Not just current player?
				
				if(chosenDefense != null){
					
					if(chosenDefense.cardTypeRequired() != null && 
							bluffCallerOption.callBluff(currentPlayer, playerWhoCanBlock, chosenDefense.cardTypeRequired())){
						if(isPlayerRightAboutOtherPlayerNotHavingCard(currentPlayer, playerWhoCanBlock, chosenDefense.cardTypeRequired())){
							playerWhoCanBlock.revealACard();
							chosenAction.performAction(currentPlayer);
							currentPlayerNum = (currentPlayerNum + 1) % players.size();
							return;
						}else{
							currentPlayer.revealACard();
							reshuffleCardAndDrawNewCard(playerWhoCanBlock,chosenDefense.cardTypeRequired());
						}
					}
					
					
					chosenDefense.defendAgainstPlayer(currentPlayer);
					blocked = true;
					break;
				}
			}
			if(!blocked){
				chosenAction.performAction(currentPlayer);
			}
		}else{
			chosenAction.performAction(currentPlayer);
		}
		currentPlayerNum = (currentPlayerNum + 1) % players.size();
		
	}

	public void setDefenseChooser(DefenseChooser defenseChooser) {
		this.defenseChooser = defenseChooser;
		
	}

	public void setBluffCallerOption(BluffCallerOption bluffCallerOption) {
		this.bluffCallerOption = bluffCallerOption;
	}

	public Player getWinner() {
		Player possibleWinner = null;
		for(Player player : players){
			if(!player.eliminated()){ //Still in game
				if(possibleWinner != null){ //Another player was already found not eliminated
					return null; //No single winner
				}else{
					possibleWinner = player; //This player could be the winner
				}
			}
		}
		return possibleWinner;
	}


}
