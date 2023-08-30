package org.javawebstack.command.bind;

import org.javawebstack.command.CommandContext;

import java.util.Map;

public class DefaultRouteAutoInjector implements AutoInjector {

    public Object getValue(CommandContext context, Map<String, Object> extraArgs, Class<?> type) {
        if(CommandContext.class.equals(type))
            return context;
        return null;
    }

}
