package org.javawebstack.command.router;

import org.javawebstack.command.CommandContext;

public class CommandRouterResult {

    boolean matched;
    CommandRoute route;
    CommandContext context;

    public CommandRouterResult(boolean matched, CommandRoute route, CommandContext context) {
        this.matched = matched;
        this.route = route;
        this.context = context;
    }

    public boolean isMatched() {
        return matched;
    }

    public CommandRoute getRoute() {
        return route;
    }

    public CommandContext getContext() {
        return context;
    }

}
