package org.javawebstack.command.bind;

import org.javawebstack.command.CommandContext;

import java.util.Map;

public interface AutoInjector {

    Object getValue(CommandContext context, Map<String, Object> extraArgs, Class<?> type);

}
