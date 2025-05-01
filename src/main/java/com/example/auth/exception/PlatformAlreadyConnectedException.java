package com.example.auth.exception;

public class PlatformAlreadyConnectedException extends RuntimeException {
    public PlatformAlreadyConnectedException(String message) {
        super(message);
    }
}
