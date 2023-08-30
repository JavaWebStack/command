package org.javawebstack.command.router;

import java.util.function.Function;

public class RouteArgument {

    private final String name;
    private final boolean required;
    private final boolean varArg;
    Function<String, Object> resolver;

    public RouteArgument(String name, boolean required, boolean varArg) {
        this(name, required, varArg, null);
    }

    public RouteArgument(String name, boolean required, boolean varArg, Function<String, Object> resolver) {
        this.name = name;
        this.required = required;
        this.varArg = varArg;
        this.resolver = resolver;
    }

    public String getName() {
        return name;
    }

    public boolean isVarArg() {
        return varArg;
    }

    public boolean isRequired() {
        return required;
    }

    public Function<String, Object> getResolver() {
        return resolver;
    }

}
