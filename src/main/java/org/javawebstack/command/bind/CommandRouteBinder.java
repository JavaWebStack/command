package org.javawebstack.command.bind;

import org.javawebstack.command.CLI;
import org.javawebstack.command.util.SimpleCommandDeclaration;
import org.javawebstack.command.bind.annotation.*;
import org.javawebstack.command.bind.annotation.param.Arg;
import org.javawebstack.command.bind.annotation.param.Attrib;
import org.javawebstack.command.bind.annotation.param.Param;
import org.javawebstack.command.CommandContext;
import org.javawebstack.command.handler.CommandHandler;
import org.javawebstack.command.router.CommandRoute;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.stream.Collectors;

public class CommandRouteBinder {

    private final CLI cli;

    public CommandRouteBinder(CLI cli) {
        this.cli = cli;
    }

    public void bind(String globalPrefix, Object controller) {
        List<String> prefixes = Arrays.stream(controller.getClass().getDeclaredAnnotationsByType(CommandPrefix.class)).map(CommandPrefix::value).collect(Collectors.toCollection(ArrayList::new));
        if (prefixes.size() == 0)
            prefixes.add("");
        With with = Arrays.stream(controller.getClass().getDeclaredAnnotationsByType(With.class)).findFirst().orElse(null);

        for (Method method : getMethodsRecursive(controller.getClass())) {
            List<String> commands = new ArrayList<>();
            With methodWith = getAnnotations(With.class, method).stream().findFirst().orElse(null);
            List<String> middlewares = new ArrayList<>();
            if (with != null)
                middlewares.addAll(Arrays.asList(with.value()));
            if (methodWith != null)
                middlewares.addAll(Arrays.asList(methodWith.value()));

            for (Command cmd : getAnnotations(Command.class, method)) {
                commands.add(cmd.value());
            }

            if (commands.size() > 0) {
                BindHandler handler = new BindHandler(cli, controller, method);
                SimpleCommandDeclaration globalDecl = new SimpleCommandDeclaration();
                if(globalPrefix != null && globalPrefix.length() > 0)
                    globalDecl.parse(globalPrefix, true);
                for (String prefix : prefixes) {
                    SimpleCommandDeclaration prefixDecl = globalDecl.clone();
                    if(prefix != null && prefix.length() > 0)
                        prefixDecl.parse(prefix, true);
                    for (String command : commands) {
                        SimpleCommandDeclaration commandDecl = prefixDecl.clone();
                        commandDecl.parse(command, false);
                        cli.route(commandDecl, handler, middlewares.toArray(new String[0]));
                    }
                }
            }
        }
    }

    private static <T extends Annotation> List<T> getAnnotations(Class<T> type, Method method) {
        return Arrays.asList(method.getDeclaredAnnotationsByType(type));
    }

    private static <T extends Annotation> T getAnnotation(Class<T> type, Method method, int param) {
        if (param < 0)
            return null;
        Parameter[] parameters = method.getParameters();
        if (param >= parameters.length)
            return null;
        T[] annotations = parameters[param].getDeclaredAnnotationsByType(type);
        return annotations.length == 0 ? null : annotations[0];
    }

    private static List<Method> getMethodsRecursive(Class<?> type) {
        List<Method> methods = new ArrayList<>(Arrays.asList(type.getDeclaredMethods()));
        if (type.getSuperclass() != null && type.getSuperclass() != Object.class)
            methods.addAll(getMethodsRecursive(type.getSuperclass()));
        return methods;
    }

    private static class BindMapper {

        private final CLI cli;
        private final Object controller;
        private final Method method;
        private final Object[] parameterAnnotations;
        private final Class<?>[] parameterTypes;
        private final String[] defaultValues;

        public BindMapper(CLI cli, Object controller, Method method) {
            this.cli = cli;
            this.controller = controller;
            this.method = method;
            method.setAccessible(true);
            parameterTypes = method.getParameterTypes();
            parameterAnnotations = new Object[parameterTypes.length];
            defaultValues = new String[parameterTypes.length];
            for (int i = 0; i < parameterAnnotations.length; i++) {

                for (Class<? extends Annotation> annotation : new Class[]{Attrib.class, Arg.class, Param.class}) {
                    Annotation annotation1 = getAnnotation(annotation, method, i);
                    if (annotation1 != null) {
                        parameterAnnotations[i] = annotation1;
                    }
                }

                if (parameterAnnotations[i] == null)
                    parameterAnnotations[i] = parameterTypes[i];
            }
        }

        public void invoke(CommandContext context, Map<String, Object> extraArgs) {
            Object[] args = new Object[parameterAnnotations.length];
            for (int i = 0; i < args.length; i++) {
                Object a = parameterAnnotations[i];
                if (a == null)
                    continue;
                if (a instanceof Attrib) {
                    Attrib attrib = (Attrib) parameterAnnotations[i];
                    args[i] = context.attrib(attrib.value());
                } else if (a instanceof Arg) {
                    Arg arg = (Arg) parameterAnnotations[i];
                    args[i] = context.arg(arg.value());
                } else if (a instanceof Param) {
                    Param param = (Param) parameterAnnotations[i];
                    args[i] = context.param(param.value());
                } else {
                    for (AutoInjector autoInjector : cli.getAutoInjectors()) {
                        args[i] = autoInjector.getValue(context, extraArgs, parameterTypes[i]);
                        if (args[i] != null)
                            break;
                    }
                }
            }
            try {
                method.invoke(controller, args);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e.getCause());
            }
        }

    }

    private static class BindHandler implements CommandHandler {
        private final BindMapper handler;

        public BindHandler(CLI cli, Object controller, Method method) {
            handler = new BindMapper(cli, controller, method);
        }

        public void handle(CommandContext context) {
            handler.invoke(context, new HashMap<>());
        }
    }

}
