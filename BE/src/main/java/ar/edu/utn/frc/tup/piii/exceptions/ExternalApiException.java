package ar.edu.utn.frc.tup.piii.exceptions;

import lombok.Getter;

@Getter
public class ExternalApiException extends RuntimeException {

    private final Integer statusCode;

    public ExternalApiException(String message) {
        super(message);
        this.statusCode = null;
    }

    public ExternalApiException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = null;
    }

    public ExternalApiException(String message, Throwable cause, Integer statusCode) {
        super(message, cause);
        this.statusCode = statusCode;
    }
}
