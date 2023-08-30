package org.javawebstack.command.router;

import java.util.List;
import java.util.Map;

public class CommandParseResult {

    private final List<String> arguments;
    private final Map<String, Object> parameters;
    private final Map<String, List<List<String>>> options;

    public CommandParseResult(List<String> arguments, Map<String, Object> parameters, Map<String, List<List<String>>> options) {
        this.arguments = arguments;
        this.parameters = parameters;
        this.options = options;
    }

    public List<String> getArguments() {
        return arguments;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public Map<String, List<List<String>>> getOptions() {
        return options;
    }

}
