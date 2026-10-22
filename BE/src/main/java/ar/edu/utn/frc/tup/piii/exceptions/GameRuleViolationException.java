package ar.edu.utn.frc.tup.piii.exceptions;

public class GameRuleViolationException extends RuntimeException {
    public GameRuleViolationException(String message) {
        super(message);
    }
}
