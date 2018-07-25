package oj;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyBasicObject;
import org.jruby.RubyBoolean;
import org.jruby.RubyFile;
import org.jruby.RubyFixnum;
import org.jruby.RubyFloat;
import org.jruby.RubyModule;
import org.jruby.RubyNil;
import org.jruby.RubyString;
import org.jruby.RubySymbol;
import org.jruby.platform.Platform;
import org.jruby.runtime.Block;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;

import static oj.NextItem.*;
import static oj.Options.*;

/**
 * Base class for all the parser types.
 *
 * Note: add_value() is common top-level method and addValue is pi-&gt;add_value.
 */
public abstract class Parse {
//    static final boolean HAS_PROC_WITH_BLOCK = false;
    static final int DEC_MAX = 15;
    static final int EXP_MAX = 100000;
    static final ByteList INFINITY = new ByteList(new byte[] { 'I', 'n', 'f', 'i', 'n', 'i', 't', 'y'});

    protected ThreadContext context;
    public ByteList json;
    public int currentOffset;
    public int current;
    public Stack<Val> stack;
    public Block proc;
    public IRubyObject undef;
    public String error = null;
    public Options options;
    public IRubyObject handler;
    public NumInfo ni;
    // C version uses head value of stack even when stack is empty.  We are using
    // Java Stack and we cannot do that.  Store into this field instead.
    public IRubyObject value;
    protected OjLibrary oj = null;
    protected List<IRubyObject> circ_array;

    public Parse(ThreadContext context, Options options, IRubyObject handler) {
        this.context = context;
        this.stack = new Stack<Val>();
        this.proc = null;
        // FIXME: This should only get made once per runtime not per parse
        this.undef = new RubyBasicObject(null);
        this.options = options;
        this.handler = handler;
        this.ni = new NumInfo(this);
        this.currentOffset = 0;
    }

    public void setJSON(ByteList json) {
        this.json = json;
        this.currentOffset = 0;
    }

    public IRubyObject stack_head_val() {
        if (stack.empty()) {
            if (value != null) {
                return value;
            }

            return context.nil;
        }

        return stack.firstElement().val;
    }

