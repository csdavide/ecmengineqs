package it.doqui.index.ecmengineqs.business.exceptions;

public class BadDataException extends RuntimeException {
    public BadDataException() {
    }

    public BadDataException(String message) {
        super(message);
    }
}
