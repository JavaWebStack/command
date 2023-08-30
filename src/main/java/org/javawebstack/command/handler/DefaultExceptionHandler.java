package org.javawebstack.command.handler;

import org.javawebstack.command.CommandContext;

public class DefaultExceptionHandler implements ExceptionHandler {

    public void handleException(CommandContext context, Exception exception) {
        context.error("Unhandled exception during execution of command!");
        context.getOutput().error(exception);
    }

}
