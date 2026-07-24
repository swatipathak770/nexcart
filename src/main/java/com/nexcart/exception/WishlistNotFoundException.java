package com.nexcart.exception;

public class WishlistNotFoundException extends RuntimeException {

    public WishlistNotFoundException(String message) {
        super(message);
    }
}
