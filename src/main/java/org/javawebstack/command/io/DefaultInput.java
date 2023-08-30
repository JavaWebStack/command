package org.javawebstack.command.io;

import java.io.InputStream;

public class DefaultInput implements Input {

    private final InputStream inputStream;

    public DefaultInput() {
        this(System.in);
    }

    public DefaultInput(InputStream inputStream) {
        this.inputStream = inputStream;
    }

}
