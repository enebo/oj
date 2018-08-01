package oj;

import org.jruby.util.ByteList;

/**
 * Created by enebo on 8/1/18.
 */
public class StringParserSource extends ParserSource {
    public ByteList json;

    public StringParserSource(ByteList json) {
        this.json = json;
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
