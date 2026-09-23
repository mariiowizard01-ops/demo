package com.fwn.foodwaste.exception;

import jakarta.validation.constraints.NotBlank;

public class CapacityExceededException extends RuntimeException {
    public CapacityExceededException(String msg) { super(msg); }
}
