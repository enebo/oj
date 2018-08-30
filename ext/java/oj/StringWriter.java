package oj;

import oj.dump.Dump;
import oj.options.DumpType;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.RubyString;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.TypeConverter;

import static oj.options.DumpType.*;

/** Document-class: Oj::StringWriter
 *
 * Supports building a JSON document one element at a time. Build the document
 * by pushing IRubyObjects into the document. Pushing an array or an object will
 * create that element in the JSON document and subsequent pushes will add the
 * elements to that array or object until a pop() is called. When complete
 * calling to_s() will return the JSON document. Note tha calling to_s() before
 * construction is complete will return the document in it's current state.
 */
public class StringWriter extends RubyObject {
    public StrWriter sw;
    private Dump dump;

    private static ObjectAllocator ALLOCATOR = new ObjectAllocator() {
        @Override
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new StringWriter(runtime, klass);
        }
    };

    public static RubyClass createStringWriterClass(Ruby runtime, RubyModule oj) {
        RubyClass clazz = oj.defineClassUnder("StringWriter", runtime.getObject(), ALLOCATOR);
        clazz.defineAnnotatedMethods(StringWriter.class);
        return clazz;
    }

    public StringWriter(Ruby runtime, RubyClass metaClass) {
        super(runtime, metaClass);
    }

    @JRubyMethod(optional = 1)
    public IRubyObject initialize(ThreadContext context, IRubyObject[] argv) {
        OjLibrary oj = RubyOj.oj(context);
        sw = new StrWriter(context, oj);

        if (1 == argv.length) {
            RubyOj.parse_options(context, argv[0], sw.opts);
        }
        sw.out.indent = sw.opts.indent;

        dump = Dump.createDump(context, sw.out, sw.opts.mode);

        return this;
    }

    /* call-seq: push_key(key)
     *
     * Pushes a key onto the JSON document. The key will be used for the next push
     * if currently in a JSON object and ignored otherwise. If a key is provided on
     * the next push then that new key will be ignored.
     * @param [String] key the key pending for the next push
     */
    @JRubyMethod
    public IRubyObject push_key(ThreadContext context, IRubyObject key) {
        RubyString string = (RubyString) TypeConverter.checkStringType(context.runtime, key);
        push_key(context, sw, string.getByteList());
        return context.nil;
    }

    /* call-seq: push_object(key=nil)
     *
     * Pushes an object onto the JSON document. Future pushes will be to this object
     * until a pop() is called.
     * @param [String] key the key if adding to an object in the JSON document
     */
    @JRubyMethod
    public IRubyObject push_object(ThreadContext context, Block block) {
        push_object(context, sw, null);

        if (block.isGiven()) {
            block.yield(context, context.nil);
            pop(context, sw);
        }
        return context.nil;
    }

    @JRubyMethod
    public IRubyObject push_object(ThreadContext context, IRubyObject key, Block block) {
        if (context.nil == key) {
            push_object(context, sw, null);
        } else {
            RubyString string = (RubyString) TypeConverter.checkStringType(context.runtime, key);
            push_object(context, sw, string.getByteList());
        }

        if (block.isGiven()) {
            block.yield(context, context.nil);
            pop(context, sw);
        }

        return context.nil;
    }

    /* call-seq: push_array(key=nil)
     *
     * Pushes an array onto the JSON document. Future pushes will be to this object
     * until a pop() is called.
     * @param [String] key the key if adding to an object in the JSON document
     */
    @JRubyMethod
    public IRubyObject push_array(ThreadContext context, Block block) {
        push_array(context, sw, null);

        if (block.isGiven()) {
            block.yield(context, context.nil);
            pop(context, sw);
        }
        return context.nil;
    }

    @JRubyMethod
    public IRubyObject push_array(ThreadContext context, IRubyObject key, Block block) {
        if (context.nil == key) {
            push_array(context, sw, null);
        } else {
            RubyString string = (RubyString) TypeConverter.checkStringType(context.runtime, key);
            push_array(context, sw, string.getByteList());
        }

        if (block.isGiven()) {
            block.yield(context, context.nil);
            pop(context, sw);
        }
        return context.nil;
    }

    /* call-seq: push_value(value, key=nil)
     *
     * Pushes a IRubyObject onto the JSON document.
     * @param [Object] IRubyObject IRubyObject to add to the JSON document
     * @param [String] key the key if adding to an object in the JSON document
     */
    @JRubyMethod
    public IRubyObject push_value(ThreadContext context, IRubyObject value) {
        push_value(context, sw, value, null);
        return context.nil;
    }

    @JRubyMethod
    public IRubyObject push_value(ThreadContext context, IRubyObject value, IRubyObject key) {
        if (context.nil == key) {
            push_value(context, sw, value, null);
        } else {
            RubyString string = (RubyString) TypeConverter.checkStringType(context.runtime, key);
            push_value(context, sw, value, string.getByteList());
        }
        return context.nil;
    }

    /* call-seq: push_json(value, key=nil)
     *
     * Pushes a string onto the JSON document. The String must be a valid JSON
     * encoded string. No additional checking is done to verify the validity of the
     * string.
     * @param [String] IRubyObject JSON document to add to the JSON document
     * @param [String] key the key if adding to an object in the JSON document
     */
    @JRubyMethod
    public IRubyObject push_json(ThreadContext context, IRubyObject value) {
        RubyString string = (RubyString) TypeConverter.checkStringType(context.runtime, value);
        push_json(context, sw, string.getByteList(), null);
        return context.nil;
    }

    @JRubyMethod
    public IRubyObject push_json(ThreadContext context, IRubyObject value, IRubyObject key) {
        RubyString string = (RubyString) TypeConverter.checkStringType(context.runtime, value);

        if (context.nil == key) {
            push_json(context, sw, string.getByteList(), null);
        } else {
            RubyString keyString = (RubyString) TypeConverter.checkStringType(context.runtime, key);
            push_json(context, sw, string.getByteList(), keyString.getByteList());
        }
        return context.nil;
    }


    /* call-seq: pop()
     *
     * Pops up a level in the JSON document closing the array or object that is
     * currently open.
     */
    @JRubyMethod
    public IRubyObject pop(ThreadContext context) {
        pop(context, sw);
        return context.nil;
    }

    /* call-seq: pop_all()
     *
     * Pops all level in the JSON document closing all the array or object that is
     * currently open.
     */
    @JRubyMethod
    public IRubyObject pop_all(ThreadContext context) {
        pop_all(context, sw);
        return context.nil;
    }

    /* call-seq: reset()
     *
     * Reset the writer back to the empty state.
     */
    @JRubyMethod
    public IRubyObject reset(ThreadContext context) {
        sw.depth = 0;
        sw.types.removeAllElements();
        sw.keyWritten = false;
        sw.out.reset();

        return context.nil;
    }

    /* call-seq: to_s()
     *
     * Returns the JSON document string in what ever state the construction is at.
     */
    @JRubyMethod
    public IRubyObject to_s(ThreadContext context) {
        RubyString rstr = sw.out.asString(context);

        rstr.setEncoding(UTF8Encoding.INSTANCE);

        return rstr;
    }


    void key_check(ThreadContext context, StrWriter sw, ByteList key) {
        DumpType type = sw.peekTypes();

        if (null == key && (ObjectNew == type || ObjectType == type)) {
            throw context.runtime.newStandardError("Can not push onto an Object without a key.");
        }
    }

    void push_type(ThreadContext context, StrWriter sw, DumpType type) {
        sw.types.push(type);
    }

    void maybe_comma(StrWriter sw) {
        DumpType type = sw.peekTypes();
        if (type == ObjectNew) {
            sw.types.set(sw.types.size() - 1, ObjectType);
        } else if (type == ArrayNew) {
            sw.types.set(sw.types.size() - 1, ArrayType);
        } else if (type == ObjectType || type == ArrayType) {
            // Always have a few characters available in the out.buf.
            sw.out.append(',');
        }
    }

    void push_key(ThreadContext context, StrWriter sw, ByteList key) {
        DumpType type = sw.peekTypes();

        if (sw.keyWritten) {
            throw context.runtime.newStandardError("Can not push more than one key before pushing a non-key.");
        }
        if (ObjectNew != type && ObjectType != type) {
            throw context.runtime.newStandardError("Can only push a key onto an Object.");
        }

        maybe_comma(sw);
        if (!sw.types.empty()) dump.fill_indent(sw.types.size());

        dump.dump_cstr(key, false, false);
        sw.out.append(':');
        sw.keyWritten = true;
    }

    void push_object(ThreadContext context, StrWriter sw, ByteList key) {
        dump_key(context, sw, key);
        sw.out.append('{');
        push_type(context, sw, ObjectNew);
    }

    void push_array(ThreadContext context, StrWriter sw, ByteList key) {
        dump_key(context, sw, key);
        sw.out.append('[');
        push_type(context, sw, ArrayNew);
    }

    void push_value(ThreadContext context, StrWriter sw, IRubyObject val, ByteList key) {
        dump_key(context, sw, key);
        dump.dump_val(val, sw.types.size(), null);
    }

    void push_json(ThreadContext context, StrWriter sw, ByteList json, ByteList key) {
        dump_key(context, sw, key);
        dump.dump_raw(json);
    }

    void dump_key(ThreadContext context, StrWriter sw, ByteList key) {
        if (sw.keyWritten) {
            sw.keyWritten = false;
        } else {
            key_check(context, sw, key);
            maybe_comma(sw);
            if (!sw.types.empty()) {
                dump.fill_indent(sw.types.size());
            }
            if (null != key) {
                dump.dump_cstr(key, false, false);
                sw.out.append(':');
            }
        }
    }

    void pop(ThreadContext context, StrWriter sw) {
        if (sw.keyWritten) {
            sw.keyWritten = false;
            throw context.runtime.newStandardError("Can not pop after writing a key but no value.");
        }

        if (sw.types.empty()) {
            throw context.runtime.newStandardError("Can not pop with no open array or object.");
        }

        DumpType type = sw.types.pop();

        dump.fill_indent(sw.types.size());
        if (type == ObjectNew || type == ObjectType) {
            sw.out.append('}');
        } else if (type == ArrayNew || type == ArrayType) {
            sw.out.append(']');
        }
        if (sw.types.empty() && 0 <= sw.out.indent) {
            sw.out.append('\n');
        }
    }

    void pop_all(ThreadContext context, StrWriter sw) {
        while (!sw.types.empty()) {
            pop(context, sw);
        }
    }
}
