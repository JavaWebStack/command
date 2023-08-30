package org.javawebstack.command.router;

import org.javawebstack.command.CommandContext;
import org.javawebstack.command.handler.CommandExitException;
import org.javawebstack.command.handler.CommandHandler;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

public class CommandRoute {

    List<RouteSegment> segments = new ArrayList<>();
    List<RouteArgument> arguments = new ArrayList<>();
    List<RouteOption> options = new ArrayList<>();
    CommandHandler handler = null;
    List<CommandHandler> beforeMiddlewares = new ArrayList<>();
    List<CommandHandler> afterMiddlewares = new ArrayList<>();

    public CommandRoute handler(CommandHandler handler) {
        this.handler = handler;
        return this;
    }

    public CommandRoute before(CommandHandler before) {
        this.beforeMiddlewares.add(before);
        return this;
    }

    public CommandRoute after(CommandHandler after) {
        this.afterMiddlewares.add(after);
        return this;
    }

    public CommandRoute segment(String name) {
        if(arguments.size() > 0)
            throw new IllegalStateException("Can not add segments after arguments");
        segments.add(new RouteSegment(name));
        return this;
    }

    public CommandRoute dynamicSegment(String name) {
        return dynamicSegment(name, s -> s);
    }

    public CommandRoute dynamicSegment(String name, Function<String, Object> resolver) {
        if(arguments.size() > 0)
            throw new IllegalStateException("Can not add segments after arguments");
        if(segments.stream().anyMatch(a -> a.isDynamic() && a.getName().equals(name)))
            throw new IllegalStateException("Dynamic segment '" + name + "' already exists");
        segments.add(new RouteSegment(name, resolver));
        return this;
    }

    public CommandRoute arg(String name, boolean required) {
        return arg(name, required, null);
    }

    public CommandRoute arg(String name, boolean required, Function<String, Object> resolver) {
        if(arguments.stream().anyMatch(a -> a.getName().equals(name)))
            throw new IllegalStateException("Argument '" + name + "' already exists");
        if(arguments.size() > 0) {
            RouteArgument previousArgument = arguments.get(arguments.size()-1);
            if(previousArgument.isVarArg())
                throw new IllegalStateException("Can not add argument '" + name + "' after vararg '" + previousArgument.getName() + "'");
            if(required && !previousArgument.isRequired())
                throw new IllegalStateException("Can not add required argument '" + name + "' after optional argument '" + previousArgument.getName() + "'");
        }
        arguments.add(new RouteArgument(name, required, false, resolver));
        return this;
    }

    public CommandRoute varArg(String name, boolean required) {
        varArg(name, required, null);
        return this;
    }

    public CommandRoute varArg(String name, boolean required, Function<String, Object> resolver) {
        if(arguments.stream().anyMatch(a -> a.getName().equals(name)))
            throw new IllegalStateException("Argument '" + name + "' already exists");
        if(arguments.size() > 0) {
            RouteArgument previousArgument = arguments.get(arguments.size()-1);
            if(previousArgument.isVarArg())
                throw new IllegalStateException("Can not add argument '" + name + "' after vararg '" + previousArgument.getName() + "'");
            if(required && !previousArgument.isRequired())
                throw new IllegalStateException("Can not add required argument '" + name + "' after optional argument '" + previousArgument.getName() + "'");
        }
        arguments.add(new RouteArgument(name, required, true, resolver));
        return this;
    }

    public CommandRoute flagOption(String name) {
        return option(name, false, false, null);
    }

    public CommandRoute option(String name, boolean required, String... params) {
        return option(name, required, false, o -> Stream.of(params).forEach(o::param));
    }

    public CommandRoute option(String name, boolean required, Consumer<RouteOption> paramConfigurator) {
        return option(name, required, false, paramConfigurator);
    }

    public CommandRoute repeatableOption(String name, boolean required, String... params) {
        return option(name, required, true, o -> Stream.of(params).forEach(o::param));
    }

    public CommandRoute repeatableOption(String name, boolean required, Consumer<RouteOption> paramConfigurator) {
        return option(name, required, true, paramConfigurator);
    }

    public CommandRoute option(String name, boolean required, boolean repeatable, Consumer<RouteOption> paramConfigurator) {
        if(options.stream().anyMatch(o -> o.getName().equals(name)))
            throw new IllegalStateException("Option '" + optionName(name) + "' already exists (use repeatable options instead)");
        RouteOption option = new RouteOption(name, required, repeatable);
        if(paramConfigurator != null)
            paramConfigurator.accept(option);
        options.add(option);
        return this;
    }

