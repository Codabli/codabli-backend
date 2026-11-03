package com.codabli.service;

/**
 * Le controle QwenGuard a echoue ou a renvoye une reponse inexploitable.
 */
public class QwenGuardIndisponibleException extends RuntimeException {

    public QwenGuardIndisponibleException(String message) {
        super(message);
    }

    public QwenGuardIndisponibleException(String message, Throwable cause) {
        super(message, cause);
    }
}
