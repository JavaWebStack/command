package org.javawebstack.command.handler;

import org.javawebstack.command.CLI;
import org.javawebstack.command.io.Input;
import org.javawebstack.command.io.Output;

public class DefaultCommandNotFoundHandler implements CommandNotFoundHandler {

    public void handleNotFound(CLI cli, String[] args, Input input, Output output) {
        if(output != null)
            output.error("Command not found!");
    }

}
