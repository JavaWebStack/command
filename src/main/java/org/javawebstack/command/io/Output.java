package org.javawebstack.command.io;

import java.io.*;
import java.nio.charset.StandardCharsets;

public interface Output {

    void print(String text);

    void warn(String warning);

    void error(String error);

    default void error(Throwable t) {
        StringWriter writer = new StringWriter();
        t.printStackTrace(new PrintWriter(writer));
        error(writer.toString());
    }

    default void write(String data) {
        write(data.getBytes(StandardCharsets.UTF_8));
    }

    default void write(byte[] data) {
        write(data, 0, data.length);
    }

    void write(byte[] data, int offset, int length);

    default PrintWriter writer() {
        return new PrintWriter(stream());
    }

    default OutputStream stream() {
        Output that = this;
        return new OutputStream() {
            public void write(int b) throws IOException {
                that.write(new byte[] { (byte) b });
            }
            public void write(byte[] b) throws IOException {
                that.write(b);
            }
            public void write(byte[] b, int off, int len) throws IOException {
                that.write(b, off, len);
            }
        };
    }

}
