package org.javawebstack.command.handler;

import org.javawebstack.command.CLI;
import org.javawebstack.command.io.Input;
import org.javawebstack.command.io.Output;

public interface CommandNotFoundHandler {

    void handleNotFound(CLI cli, String[] args, Input input, Output output);

}
