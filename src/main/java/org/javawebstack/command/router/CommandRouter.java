package org.javawebstack.command.router;

import org.javawebstack.command.CommandContext;

import java.util.ArrayList;
import java.util.List;

public class CommandRouter {

    List<CommandRoute> routes = new ArrayList<>();

    public CommandRouter add(CommandRoute route) {
        routes.add(route);
        return this;
    }

    public CommandRouterResult match(String[] args) throws CommandValidationException {
        for(CommandRoute route : routes) {
            CommandParseResult parseResult = route.match(args);
            if(parseResult != null) {
                CommandContext context = route.validate(parseResult);
                return new CommandRouterResult(true, route, context);
            }
        }
        return new CommandRouterResult(false, null, null);
    }

}
