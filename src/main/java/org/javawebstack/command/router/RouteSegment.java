package org.javawebstack.command.router;

import java.util.function.Function;

public class RouteSegment {

    private final String name;
    private final Function<String, Object> resolver;

    public RouteSegment(String name) {
        this.name = name;
        this.resolver = null;
    }

    public RouteSegment(String name, Function<String, Object> resolver) {
        this.name = name;
        this.resolver = resolver;
    }

    public String getName() {
        return name;
    }

    public Function<String, Object> getResolver() {
        return resolver;
    }

    public boolean isDynamic() {
        return resolver != null;
    }

}
