package org.javawebstack.command.handler;

public class CommandExitException extends RuntimeException {

    private final boolean success;

    public CommandExitException(boolean success) {
        this.success = success;
    }

    public boolean isSuccess() {
        return success;
    }

}
