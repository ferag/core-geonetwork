package org.fao.geonet.handle.client;

public class HandleClientException extends Exception {
    public HandleClientException(String message) {
        super(message);
    }

    public HandleClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
