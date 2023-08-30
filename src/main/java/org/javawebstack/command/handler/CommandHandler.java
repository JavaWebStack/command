package org.javawebstack.command.handler;

import org.javawebstack.command.CommandContext;

public interface CommandHandler {

    void handle(CommandContext context);

}
