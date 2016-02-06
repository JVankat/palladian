package ws.palladian.helper.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import org.apache.commons.lang3.Validate;

import ws.palladian.helper.collection.AbstractIterator;

public final class LineIterator extends AbstractIterator<String> implements CloseableIterator<String> {

    private final BufferedReader reader;
    private boolean closed;

    public LineIterator(File filePath) {
        Validate.notNull(filePath, "filePath must not be null");
        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(filePath);
            reader = new BufferedReader(new InputStreamReader(inputStream, FileHelper.DEFAULT_ENCODING));
        } catch (FileNotFoundException e) {
            throw new IllegalStateException(filePath + " not found.");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("Unupported encoding: " + FileHelper.DEFAULT_ENCODING);
        }

    }

    @Override
    protected String getNext() throws Finished {
        if (closed) {
            throw FINISHED;
        }
        try {
            String line = reader.readLine();
            if (line == null) {
                close();
                throw FINISHED;
            }
            return line;
        } catch (IOException e) {
            throw new IllegalStateException("I/O exception while trying to read from file", e);
        }
    }

    @Override
    public void close() throws IOException {
        reader.close();
        closed = true;
    }

}
