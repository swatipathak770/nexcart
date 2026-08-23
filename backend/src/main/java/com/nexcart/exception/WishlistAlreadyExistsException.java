package com.nexcart.exception;

public class WishlistAlreadyExistsException extends RuntimeException {

    public WishlistAlreadyExistsException(String message) {
        super(message);
    }
}
