package com.educore.platform.users.exception;

/**
 * Excepción lanzada cuando se intenta registrar un usuario con un email que ya está registrado.
 */
public class EmailAlreadyExistsException extends RuntimeException {

    public EmailAlreadyExistsException(String message) {
        super(message);
    }
}
