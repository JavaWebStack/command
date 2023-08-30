package org.javawebstack.command.router;

import java.util.Map;

public class OptionValues {

    private final Map<String, Object> values;

    public OptionValues(Map<String, Object> parameters) {
        this.values = parameters;
    }

    public Map<String, Object> getValues() {
        return values;
    }

    public <T> T value(String name) {
        if(!values.containsKey(name))
            throw new IllegalArgumentException("Unknown option parameter '" + name + "'");
        return (T) values.get(name);
    }

}
