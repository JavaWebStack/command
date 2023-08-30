package org.javawebstack.command.router;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class RouteOption {

    String name;
    boolean required;
    boolean repeatable;
    List<RouteOptionParameter> parameters = new ArrayList<>();

    public RouteOption(String name, boolean required, boolean repeatable) {
        this.name = name;
        this.required = required;
        this.repeatable = repeatable;
    }

    public RouteOption param(String name) {
        return param(name, null);
    }

    public RouteOption param(String name, Function<String, Object> resolver) {
        if(parameters.stream().anyMatch(p -> p.getName().equals(name)))
            throw new IllegalStateException("Option parameter '" + name + "' already exists");
        parameters.add(new RouteOptionParameter(name, resolver));
        return this;
    }

    public String getName() {
        return name;
    }

    public boolean isRequired() {
        return required;
    }

    public boolean isRepeatable() {
        return repeatable;
    }

    public List<RouteOptionParameter> getParameters() {
        return parameters;
    }

}
