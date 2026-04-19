package com.stock_tracker.grey_toad.exceptions;

import org.hibernate.annotations.NotFound;

public class NotFoundException extends RuntimeException{
    public NotFoundException(String message){
        super(message);
    }
}
