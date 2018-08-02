package oj;

import org.jruby.RubyFile;
import org.jruby.RubyIO;
import org.jruby.RubyString;
import org.jruby.ext.stringio.StringIO;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by enebo on 8/1/18.
 */
public class ReadParserSource extends ParserSource {
    public ByteList json;

    public ReadParserSource(ThreadContext context, IRubyObject obj, String method) {
        // FIXME: This is not a reasonable streamer but should be good enough for debugging tests.
        int buffer_size = 20000;
        IRubyObject value = obj.callMethod(context, method, context.runtime.newFixnum(buffer_size));
        if (value instanceof RubyString) {
            json = ((RubyString) value).getByteList();
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
