package org.javawebstack.command.io;

import java.io.PrintStream;

public class DefaultOutput implements Output {

    private final PrintStream out;
    private final PrintStream err;

    public DefaultOutput() {
        this(System.out, System.err);
    }

    public DefaultOutput(PrintStream out, PrintStream err) {
        this.out = out;
        this.err = err;
    }

    public void print(String text) {
        out.println(text);
    }

    @Override
    public void warn(String warning) {
        out.println("[WARN] " + warning);
    }

    @Override
    public void error(String error) {
        err.println("[ERROR] " + error);
    }

    @Override
    public void write(byte[] data, int offset, int length) {
        out.write(data, offset, length);
    }

}
