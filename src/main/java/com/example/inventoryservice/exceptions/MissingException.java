package com.example.inventoryservice.exceptions;

public class MissingException extends RuntimeException {
    public MissingException(String message) {
        super(message);
    }
}