    public List<RouteSegment> getSegments() {
        return segments;
    }

    public List<RouteArgument> getArguments() {
        return arguments;
    }

    public List<RouteOption> getOptions() {
        return options;
    }

    public CommandHandler getHandler() {
        return handler;
    }

    public List<CommandHandler> getBeforeMiddlewares() {
        return beforeMiddlewares;
    }

    public List<CommandHandler> getAfterMiddlewares() {
        return afterMiddlewares;
    }

    /**
     * Tries to match the route. This method will not validate arguments and options, see validate
     * @param args command line to parse and match
     * @return CommandRouteMatch if matched, else null
     */
    public CommandParseResult match(String[] args) {
        Stack<String> stack = new Stack<>();
        for(int i=args.length-1; i>=0; i--)
            stack.push(args[i]);
        Map<String, Object> params = new HashMap<>();
        Map<String, List<List<String>>> rawOptions = new HashMap<>();
        List<String> rawArgs = new ArrayList<>();
        int nextSegment = 0;

        while (!stack.isEmpty()) {
            String s = stack.pop();
            if(s.length() >= 2 && s.startsWith("-") && s.charAt(1) != '-') {
                s = s.substring(1);
                for(int i=0; i<s.length()-1; i++) {
                    rawOptions.computeIfAbsent(String.valueOf(s.charAt(i)), k -> new ArrayList<>()).add(new ArrayList<>());
                }
                s = String.valueOf(s.charAt(s.length()-1));
                String on = s;
                RouteOption option = options.stream().filter(o -> o.name.equals(on)).findFirst().orElse(null);
                List<String> values = new ArrayList<>();
                if(option != null) {
                    for(int i=0; i<option.parameters.size() && !stack.isEmpty(); i++) {
                        values.add(stack.pop());
                    }
                }
                rawOptions.computeIfAbsent(s.toLowerCase(Locale.ROOT), k -> new ArrayList<>()).add(values);
            } else if(s.length() >= 3 && s.startsWith("--") && s.charAt(2) != '-') {
                s = s.substring(2);
                if(s.contains("=")) {
                    String[] spl = s.split("=", 2);
                    List<String> values = new ArrayList<>();
                    values.add(spl.length == 2 ? spl[1] : "");
                    rawOptions.computeIfAbsent(spl[0].toLowerCase(Locale.ROOT), k -> new ArrayList<>()).add(values);
                } else {
                    String on = s.toLowerCase(Locale.ROOT);
                    RouteOption option = options.stream().filter(o -> o.name.equalsIgnoreCase(on)).findFirst().orElse(null);
                    List<String> values = new ArrayList<>();
                    if(option != null) {
                        for(int i=0; i<option.parameters.size() && !stack.isEmpty(); i++) {
                            values.add(stack.pop());
                        }
                    }
                    rawOptions.computeIfAbsent(on, k -> new ArrayList<>()).add(values);
                }
            } else {
                if(nextSegment < segments.size()) {
                    RouteSegment segment = segments.get(nextSegment);
                    if(segment.isDynamic()) {
                        Object res = safeApply(segment.getResolver(), s);
                        if(res == null)
                            return null;
                        params.put(segment.getName(), res);
                    } else {
                        if(!segment.getName().equalsIgnoreCase(s))
                            return null;
                    }
                    nextSegment++;
                } else {
                    rawArgs.add(s);
                }
            }
        }
        if(nextSegment < segments.size())
            return null;
        return new CommandParseResult(rawArgs, params, rawOptions);
    }

