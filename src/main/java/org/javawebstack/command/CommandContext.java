package org.javawebstack.command;

import org.javawebstack.command.handler.CommandExitException;
import org.javawebstack.command.io.Input;
import org.javawebstack.command.io.Output;
import org.javawebstack.command.router.OptionValues;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommandContext {

    private final Map<String, Object> arguments;
    private final Map<String, Object> parameters;
    private final Map<String, List<OptionValues>> options;
    private final Map<String, Object> attributes = new HashMap<>();
    private Input input;
    private Output output;

    public CommandContext(Map<String, Object> arguments, Map<String, Object> parameters, Map<String, List<OptionValues>> options) {
        this.arguments = arguments;
        this.parameters = parameters;
        this.options = options;
    }

    public <T> T arg(String name) {
        if(!arguments.containsKey(name))
            throw new IllegalArgumentException("Unknown argument '" + name + "'");
        return (T) arguments.get(name);
    }

    public <T> List<T> varArg(String name) {
        Object v = arg(name);
        if(!(v instanceof List))
            throw new IllegalArgumentException("Invalid vararg '" + name + "'");
        return (List<T>) v;
    }

    public <T> T param(String name) {
        if(!parameters.containsKey(name))
            throw new IllegalArgumentException("Unknown parameter '" + name + "'");
        return (T) parameters.get(name);
    }

    public boolean hasOption(String name) {
        return optionCount(name) > 0;
    }

    public int optionCount(String name) {
        if(!options.containsKey(name))
            throw new IllegalArgumentException("Unknown option '" + name + "'");
        return options.get(name).size();
    }

    public OptionValues option(String name) {
        return option(name, 0);
    }

    public <T> T option(String name, String param) {
        return option(name, 0).value(param);
    }

    public OptionValues option(String name, int i) {
        if(!options.containsKey(name))
            throw new IllegalArgumentException("Unknown option '" + name + "'");
        List<OptionValues> instances = options.get(name);
        if(i < 0 || i >= instances.size())
            throw new IllegalArgumentException("The option '" + name + "' only exists " + instances.size() + " times, requested index " + i);
        return instances.get(i);
    }

    public CommandContext attrib(String key, Object value) {
        attributes.put(key, value);
        return this;
    }

    public <T> T attrib(String key) {
        return (T) attributes.get(key);
    }

    public void done() {
        throw new CommandExitException(true);
    }

    public void print(String message) {
        if(output != null)
            output.print(message);
    }

    public void warn(String message) {
        if(output != null)
            output.warn(message);
    }

    public void error(String message) {
        if(output != null)
            output.error(message);
    }

    public void panic(String message) {
        if(output != null)
            output.error(message);
        throw new CommandExitException(false);
    }

    public void panic(Throwable t) {
        if(output != null)
            output.error(t);
        throw new CommandExitException(false);
    }

    public Input getInput() {
        return input;
    }

    public Output getOutput() {
        return output;
    }

    public void setInput(Input input) {
        this.input = input;
    }

    public void setOutput(Output output) {
        this.output = output;
    }

}
