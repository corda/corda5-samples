package com.r3.csde;

public class CsdeException extends Exception {
    public CsdeException(String message, Throwable cause) {
        super(message, cause);
    }
    public CsdeException(String message){
        super(message);
    }
}