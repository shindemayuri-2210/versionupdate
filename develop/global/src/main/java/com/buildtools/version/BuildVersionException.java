package com.buildtools.version;

/**
 * Custom exception for build version update operations.
 */
public class BuildVersionException extends Exception {
    public BuildVersionException(String message) {
        super(message);
    }

    public BuildVersionException(String message, Throwable cause) {
        super(message, cause);
    }
}
