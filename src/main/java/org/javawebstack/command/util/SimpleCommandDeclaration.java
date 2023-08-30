package org.javawebstack.command.util;

import org.javawebstack.command.router.CommandRoute;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

public class SimpleCommandDeclaration {

    List<Segment> segments = new ArrayList<>();
    List<Argument> arguments = new ArrayList<>();
    List<Option> options = new ArrayList<>();

    public void parse(String line, boolean prefix) {
        String[] args = Stream.of(line.split(" ")).map(String::trim).filter(s -> !s.isEmpty()).toArray(String[]::new);
        boolean argsStarted = false;
        boolean optionsStarted = false;
        for(int i=0; i<args.length; i++) {
            String a = args[i];
            if(a.startsWith("{")) {
                if(!a.endsWith("}"))
                    throw new IllegalArgumentException("Invalid dynamic command: " + a);
                if(argsStarted || optionsStarted)
                    throw new IllegalArgumentException("Commands must be specified before arguments and options");
                String resolver = "string";
                if(a.contains(":")) {
                    String[] spl = a.split(":", 2);
                    a = spl.length == 2 ? spl[1] : "";
                    resolver = spl[1];
                }
                if(a.length() == 0)
                    throw new IllegalArgumentException("Name of dynamic command can not be empty");
                segments.add(new Segment(a, resolver));
            } if(a.startsWith("<") || a.startsWith("[")) {
                boolean required = a.charAt(0) == '<';
                argsStarted = true;
                if(!a.endsWith(required ? ">" : "]"))
                    throw new IllegalArgumentException("Invalid argument: " + a);
                if(prefix)
                    throw new IllegalArgumentException("Arguments are not allowed in prefixes");
                if(optionsStarted)
                    throw new IllegalArgumentException("Arguments must be specified before options");
                parseArg(a);
            } else if(a.length() >= 2 && a.charAt(0) == '-' && a.charAt(1) != '-') {
                a = a.substring(1);
                optionsStarted = true;
                argsStarted = true;
                while (a.length() > 1 && Character.isAlphabetic(a.charAt(1))) {
                    String on = String.valueOf(a.charAt(0));
                    a = a.substring(1);
                    options.add(new Option(on, false, false, new ArrayList<>()));
                }
                i = parseOption(args, i, a);
            } else if(a.length() >= 3 && a.startsWith("--") && a.charAt(2) != '-') {
                a = a.substring(2);
                optionsStarted = true;
                argsStarted = true;
                i = parseOption(args, i, a);
            } else {
                if(argsStarted || optionsStarted)
                    throw new IllegalArgumentException("Commands must be specified before arguments and options");
                segments.add(new Segment(a, null));
            }
        }
    }

    public CommandRoute create(Map<String, Function<String, Object>> resolvers) {
        CommandRoute route = new CommandRoute();
        apply(route, resolvers);
        return route;
    }

    public void apply(CommandRoute route, Map<String, Function<String, Object>> resolvers) {
        for(Segment s : segments) {
            if(s.resolver == null) {
                route.segment(s.name);
            } else {
                if(!resolvers.containsKey(s.resolver))
                    throw new IllegalArgumentException("Unknown resolver " + s.resolver);
                route.dynamicSegment(s.name, resolvers.get(s.resolver));
            }
        }
        for(Argument a : arguments) {
            Function<String, Object> resolver = null;
            if(a.resolver != null) {
                resolver = resolvers.get(a.resolver);
                if(resolver == null)
                    throw new IllegalArgumentException("Unknown resolver " + a.resolver);
            }
            if(a.vararg) {
                route.varArg(a.name, a.required, resolver);
            } else {
                route.arg(a.name, a.required, resolver);
            }
        }
        for(Option option : options) {
            route.option(option.name, option.required, option.repeatable, o -> {
                for(Option.Param p : option.params) {
                    Function<String, Object> resolver = null;
                    if(p.resolver != null) {
                        resolver = resolvers.get(p.resolver);
                        if(resolver == null)
                            throw new IllegalArgumentException("Unknown resolver " + p.resolver);
                    }
                    o.param(p.name, resolver);
                }
            });
        }
    }

    private void parseArg(String a) {
        boolean vararg = false;
        boolean required = a.charAt(0) == '<';
        a = a.substring(1, a.length()-1);
        String resolver = null;
        if(a.startsWith("...")) {
            vararg = true;
            a = a.substring(3);
        }
        if(!vararg && a.endsWith("...")) {
            vararg = true;
            a = a.substring(0, a.length()-3);
        }
        if(a.contains(":")) {
            String[] spl = a.split(":", 2);
            a = spl.length == 2 ? spl[1] : "";
            resolver = spl[0];
        }
        if(!vararg && a.startsWith("...")) {
            vararg = true;
            a = a.substring(3);
        }
        if(a.length() == 0)
            throw new IllegalArgumentException("Argument name can not be empty");
        arguments.add(new Argument(a, required, vararg, resolver));
    }

    private int parseOption(String[] args, int i, String a) {
        boolean required = false;
        boolean repeatable = false;
        if(a.endsWith("!")) {
            required = true;
            a = a.substring(0, a.length()-1);
        }
        if(a.endsWith("[]")) {
            repeatable = true;
            a = a.substring(0, a.length()-2);
        }
        if(!required && a.endsWith("!")) {
            required = true;
            a = a.substring(0, a.length()-1);
        }
        String name = a;
        List<Option.Param> params = new ArrayList<>();
        while (i + 1 < args.length && args[i+1].startsWith("{")) {
            i++;
            a = args[i];
            if(!a.endsWith("}"))
                throw new IllegalArgumentException("Invalid option parameter (option: " + name + "}: " + a);
            a = a.substring(1, a.length()-1);
            String resolver = null;
            if(a.contains(":")) {
                String[] spl = a.split(":", 2);
                a = spl.length == 2 ? spl[1] : "";
                resolver = spl[0];
            }
            if(a.length() == 0)
                throw new IllegalArgumentException("Option parameter name can not be empty");
            params.add(new Option.Param(a, resolver));
        }
        options.add(new Option(name, required, repeatable, params));
        return i;
    }

    private static class Segment {

        String name;
        String resolver;

        private Segment(String name, String resolver) {
            this.name = name;
            this.resolver = resolver;
        }

    }

    private static class Argument {

        String name;
        boolean required;
        boolean vararg;
        String resolver;

        private Argument(String name, boolean required, boolean vararg, String resolver) {
            this.name = name;
            this.required = required;
            this.vararg = vararg;
            this.resolver = resolver;
        }

    }

    private static class Option {

        String name;
        boolean required;
        boolean repeatable;
        List<Param> params;

        private Option(String name, boolean required, boolean repeatable, List<Param> params) {
            this.name = name;
            this.required = required;
            this.repeatable = repeatable;
            this.params = params;
        }

        private static class Param {

            String name;
            String resolver;

            private Param(String name, String resolver) {
                this.name = name;
                this.resolver = resolver;
            }

        }

    }

    public SimpleCommandDeclaration clone() {
        SimpleCommandDeclaration clone = new SimpleCommandDeclaration();
        clone.segments = new ArrayList<>(segments);
        clone.arguments = new ArrayList<>(arguments);
        clone.options = new ArrayList<>(clone.options);
        return clone;
    }

}
