package org.javawebstack.command.handler;

import org.javawebstack.command.CommandContext;

public interface ExceptionHandler {

    void handleException(CommandContext context, Exception exception);

}
