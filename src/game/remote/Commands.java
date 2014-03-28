package game.remote;

public enum Commands {
	ActionsEnable,
	ActionsDisable,
	UpdateCoins,
	UpdateCards,
	RevealCardChoice,
	RevealOnlyUnrevealedCard, 
	ChooseCards,
	CallBluff,
	Block,
	DEFEAT, VICTORY, GAME_OVER,
	ShowSelectedAction,
	AddPlayer, StartGame
}
