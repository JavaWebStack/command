package org.javawebstack.command.router;

import java.util.function.Function;

public class RouteOptionParameter {

    private final String name;
    private final Function<String, Object> resolver;

    public RouteOptionParameter(String name, Function<String, Object> resolver) {
        this.name = name;
        this.resolver = resolver;
    }

    public String getName() {
        return name;
    }

    public Function<String, Object> getResolver() {
        return resolver;
    }

}
