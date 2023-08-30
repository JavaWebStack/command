package org.javawebstack.command;

import org.javawebstack.command.bind.AutoInjector;
import org.javawebstack.command.bind.CommandRouteBinder;
import org.javawebstack.command.bind.DefaultRouteAutoInjector;
import org.javawebstack.command.handler.*;
import org.javawebstack.command.io.DefaultInput;
import org.javawebstack.command.io.DefaultOutput;
import org.javawebstack.command.io.Input;
import org.javawebstack.command.io.Output;
import org.javawebstack.command.router.CommandRoute;
import org.javawebstack.command.router.CommandRouter;
import org.javawebstack.command.router.CommandRouterResult;
import org.javawebstack.command.router.CommandValidationException;
import org.javawebstack.command.util.SimpleCommandDeclaration;
import org.reflections.Reflections;

import java.util.*;
import java.util.function.Function;

public class CLI {

    private final CommandRouter router = new CommandRouter();
    private final Map<String, CommandHandler> beforeMiddleware = new HashMap<>();
    private final Map<String, CommandHandler> afterMiddleware = new HashMap<>();

    private final List<AutoInjector> autoInjectors = new ArrayList<>();
    private final Map<String, Function<String, Object>> resolvers = new HashMap<>();
    private ExceptionHandler exceptionHandler = new DefaultExceptionHandler();
    private CommandNotFoundHandler notFoundHandler = new DefaultCommandNotFoundHandler();
    private Function<Class<?>, Object> controllerInitiator = CLI::defaultControllerInitiator;
    private final CommandRouteBinder routeBinder = new CommandRouteBinder(this);

    public CLI() {
        autoInjectors.add(new DefaultRouteAutoInjector());
    }

    public CLI route(CommandRoute route) {
        router.add(route);
        return this;
    }

    public CLI route(String declaration, CommandHandler handler, String... middlewares) {
        SimpleCommandDeclaration decl = new SimpleCommandDeclaration();
        decl.parse(declaration, false);
        return route(decl, handler, middlewares);
    }

    public CLI route(SimpleCommandDeclaration declaration, CommandHandler handler, String... middlewares) {
        CommandRoute route = declaration.create(getResolvers());
        route.handler(handler);
        for (String name : middlewares) {
            CommandHandler before = getBeforeMiddleware(name);
            CommandHandler after = getAfterMiddleware(name);
            if (before == null && after == null) {
                System.out.println("[WARN] Middleware \"" + name + "\" not found!");
                continue;
            }
            if (before != null)
                route.getBeforeMiddlewares().add(before);
            if (after != null)
                route.getAfterMiddlewares().add(after);
        }
        return route(route);
    }

    public CLI controller(Class<?> parentClass, Package p) {
        return controller("", parentClass, p);
    }

    public CLI controller(String globalPrefix, Class<?> parentClass, Package p) {
        Reflections reflections = new Reflections(p.getName());
        reflections.getSubTypesOf(parentClass)
                .stream()
                .map(controllerInitiator)
                .forEach(c -> controller(globalPrefix, c));
        return this;
    }

    public CLI controller(Object controller) {
        return controller("", controller);
    }

    public CLI controller(String globalPrefix, Object controller) {
        routeBinder.bind(globalPrefix, controller);
        return this;
    }

    public boolean execute(String[] args) {
        return execute(args, new DefaultInput(), new DefaultOutput());
    }

    public boolean execute(String[] args, Input input, Output output) {
        try {
            CommandRouterResult result = router.match(args);
            if(!result.isMatched()) {
                notFoundHandler.handleNotFound(this, args, input, output);
                return false;
            }
            CommandContext context = result.getContext();
            context.setInput(input);
            context.setOutput(output);
            CommandRoute route = result.getRoute();
            try {
                return route.execute(context);
            } catch (Exception ex) {
                try {
                    exceptionHandler.handleException(context, ex);
                } catch (Exception hex) {
                    if(output != null) {
                        output.error("Exception in ExceptionHandler");
                        output.error(hex);
                    }
                }
                return false;
            }
        } catch (CommandValidationException e) {
            if(output != null)
                output.error(e.getMessage());
            return false;
        }
    }

    public CLI notFound(CommandNotFoundHandler notFoundHandler) {
        this.notFoundHandler = notFoundHandler;
        return this;
    }

    public CLI exceptionHandler(ExceptionHandler exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
        return this;
    }

    public CLI middleware(String name, CommandHandler handler) {
        beforeMiddleware.put(name, handler);
        return this;
    }

    public CLI afterMiddleware(String name, CommandHandler handler) {
        afterMiddleware.put(name, handler);
        return this;
    }

    public CommandHandler getBeforeMiddleware(String name) {
        return beforeMiddleware.get(name);
    }

    public CommandHandler getAfterMiddleware(String name) {
        return afterMiddleware.get(name);
    }

    public CLI autoInjector(AutoInjector injector) {
        autoInjectors.add(injector);
        return this;
    }

    public List<AutoInjector> getAutoInjectors() {
        return autoInjectors;
    }

    public CLI resolver(String name, Function<String, Object> resolver) {
        resolvers.put(name, resolver);
        return this;
    }

    public Map<String, Function<String, Object>> getResolvers() {
        return resolvers;
    }

    public CLI controllerInitiator(Function<Class<?>, Object> initiator) {
        controllerInitiator = initiator;
        return this;
    }

    private static Object defaultControllerInitiator(Class<?> clazz) {
        try {
            return clazz.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

}
