package oj;

import org.jruby.runtime.ThreadContext;
import org.jruby.util.ByteList;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Created by enebo on 8/2/18.
 */
public class FileParserSource extends ParserSource {
    public ByteList json;

    public FileParserSource(ThreadContext context, String file) {

        // FIXME: This is reading whole file into memory and not streaming.
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            BufferedInputStream buf = new BufferedInputStream(fis);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int read;
            while ((read = buf.read(buffer, 0, buffer.length)) != -1) {
                baos.write(buffer, 0, read);
            }
            baos.flush();
            json = new ByteList(baos.toByteArray());
        } catch (IOException e) {
            if (fis != null) {
                try { fis.close(); } catch (IOException e1) {}
            }
        }
    }

    public void appendTo(ByteList buf, int start) {
        buf.append(json, start, currentOffset - start);
    }

    public int advance() {
        return advance(1);
    }

    public int advance(int amount) {
        currentOffset += amount;

        if (currentOffset >= json.getRealSize()) {
            current = EOF;
        } else {
            current = json.get(currentOffset);
        }
        return current;
    }

    public int at(int offset) {
        return json.get(offset);
    }

    public ByteList subStr(int offset, int length) {
        return json.makeShared(offset, length);
    }

    public boolean startsWith(ByteList str) {
        return json.startsWith(str, currentOffset);
    }

    public int peek(int amount) {
        if (currentOffset >= json.getRealSize()) {
            return 0;
        }

        return json.get(currentOffset + amount);
    }
}