    /**
     * Validates the parsed result and resolves all args and options
     * @param parsed Result of the match method
     * @return The command context with resolved args, params and options from the "parsed" input
     * @throws CommandValidationException if args or options are invalid
     */
    public CommandContext validate(CommandParseResult parsed) throws CommandValidationException {
        int minArgs = (int) arguments.stream().filter(RouteArgument::isRequired).count();
        int maxArgs = arguments.stream().anyMatch(RouteArgument::isVarArg) ? Integer.MAX_VALUE : arguments.size();
        if(parsed.getArguments().size() < minArgs) {
            RouteArgument missing = arguments.get(parsed.getArguments().size());
            throw new CommandValidationException("Missing required argument '" + missing.getName() + "'");
        }
        if(parsed.getArguments().size() > maxArgs) {
            String unexpected = parsed.getArguments().get(maxArgs);
            throw new CommandValidationException("Unexpected argument: " + unexpected);
        }
        Map<String, Object> resolvedArgs = new HashMap<>();
        for(int i=0; i<parsed.getArguments().size(); i++) {
            RouteArgument argument = arguments.get(Math.min(arguments.size()-1, i));
            String av = parsed.getArguments().get(i);
            Object value = av;
            if(argument.getResolver() != null) {
                value = safeApply(argument.getResolver(), av);
                if(value == null)
                    throw new CommandValidationException("Invalid argument value for argument '" + argument.getName() + "': " + av);
            }
            if(argument.isVarArg()) {
                List<Object> values = (List<Object>) resolvedArgs.computeIfAbsent(argument.getName(), n -> new ArrayList<>());
                values.add(value);
            } else {
                resolvedArgs.put(argument.getName(), value);
            }
        }
        arguments.stream().filter(RouteArgument::isVarArg).forEach(a -> resolvedArgs.computeIfAbsent(a.getName(), n -> new ArrayList<>()));
        for(String o : parsed.getOptions().keySet()) {
            if(options.stream().noneMatch(op -> op.getName().equalsIgnoreCase(o)))
                throw new CommandValidationException("Unknown option '" + optionName(o) + "'");
        }
        Map<String, List<OptionValues>> resolvedOptions = new HashMap<>();
        for(RouteOption option : options) {
            List<List<String>> instances = parsed.getOptions().get(option.getName());
            if(instances == null) {
                if(option.isRequired())
                    throw new CommandValidationException("Missing required option '" + optionName(option.getName()) + "'");
                resolvedOptions.put(option.getName(), new ArrayList<>());
            } else {
                if(instances.size() > 1 && !option.isRepeatable())
                    throw new CommandValidationException("Option '" + optionName(option.getName()) + "' can only be specified once");
                List<OptionValues> optionValues = new ArrayList<>();
                for(List<String> instance : instances) {
                    if(instance.size() > option.getParameters().size()) {
                        if(option.getParameters().size() == 0) {
                            throw new CommandValidationException("The option '" + optionName(option.getName()) + "' does not allow any parameters, given '" + instance.get(0) + "'");
                        } else {
                            // Will never happen because we only parse 0-1 params when we don't know the option, never more
                            throw new RuntimeException("This should be unreachable, if it does check for changes in the match logic");
                        }
                    }
                    if(instance.size() < option.getParameters().size()) {
                        RouteOptionParameter missing = option.getParameters().get(instance.size());
                        throw new RuntimeException("Option '" + optionName(option.getName()) + "' is missing the required parameter #" + instance.size() + " ('" + missing.getName() + "')");
                    }
                    Map<String, Object> optionParams = new HashMap<>();
                    for(int i=0; i<instance.size(); i++) {
                        RouteOptionParameter parameter = option.getParameters().get(i);
                        String iv = instance.get(i);
                        Object value = iv;
                        if(parameter.getResolver() != null) {
                            value = safeApply(parameter.getResolver(), iv);
                            if(value == null)
                                throw new CommandValidationException("Parameter #" + i + " ('" + parameter.getName() + "') of option '" + optionName(option.getName()) + "' has invalid value: " + iv);
                        }
                        optionParams.put(parameter.getName(), value);
                    }
                    optionValues.add(new OptionValues(optionParams));
                }
                resolvedOptions.put(option.getName(), optionValues);
            }
        }
        return new CommandContext(resolvedArgs, parsed.getParameters(), resolvedOptions);
    }

    private static String optionName(String name) {
        if(name.length() == 1)
            return "-" + name;
        return "--" + name;
    }

    public boolean execute(CommandContext context) throws Exception {
        if(handler == null)
            throw new IllegalStateException("Route has no handler");
        for(CommandHandler mw : beforeMiddlewares) {
            try {
                mw.handle(context);
            } catch (CommandExitException ex) {
                return ex.isSuccess();
            }
        }
        try {
            handler.handle(context);
        } catch (CommandExitException ex) {
            return ex.isSuccess();
        }
        for(CommandHandler mw : afterMiddlewares) {
            try {
                mw.handle(context);
            } catch (CommandExitException ex) {
                return ex.isSuccess();
            }
        }
        return true;
    }

    private static <T,R> R safeApply(Function<T, R> function, T value) {
        try {
            return function.apply(value);
        } catch (Exception ex) {
            return null;
        }
    }

}