    public Val stack_peek() {
        if (stack.empty()) {
            return null;
        }

        return stack.peek();
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
            current = 0;
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

    public int length() {
        return json.getRealSize();
    }

    public void err_init() {
        error = null;
    }

    public boolean err_has() {
        return error != null;
    }

    public void setError(String error) {
        this.error = error;
    }

    public IRubyObject nilValue() {
        return context.runtime.getNil();
    }

    public IRubyObject trueValue() {
        return context.runtime.getTrue();
    }

    public IRubyObject falseValue() {
        return context.runtime.getFalse();
    }

    public IRubyObject newString(ObjectParse stringValue) {
        return null;
    }

    public ThreadContext getContext() {
        return context;
    }

    public Ruby getRuntime() {
        return context.runtime;
    }

    void non_white() {
        while (true) {
            switch(current) {
            case ' ':
            case '\t':
            case '\f':
            case '\n':
            case '\r':
                advance();
                break;
            default:
                return;
            }
        }
    }

    void skip_comment() {
        advance();
        if ('*' == current) {
            advance();
            int length = length();
            for (; currentOffset < length; advance()) {
                if ('*' == current && '/' == peek(1)) {
                    advance(1);
                    return;
                } else if (length <= currentOffset) {
                    setError("comment not terminated");
                    return;
                }
            }
        } else if ('/' == current) {
            for (; true; advance()) {
                switch (current) {
                case '\n':
                case '\r':
                case '\f':
                case '\0':
                    return;
                }
            }
        } else {
            setError("invalid comment format");
        }
    }

    void add_value(IRubyObject rval) {
        Val parent = stack_peek();

        if (parent == null) { // simple add
            addValue(rval);
        } else {
            switch (parent.next) {
            case ARRAY_NEW:
            case ARRAY_ELEMENT:
                appendValue(rval);
                parent.next = ARRAY_COMMA;
                break;
                case HASH_VALUE:
                    setValue(parent, rval);
                    parent.next = HASH_COMMA;
                    break;
                case HASH_NEW:
                case HASH_KEY:
                case HASH_COMMA:
                case NONE:
                case ARRAY_COMMA:
                case HASH_COLON:
                default:
                    setError("expected " + parent.next);
                    break;
            }
        }
    }

    void read_null() {
        if ('u' == advance() && 'l' == advance() && 'l' == advance()) {
            add_value(nilValue());
        } else {
            setError("expected null");
        }
    }
    
    void read_true() {
        if ('r' == advance() && 'u' == advance() && 'e' == advance()) {
            add_value(trueValue());
        } else {
            setError("expected true");
        }
    }

    void read_false() {
        if ('a' == advance() && 'l' == advance() && 's' == advance() && 'e' == advance()) {
            add_value(falseValue());
        } else {
            setError("expected false");
        }
    }

    // FIXME: Uses String.  Should get swapped to bytelist eventually or byte[]
    int read_hex() {
        int	b = 0;

        for (int i = 0; i < 4; i++) {
            int h = peek(i);
            b = b << 4;
            if ('0' <= h && h <= '9') {
                b += h - '0';
            } else if ('A' <= h && h <= 'F') {
                b += h - 'A' + 10;
            } else if ('a' <= h && h <= 'f') {
                b += h - 'a' + 10;
            } else {
                setError("invalid hex character");
                return 0;
            }
        }
        return b;
    }

    void unicode_to_chars(ByteList buf, int code) {
        // FIXME: I think this will not work as written with signed ints (we should use jcodings
        if (0x0000007F >= code) {
            buf.append((char) code);
        } else if (0x000007FF >= code) {
            buf.append(0xC0 | (code >> 6));
            buf.append(0x80 | (0x3F & code));
        } else if (0x0000FFFF >= code) {
            buf.append(0xE0 | (code >> 12));
            buf.append(0x80 | ((code >> 6) & 0x3F));
            buf.append(0x80 | (0x3F & code));
        } else if (0x001FFFFF >= code) {
            buf.append(0xF0 | (code >> 18));
            buf.append(0x80 | ((code >> 12) & 0x3F));
            buf.append(0x80 | ((code >> 6) & 0x3F));
            buf.append(0x80 | (0x3F & code));
        } else if (0x03FFFFFF >= code) {
            buf.append(0xF8 | (code >> 24));
            buf.append(0x80 | ((code >> 18) & 0x3F));
            buf.append(0x80 | ((code >> 12) & 0x3F));
            buf.append(0x80 | ((code >> 6) & 0x3F));
            buf.append(0x80 | (0x3F & code));
        } else if (0x7FFFFFFF >= code) {
            buf.append(0xFC | (code >> 30));
            buf.append(0x80 | ((code >> 24) & 0x3F));
            buf.append(0x80 | ((code >> 18) & 0x3F));
            buf.append(0x80 | ((code >> 12) & 0x3F));
            buf.append(0x80 | ((code >> 6) & 0x3F));
            buf.append(0x80 | (0x3F & code));
        } else {
            setError("invalid Unicode character");
        }
    }

// entered at /
    void read_escaped_str(int start) {
        ByteList buf = new ByteList();
        int	cnt = currentOffset - start;
        int	code;
        Val parent = stack_peek();

        if (0 < cnt) {
            appendTo(buf, start);
        }

        int length = length();
        for (int s = current; '"' != s; s = advance()) {
            if (currentOffset >= length) {
                setError("quoted string not terminated");
                return;
            } else if ('\\' == s) {
                s = advance();
                switch (s) {
                case 'n':	buf.append('\n');	break;
                case 'r':	buf.append('\r');	break;
                case 't':	buf.append('\t');	break;
                case 'f':	buf.append('\f');	break;
                case 'b':	buf.append('\b');	break;
                case '"':	buf.append('"');	break;
                case '/':	buf.append('/');	break;
                case '\\':	buf.append('\\');	break;
                case 'u':
                    advance();
                    if (0 == (code = read_hex()) && err_has()) {
                        return;
                    }
                    advance(3);
                    if (0x0000D800 <= code && code <= 0x0000DFFF) {
                        int	c1 = (code - 0x0000D800) & 0x000003FF;
                        int	c2;

                        advance();
                        if ('\\' != current || 'u' != peek(1)) {
                            advance(-1);
                            setError("invalid escaped character");
                            return;
                        }
                        advance(2);
                        if (0 == (c2 = read_hex()) && err_has()) {
                            return;
                        }
                        advance(3);
                        c2 = (c2 - 0x0000DC00) & 0x000003FF;
                        code = ((c1 << 10) | c2) + 0x00010000;
                    }
                    unicode_to_chars(buf, code);
                    if (err_has()) {
                        return;
                    }
                    break;
                    default:
                        advance(-1);
                        setError("invalid escaped character");
                        return;
                }
            } else {
                buf.append(s);
            }
        }

        if (null == parent) {
            addCStr(buf, start);
        } else {
            switch (parent.next) {
                case ARRAY_NEW:
                case ARRAY_ELEMENT:
                    arrayAppendCStr(buf, start);
                    parent.next =  ARRAY_COMMA;
                    break;
                case HASH_NEW:
                case HASH_KEY:
                    if (undef == (parent.key_val = hashKey(buf))) {
                    parent.key = buf.dup();
                } else {
                    parent.key = new ByteList();
                }
                parent.k1 = (byte) at(start);
                parent.next =  HASH_COLON;
                break;
                case HASH_VALUE:
                    setCStr(parent, buf, 0);
                    parent.next =  HASH_COMMA;
                    break;
                case HASH_COMMA:
                case NONE:
                case ARRAY_COMMA:
                case HASH_COLON:
                default:
                    setError("expected " + parent.next + ", not a string");
                    break;
            }
        }
        // Do I advance or is it advanced at this potin
        //currentOffset = s + 1;
    }

    void read_str() {
        advance();
        int str = currentOffset;
        Val parent = stack_peek();

        int length = length();
        for (; '"' != current; advance()) {
            if (length <= currentOffset) {
                setError("quoted string not terminated");
                return;
            } else if ('\0' == current) {
                setError("NULL byte in string");
                return;
            } else if ('\\' == current) {
                read_escaped_str(str);
                return;
            }
        }
        if (null == parent) { // simple add
            addCStr(subStr(str, currentOffset - str), str);
        } else {
            switch (parent.next) {
                case ARRAY_NEW:
                case ARRAY_ELEMENT:
                    arrayAppendCStr(subStr(str, currentOffset - str), str);
                    parent.next =  ARRAY_COMMA;
                    break;
                case HASH_NEW:
                case HASH_KEY: {
                    if (undef == (parent.key_val = hashKey(str, currentOffset - str))) {
                        parent.key = subStr(str, currentOffset - str);
                    } else {
                        parent.key = new ByteList();
                    }
                    parent.k1 = (byte) at(str);
                    parent.next = HASH_COLON;
                    break;
                }
                case HASH_VALUE:
                    setCStr(parent, str, currentOffset - str);
                    parent.next =  HASH_COMMA;
                    break;
                case HASH_COMMA:
                case NONE:
                case ARRAY_COMMA:
                case HASH_COLON:
                default:
                    setError("expected " + parent.next + ", not a string");
                    break;
            }
        }
    }

    void read_num() {
        ni.reset();
        Val	parent = stack_peek();
        int zero_cnt = 0;
        int start = currentOffset;

        ni.no_big = (FloatDec == options.bigdec_load);

        if ('-' == current) {
            advance();
            ni.neg = true;
        } else if ('+' == current) {
            advance();
        }
        if ('I' == current) {
            if (!startsWith(INFINITY)) {
                setError("not a number or other Object");
                return;
            }
            advance(8);
            ni.infinity = true;
        } else if ('N' == current || 'n' == current) {
            if ('a' != peek(1) || ('N' != peek(2) && 'n' != peek(2))) {
                setError("not a number or other Object");
                return;
            }
            advance(3);
            ni.nan = true;
        } else {
            for (; '0' <= current && current <= '9'; advance()) {
                ni.dec_cnt++;
                if (ni.big) {
                    //ni.big++; // What is this for?
                } else {
                    int	d = (current - '0');

                    if (0 == d) {
                        zero_cnt++;
                    } else {
                        zero_cnt = 0;
                    }
                    // TBD move size check here
                    ni.i = ni.i * 10 + d;
                    if (Long.MAX_VALUE <= ni.i || DEC_MAX < ni.dec_cnt - zero_cnt) {
                        ni.big = true;
                    }
                }
            }
            if ('.' == current) {
                advance();
                for (; '0' <= current && current <= '9'; advance()) {
                    int	d = (current - '0');

                    if (0 == d) {
                        zero_cnt++;
                    } else {
                        zero_cnt = 0;
                    }
                    ni.dec_cnt++;
                    // TBD move size check here
                    ni.num = ni.num * 10 + d;
                    ni.div *= 10;
                    if (Long.MAX_VALUE <= ni.div || DEC_MAX < ni.dec_cnt - zero_cnt) {
                        ni.big = true;
                    }
                }
            }
            if ('e' == current || 'E' == current) {
                boolean	eneg = false;

                ni.hasExp = true;
                advance();
                if ('-' == current) {
                    advance();
                    eneg = true;
                } else if ('+' == current) {
                    advance();
                }
                for (; '0' <= current && current <= '9'; advance()) {
                    ni.exp = ni.exp * 10 + (current - '0');
                    if (EXP_MAX <= ni.exp) {
                        ni.big = true;
                    }
                }
                if (eneg) {
                    ni.exp = -ni.exp;
                }
            }
            ni.dec_cnt -= zero_cnt;
            ni.str_start = start;
            ni.str_length = currentOffset - start;
        }
        advance(-1);
        if (BigDec == options.bigdec_load) {
            ni.big = true;
        }
        if (null == parent) {
            addNum(ni);
        } else {
            switch (parent.next) {
                case ARRAY_NEW:
                case ARRAY_ELEMENT:
                    appendNum(ni);
                    parent.next = ARRAY_COMMA;
                    break;
                case HASH_VALUE:
                    setNum(parent, ni);
                    parent.next = HASH_COMMA;
                    break;
                default:
                    setError("expected " + parent.next);
                    break;
            }
        }
    }

    void array_start() {
        //System.err.println("array_start");
        stack.push(new Val(startArray(), ARRAY_NEW));
    }

    void array_end() {
        //System.err.println("array_end");
        Val	array = stack.pop();

        if (null == array) {
            setError("unexpected array close");
        } else if (ARRAY_COMMA != array.next && ARRAY_NEW != array.next) {
            setError("expected " + array.next + ", not an array close");
        } else {
            endArray();
            add_value(array.val);
        }
    }

    void hash_start() {
        stack.push(new Val(startHash(), HASH_NEW));
    }

    void hash_end() {
        Val	hash = stack_peek();

        // leave hash on stack until just before
        if (null == hash) {
            setError("unexpected hash close");
        } else if (HASH_COMMA != hash.next && HASH_NEW != hash.next) {
            setError("expected " + hash.next + ", not a hash close");
        } else {
            endHash();
            stack.pop();
            add_value(hash.val);
        }
    }

    void comma() {
        Val	parent = stack_peek();

        if (null == parent) {
            setError("unexpected comma");
        } else if (ARRAY_COMMA == parent.next) {
            parent.next = ARRAY_ELEMENT;
        } else if (HASH_COMMA == parent.next) {
            parent.next = HASH_KEY;
        } else {
            setError("unexpected comma");
        }
    }

    void colon() {
        Val	parent = stack_peek();

        if (null != parent && HASH_COLON == parent.next) {
            parent.next = HASH_VALUE;
        } else {
            setError("unexpected colon");
        }
    }

    void oj_parse2() {
        boolean first = true;

        err_init();
        for (advance(0); true; advance()) {
            //System.out.println("CURBEG: " + (char) current);
            non_white();

            if (!first && '\0' != current) {
                setError("unexpected characters after the JSON document");
            }

            switch (current) {
                case '{':
                    hash_start();
                    break;
                case '}':
                    hash_end();
                    break;
                case ':':
                    colon();
                    break;
                case '[':
                    array_start();
                    break;
                case ']':
                    array_end();
                    break;
                case ',':
                    comma();
                    break;
                case '"':
                    read_str();
                    break;
                case '+':
                case '-':
                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                case 'I':
                case 'N':
                    read_num();
                    break;
                case 't':
                    read_true();
                    break;
                case 'f':
                    read_false();
                    break;
                case 'n':
                    if ('u' == peek(1)) {
                        read_null();
                    } else {
                        read_num();
                    }
                    break;
                case '/':
                    skip_comment();
                    break;
                case '\0':
                    advance(-1);
                    return;
                default:
                    setError("unexpected character");
                    return;
            }
            if (err_has()) {
                return;
            }
            //System.out.println("CUREND: " + (char) current);
            if (stack.isEmpty()) {
                if (proc != null) {
                    proc.yield(getContext(), stack_head_val());
/*                } else {
                    if (HAS_PROC_WITH_BLOCK) {
                        Object[] args= new Object[] { stack.firstElement() };
                        proc.call(args);
                    } else {
                        throw getRuntime().newNotImplementedError("Calling a Proc with a block not supported in this version. Use func() {|x| } syntax instead.");
                    }
                }*/
                } else {
                    first = false;
                }
            }
        }
    }

    void oj_set_error_at(String format, String... data) {
        //RubyModule errorClass = getRuntime().getClass("Oj").getClass("ParseError");
        String file = context.getFile();
        int line = context.getLine();

        if (null == json) {
            // FIXME:
            //oj_err_set(err, err_clas, "%s at line %d, column %d [%s:%d]", msg, rd.line, rd.col, file, line);
        } else {
            // FIXME:
            //_oj_err_set_with_location(err, err_clas, msg, json, currentOffset - 1, file, line);
        }
}

    IRubyObject protect_parse() {
        oj_parse2();

        return nilValue();
    }

    void oj_pi_set_input_str(ByteList input) {
        json = input;
    }

    IRubyObject calc_hash_key(Val kval) {
        IRubyObject rkey;

        if (undef == kval.key_val) {
            rkey = getRuntime().newString(kval.key);
        } else {
            rkey = kval.key_val;
        }

        rkey = oj_encode(rkey);
        if (Yes == options.sym_key) {
            if (rkey instanceof RubyString) {
                return getRuntime().newSymbol(((RubyString) rkey).getByteList());
            } else if (!(rkey instanceof RubySymbol) && rkey.respondsTo("to_sym")) {
                return Helpers.invoke(context, rkey, "to_sym");
            }
        }

        return rkey;
    }

    IRubyObject oj_encode(IRubyObject str) {
        // FIXME: Add 1.9 + 1.8 ability to convert to UTF-8
        return str;
    }

    // FIXME:
    public IRubyObject sparse(OjLibrary oj, IRubyObject[] args, InputStream fd, Block block) {
        this.oj = oj;

        return null;
    }

    public IRubyObject parse(OjLibrary oj, IRubyObject[] args, ByteList json) {
        return parse(oj, args, json, false, Block.NULL_BLOCK);
    }

    public IRubyObject parse(OjLibrary oj, IRubyObject[] args, ByteList json, boolean yieldOk, Block block) {
        this.oj = oj;

        Ruby runtime = context.runtime;
        IRubyObject	input;
        IRubyObject result;
        int			line = 0;

        if (args.length < 1) {
            throw getRuntime().newArgumentError("Wrong number of arguments to parse.");
        }
        input = args[0];
        if (2 == args.length) {
            RubyOj.parse_options(context, args[1], options);
        }
        if (yieldOk && block.isGiven()) {
            proc = block;
        } else {
            proc = null;
        }
        if (null != json) {
            this.json = json;
        } else if (input instanceof RubyString) {
            oj_pi_set_input_str(((RubyString) input).getByteList());
        } else if (nilValue() == input && Yes == options.nilnil) {
            return nilValue();
        } else {
            RubyModule clas = input.getMetaClass();

            if (runtime.getClass("StringIO") == clas) {
                input = input.callMethod(context, "string");
            } else if (!Platform.IS_WINDOWS && runtime.getFile() == clas && 0 == input.callMethod(context, "pos").convertToInteger().getLongValue()) {
                input = ((RubyFile) input).read(context);
            } else if (input.respondsTo("read")) {
                throw runtime.newArgumentError("FIXME: No streaming parser");
                // use stream parser instead
                // FIXME:
                //return oj_pi_sparse(args, pi, 0);
            } else {
                throw runtime.newArgumentError("strict_parse() expected a String or IO Object.");
            }

            if (!(input instanceof RubyString)) {
                throw runtime.newArgumentError("strict_parse() expected a String or IO Object.");
            }
            this.json = ((RubyString) input).getByteList();
        }

        if (Yes == options.circular) {
            circ_array = new ArrayList<>();
        } else {
            circ_array = null;
        }

        protect_parse();

        result = value;
        if (!err_has()) {
            // If the stack is not empty then the JSON terminated early.
            Val	v;

            if (null != (v = stack_peek())) {
                switch (v.next) {
                    case ARRAY_NEW:
                    case ARRAY_ELEMENT:
                    case ARRAY_COMMA:
                        setError("Array not terminated");
                        break;
                    case HASH_NEW:
                    case HASH_KEY:
                    case HASH_COLON:
                    case HASH_VALUE:
                    case HASH_COMMA:
                        setError("Hash/Object not terminated");
                        break;
                    default:
                        setError("not terminated");
                }
            }
        }
        // proceed with cleanup
        if (0 != line) {
            // FIXME:
            //rb_jump_tag(line);
        }

        if (err_has()) {
            parseError(error);
        }

        if (options.quirks_mode == No) {
            if (result instanceof RubyNil || result instanceof RubyBoolean || result instanceof RubyFixnum ||
                    result instanceof RubyFloat || result instanceof RubyModule || result instanceof RubySymbol) {
                parseError("unexpected non-document Object");
            }
        }
        return result;
    }

    // Equivalent to C-callbacks in C impl.  Look to Parse subclasses for specific overrides.
    // Java naming conventions used for call back to avoid naming conflicts with the recursive
    // descent parser method names.

    public void addCStr(ByteList value, int orig) {
    }

    public void arrayAppendCStr(ByteList value, int orig) {
    }

    public void setCStr(Val kval, int start, int length) {
        setCStr(kval, subStr(start, length), start);
    }

    public void setCStr(Val parent, ByteList value, int orig) {
    }

    public void addValue(IRubyObject value) {
        this.value = value;
    }

    public void appendValue(IRubyObject value) {
        ((RubyArray) stack_peek().val).append(value);
    }

    public void setValue(Val parent, IRubyObject value) {
    }

    public void addNum(NumInfo value) {
    }

    public void appendNum(NumInfo value) {
    }

    public void setNum(Val parent, NumInfo value) {
    }

    public IRubyObject endArray() {
        return context.nil;
    }

    public IRubyObject startArray() {
        return getRuntime().newArray();
    }

    public IRubyObject endHash() {
        return context.nil;
    }

    public IRubyObject startHash() {
        return context.nil;
    }

    // For hash keys which came in with no escape characters.
    public IRubyObject hashKey(int start, int length) {
        return undef;
    }

    public IRubyObject hashKey(ByteList key) {
        return undef;
    }

    public void parseError(String message) {
        throw context.runtime.newRaiseException(oj.getParseError(), message);
    }

}
