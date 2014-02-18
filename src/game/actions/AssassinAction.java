package game.actions;

import game.CardType;
import game.Player;

import java.util.Arrays;
import java.util.List;

public class AssassinAction implements Action {

	private final Player playerToAssassinate;

	public AssassinAction(Player playerToAssassinate) {
		this.playerToAssassinate = playerToAssassinate;
	}

	@Override
	public void performAction(Player playerDoingAssassinating) {
		// TODO Auto-generated method stub
		playerDoingAssassinating.takeActionAssassin();
		playerToAssassinate.revealACard();
	}

	@Override
	public List<Player> targetedPlayers() {
		return Arrays.asList(playerToAssassinate);
	}

	@Override
	public CardType cardTypeRequired() {
		return CardType.assassin;
	}

	@Override
	public boolean canPerformAction(Player player) {
		return player.getCoins() >= 3 && player.getCoins() < 10 && !playerToAssassinate.eliminated();
	}

	@Override
	public String actionDescription() {
		return "Assassin: pay 3 coins and make " + playerToAssassinate + " lose influence, blockable by contessa";
	}

	@Override
	public List<Defense> defensesThatCanBlock() {
		return Arrays.asList((Defense)new ContessaDefense());
	}
	
}
