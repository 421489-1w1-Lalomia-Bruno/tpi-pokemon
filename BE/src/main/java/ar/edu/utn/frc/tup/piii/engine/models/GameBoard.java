package ar.edu.utn.frc.tup.piii.engine.models;

public class GameBoard {

    private PlayerBoard player1Board;
    private PlayerBoard player2Board;
    private TrainerCard sharedStadium;

    public PlayerBoard getPlayer1Board() { return player1Board; }
    public void setPlayer1Board(PlayerBoard player1Board) { this.player1Board = player1Board; }

    public PlayerBoard getPlayer2Board() { return player2Board; }
    public void setPlayer2Board(PlayerBoard player2Board) { this.player2Board = player2Board; }

    public TrainerCard getSharedStadium() { return sharedStadium; }
    public void setSharedStadium(TrainerCard sharedStadium) { this.sharedStadium = sharedStadium; }
}
