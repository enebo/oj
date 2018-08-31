package oj;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyNumeric;
import org.jruby.RubyObject;
import org.jruby.RubyString;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.TypeConverter;

import static oj.StreamWriterType.*;

/**
 * Document-class: Oj::StreamWriter
 *
 * Supports building a JSON document one element at a time. Build the IO stream
 * document by pushing values into the document. Pushing an array or an object
 * will create that element in the JSON document and subsequent pushes will add
 * the elements to that array or object until a pop() is called.
 */
public class StreamWriter extends RubyObject {
    StringWriter sw;
    StreamWriterType type = STREAM_IO;
    IRubyObject stream;
    int flush_limit;

    public static void createStreamWriterClass(Ruby runtime, RubyModule oj) {
        RubyClass clazz = oj.defineClassUnder("StreamWriter", runtime.getObject(), ALLOCATOR);
        clazz.defineAnnotatedMethods(StreamWriter.class);
    }

    private static ObjectAllocator ALLOCATOR = new ObjectAllocator() {
        @Override
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new StreamWriter(runtime, klass);
        }
    };

    public StreamWriter(Ruby runtime, RubyClass metaClass) {
        super(runtime, metaClass);
    }

    void write(ThreadContext context) {
        RubyString string = sw.dump.out.asString(context);

        if (type == FILE_IO) {
            int size = string.getByteList().realSize();

            // Note(s): 1. fixnum is immediate so == is ok. 2. this is doing dyndispatch where as C is direct fd write
            // FIXME: Make this direct call
            if (size != RubyNumeric.fix2int(stream.callMethod(context, "write", string))) {
                throw context.runtime.newIOError("Write failed.");// [%d:%s]\n", errno, strerror(errno));
            }
        } else {
            stream.callMethod(context, "write", string);
        }
    }

    void reset_buf() {
        sw.dump.out.buf.setRealSize(0);
    }

    /* call-seq: new(io, options)
     *
     * Creates a new StreamWriter.
     * @param [IO] io stream to write to
     * @param [Hash] options formating options
     */

    @JRubyMethod(required = 1, optional = 1)
    public IRubyObject initialize(ThreadContext context, IRubyObject[] argv) {
        stream = argv[0];
        IRubyObject s;

        if (context.runtime.getObject().getConstantAt("StringIO") == stream.getMetaClass()) {
            type = STRING_IO;
        } else if (stream.respondsTo("fileno") &&
                context.nil != (s = stream.callMethod(context, "fileno")) &&
                0 != RubyNumeric.fix2int(s)) {
            type = FILE_IO;
        } else if (stream.respondsTo("write")) {
            type = STREAM_IO;
        } else {
            throw context.runtime.newArgumentError("expected an IO Object.");
        }

        sw = new StringWriter(context.runtime, RubyOj.oj(context).stringWriter);
        sw.initialize(context, IRubyObject.NULL_ARRAY);

        if (2 == argv.length) {
            RubyOj.parse_options(context, argv[1], sw.opts);
        }


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
    IRubyObject push_key(ThreadContext context, IRubyObject keyArg) {
        IRubyObject key = TypeConverter.checkStringType(context.runtime, keyArg);
        reset_buf();
        sw.push_key(context, key);
        write(context);

        return context.nil;
    }

    /* call-seq: push_object(key=nil)
     *
     * Pushes an object onto the JSON document. Future pushes will be to this object
     * until a pop() is called.
     * @param [String] key the key if adding to an object in the JSON document
     */
    @JRubyMethod
    IRubyObject push_object(ThreadContext context) {
        reset_buf();
        sw.push_object(context, Block.NULL_BLOCK);
        write(context);
        return context.nil;
    }

    @JRubyMethod
    IRubyObject push_object(ThreadContext context, IRubyObject key) {
        reset_buf();

        if (context.nil == key) return push_object(context);

        sw.push_object(context, TypeConverter.checkStringType(context.runtime, key), Block.NULL_BLOCK);
        write(context);
        return context.nil;
    }

    /* call-seq: push_array(key=nil)
     *
     * Pushes an array onto the JSON document. Future pushes will be to this object
     * until a pop() is called.
     * @param [String] key the key if adding to an object in the JSON document
     */
    @JRubyMethod
    IRubyObject push_array(ThreadContext context) {
        reset_buf();
        sw.push_array(context, Block.NULL_BLOCK);
        write(context);
        return context.nil;
    }

    @JRubyMethod
    IRubyObject push_array(ThreadContext context, IRubyObject key) {
        reset_buf();

        if (context.nil == key) return push_array(context);

        sw.push_array(context, TypeConverter.checkStringType(context.runtime, key), Block.NULL_BLOCK);
        write(context);
        return context.nil;
    }

    /* call-seq: push_value(value, key=nil)
     *
     * Pushes a value onto the JSON document.
     * @param [Object] value value to add to the JSON document
     * @param [String] key the key if adding to an object in the JSON document
     */
    @JRubyMethod
    IRubyObject push_value(ThreadContext context, IRubyObject value) {
        reset_buf();
        sw.push_value(context, value);
        write(context);
        return context.nil;
    }

    @JRubyMethod
    IRubyObject push_value(ThreadContext context, IRubyObject value, IRubyObject key) {
        reset_buf();

        if (context.nil == key) {
            sw.push_value(context, value);
        } else {
            sw.push_value(context, value, TypeConverter.checkStringType(context.runtime, key));
        }

        write(context);
        return context.nil;
    }

    /* call-seq: push_json(value, key=nil)
     *
     * Pushes a string onto the JSON document. The String must be a valid JSON
     * encoded string. No additional checking is done to verify the validity of the
     * string.
     * @param [Object] value value to add to the JSON document
     * @param [String] key the key if adding to an object in the JSON document
     */
    @JRubyMethod
    IRubyObject push_json(ThreadContext context, IRubyObject value) {
        reset_buf();
        sw.push_json(context, TypeConverter.checkStringType(context.runtime, value));
        write(context);
        return context.nil;
    }

    @JRubyMethod
    IRubyObject push_json(ThreadContext context, IRubyObject valueArg, IRubyObject key) {
        reset_buf();

        IRubyObject value = TypeConverter.checkStringType(context.runtime, valueArg);
        if (context.nil == key) {
            sw.push_json(context, value);
        } else {
            sw.push_json(context, value, TypeConverter.checkStringType(context.runtime, key));
        }

        write(context);
        return context.nil;
    }

    /* call-seq: pop()
     *
     * Pops up a level in the JSON document closing the array or object that is
     * currently open.
     */
    @JRubyMethod
    IRubyObject pop(ThreadContext context) {
        reset_buf();
        sw.pop(context);
        write(context);
        return context.nil;
    }

    /* call-seq: pop_all()
     *
     * Pops all level in the JSON document closing all the array or object that is
     * currently open.
     */
    @JRubyMethod
    IRubyObject pop_all(ThreadContext context) {
        reset_buf();
        sw.pop_all(context);
        write(context);

        return context.nil;
    }    
}