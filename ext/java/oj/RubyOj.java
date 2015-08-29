package oj;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import oj.handlers.AddPICall;
import oj.handlers.ArrayAppendPICall;
import oj.handlers.DispatchAddPICall;
import oj.handlers.DispatchArrayAppendPICall;
import oj.handlers.DispatchHashSetPICall;
import oj.handlers.DispatchPICall;
import oj.handlers.HashSetPICall;
import oj.handlers.NoopAddPICall;
import oj.handlers.NoopArrayAppendPICall;
import oj.handlers.NoopHashSetPICall;
import oj.handlers.NoopPICall;
import oj.handlers.PICall;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyFile;
import org.jruby.RubyFixnum;
import org.jruby.RubyHash;
import org.jruby.RubyModule;
import org.jruby.RubyNumeric;
import org.jruby.RubyString;
import org.jruby.RubySymbol;
import org.jruby.anno.JRubyMethod;
import org.jruby.anno.JRubyModule;
import org.jruby.platform.Platform;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.TypeConverter;

import static oj.Options.*;

/**
 * Created by enebo on 8/25/15.
 */
@JRubyModule(name = "Oj")
public class RubyOj extends RubyModule {
    public static final int MAX_ODD_ARGS = 10;

    public RubyOj(Ruby runtime) {
        super(runtime);
    }

    private static OjLibrary resolveOj(IRubyObject self) {
        return (OjLibrary) self.getInternalVariables().getInternalVariable("_oj");
    }
    
    @JRubyMethod(module = true)
    public static IRubyObject default_options(ThreadContext context, IRubyObject self) {
        OjLibrary oj = resolveOj(self);
        Ruby runtime = context.runtime;
        RubyHash opts = RubyHash.newHash(runtime);
        IRubyObject Qtrue = runtime.getTrue();
        IRubyObject Qfalse = runtime.getFalse();
        IRubyObject Qnil = runtime.getNil();

        opts.fastASet(runtime.newSymbol("indent"), runtime.newFixnum(oj.default_options.indent));
        opts.fastASet(runtime.newSymbol("sec_prec"), runtime.newFixnum(oj.default_options.sec_prec));
        opts.fastASet(runtime.newSymbol("circular"), (Yes == oj.default_options.circular) ? Qtrue : ((No == oj.default_options.circular) ? Qfalse : Qnil));
        opts.fastASet(runtime.newSymbol("class_cache"), (Yes == oj.default_options.class_cache) ? Qtrue : ((No == oj.default_options.class_cache) ? Qfalse : Qnil));
        opts.fastASet(runtime.newSymbol("auto_define"), (Yes == oj.default_options.auto_define) ? Qtrue : ((No == oj.default_options.auto_define) ? Qfalse : Qnil));
        opts.fastASet(runtime.newSymbol("symbol_keys"), (Yes == oj.default_options.sym_key) ? Qtrue : ((No == oj.default_options.sym_key) ? Qfalse : Qnil));
        opts.fastASet(runtime.newSymbol("bigdecimal_as_decimal"), (Yes == oj.default_options.bigdec_as_num) ? Qtrue : ((No == oj.default_options.bigdec_as_num) ? Qfalse : Qnil));
        opts.fastASet(runtime.newSymbol("use_to_json"), (Yes == oj.default_options.to_json) ? Qtrue : ((No == oj.default_options.to_json) ? Qfalse : Qnil));
        opts.fastASet(runtime.newSymbol("nilnil"), (Yes == oj.default_options.nilnil) ? Qtrue : ((No == oj.default_options.nilnil) ? Qfalse : Qnil));
        opts.fastASet(runtime.newSymbol("allow_gc"), (Yes == oj.default_options.allow_gc) ? Qtrue : ((No == oj.default_options.allow_gc) ? Qfalse : Qnil));
        opts.fastASet(runtime.newSymbol("quirks_mode"), (Yes == oj.default_options.quirks_mode) ? Qtrue : ((No == oj.default_options.quirks_mode) ? Qfalse : Qnil));
        opts.fastASet(runtime.newSymbol("float_prec"), runtime.newFixnum(oj.default_options.float_prec));

        switch (oj.default_options.mode) {
            case StrictMode:
                opts.fastASet(runtime.newSymbol("mode"), runtime.newSymbol("strict"));
                break;
            case CompatMode:
                opts.fastASet(runtime.newSymbol("mode"), runtime.newSymbol("compat"));
                break;
            case NullMode:
                opts.fastASet(runtime.newSymbol("mode"), runtime.newSymbol("null"));
                break;
            case ObjectMode:
            default:
                opts.fastASet(runtime.newSymbol("mode"), runtime.newSymbol("object"));
                break;
        }
        switch (oj.default_options.escape_mode) {
            case NLEsc:
                opts.fastASet(runtime.newSymbol("escape_mode"), runtime.newSymbol("newline"));
                break;
            case JSONEsc:
                opts.fastASet(runtime.newSymbol("escape_mode"), runtime.newSymbol("json"));
                break;
            case XSSEsc:
                opts.fastASet(runtime.newSymbol("escape_mode"), runtime.newSymbol("xss_safe"));
                break;
            case ASCIIEsc:
                opts.fastASet(runtime.newSymbol("escape_mode"), runtime.newSymbol("ascii"));
                break;
            default:
                opts.fastASet(runtime.newSymbol("escape_mode"), runtime.newSymbol("json"));
                break;
        }
        switch (oj.default_options.time_format) {
            case XmlTime:
                opts.fastASet(runtime.newSymbol("time_format"), runtime.newSymbol("xmlschema"));
                break;
            case RubyTime:
                opts.fastASet(runtime.newSymbol("time_format"), runtime.newSymbol("ruby"));
                break;
            case UnixZTime:
                opts.fastASet(runtime.newSymbol("time_format"), runtime.newSymbol("unix_zone"));
                break;
            case UnixTime:
            default:
                opts.fastASet(runtime.newSymbol("time_format"), runtime.newSymbol("unix"));
                break;
        }
        switch (oj.default_options.bigdec_load) {
            case BigDec:
                opts.fastASet(runtime.newSymbol("bigdecimal_load"), runtime.newSymbol("bigdecimal"));
                break;
            case FloatDec:
                opts.fastASet(runtime.newSymbol("bigdecimal_load"), runtime.newSymbol("float"));
                break;
            case AutoDec:
            default:
                opts.fastASet(runtime.newSymbol("bigdecimal_load"), runtime.newSymbol("auto"));
                break;
        }
        opts.fastASet(runtime.newSymbol("create_id"), (null == oj.default_options.create_id) ? Qnil : runtime.newString(oj.default_options.create_id));

        return opts;
    }

    @JRubyMethod(name = "default_options=", module = true)
    public static IRubyObject set_def_opts(ThreadContext context, IRubyObject self, IRubyObject roptsArg) {
        OjLibrary oj = resolveOj(self);

        if (roptsArg instanceof RubyHash) oj_parse_options(context, roptsArg, oj.default_options);

        return context.nil;
    }

    public static void oj_parse_options(ThreadContext context, IRubyObject roptsArg, Options copts) {
        if (!(roptsArg instanceof RubyHash)) {
            // FIXME: I think this should be a raise.
            return;
        }
        RubyHash ropts = (RubyHash) roptsArg;
        Ruby runtime = context.runtime;
        IRubyObject Qtrue = runtime.getTrue();
        IRubyObject Qfalse = runtime.getFalse();
        IRubyObject Qnil = runtime.getNil();
        IRubyObject v;

        if (Qnil != (v = ropts.fastARef(runtime.newSymbol("indent")))) {
            if (!(v instanceof RubyFixnum)) {
                throw runtime.newArgumentError(":indent must be a Fixnum.");
            }
            copts.indent = RubyNumeric.num2int(v);
        }

        if (Qnil != (v = ropts.fastARef(runtime.newSymbol("float_prec")))) {
            if (!(v instanceof RubyFixnum)) {
                throw runtime.newArgumentError(":float_precision must be a Fixnum.");
            }

            int n = (int) ((RubyFixnum) v).getLongValue();
            if (0 >= n) {
                copts.float_fmt = "";
                copts.float_prec = 0;
            } else {
                if (20 < n) {
                    n = 20;
                }
                // FIXME:
                //sprintf(copts.float_fmt, "%%0.%dg", n);
                copts.float_prec = (char) n;
            }
        }

        if (Qnil != (v = ropts.fastARef(runtime.newSymbol("sec_prec")))) {
            if (!(v instanceof RubyFixnum)) {
                throw runtime.newArgumentError(":second_precision must be a Fixnum.");
            }
            int n = RubyNumeric.num2int(v);
            if (0 > n) {
                n = 0;
            } else if (9 < n) {
                n = 9;
            }
            copts.sec_prec = n;
        }

        if (Qnil != (v = ropts.fastARef(runtime.newSymbol("mode")))) {
            if (runtime.newSymbol("object") == v) {
                copts.mode = ObjectMode;
            } else if (runtime.newSymbol("strict") == v) {
                copts.mode = StrictMode;
            } else if (runtime.newSymbol("compat") == v) {
                copts.mode = CompatMode;
            } else if (runtime.newSymbol("null") == v) {
                copts.mode = NullMode;
            } else {
                throw runtime.newArgumentError(":mode must be :object, :strict, :compat, or :null.");
            }
        }
        if (Qnil != (v = ropts.fastARef(runtime.newSymbol("time_format")))) {
            if (runtime.newSymbol("unix") == v) {
                copts.time_format = UnixTime;
            } else if (runtime.newSymbol("unix_zone") == v) {
                copts.time_format = UnixZTime;
            } else if (runtime.newSymbol("xmlschema") == v) {
                copts.time_format = XmlTime;
            } else if (runtime.newSymbol("ruby") == v) {
                copts.time_format = RubyTime;
            } else {
                throw runtime.newArgumentError(":time_format must be :unix, :unix_zone, :xmlschema, or :ruby.");
            }
        }
        if (Qnil != (v = ropts.fastARef(runtime.newSymbol("escape_mode")))) {
            if (runtime.newSymbol("newline") == v) {
                copts.escape_mode = NLEsc;
            } else if (runtime.newSymbol("json") == v) {
                copts.escape_mode = JSONEsc;
            } else if (runtime.newSymbol("xss_safe") == v) {
                copts.escape_mode = XSSEsc;
            } else if (runtime.newSymbol("ascii") == v) {
                copts.escape_mode = ASCIIEsc;
            } else {
                throw runtime.newArgumentError(":encoding must be :newline, :json, :xss_safe, or :ascii.");
            }
        }
        if (Qnil != (v = ropts.fastARef(runtime.newSymbol("bigdecimal_load")))) {
            if (runtime.newSymbol("bigdecimal") == v || Qtrue == v) {
                copts.bigdec_load = BigDec;
            } else if (runtime.newSymbol("float") == v) {
                copts.bigdec_load = FloatDec;
            } else if (runtime.newSymbol("auto") == v || Qfalse == v) {
                copts.bigdec_load = AutoDec;
            } else {
                throw runtime.newArgumentError(":bigdecimal_load must be :bigdecimal, :float, or :auto.");
            }
        }
        if (Qtrue == ropts.callMethod(context, "has_key?", runtime.newSymbol("create_id"))) {
            v = ropts.fastARef(runtime.newSymbol("create_id"));
            if (Qnil == v) {
                copts.create_id = null;
            } else if (v instanceof RubyString) {
                String str = v.asJavaString();

                if (!copts.create_id.equals(str)) {
                    copts.create_id = str;
                }
            } else {
                throw runtime.newArgumentError(":create_id must be string.");
            }
        }

        // FIXME:
        /*
        for (o = ynos; 0 != o.attr; o++) {
            if (Qnil != (v = ropts.fastARef(o.sym))) {
                if (Qtrue == v) {
                    o.attr = Yes;
                } else if (Qfalse == v) {
                    o.attr = No;
                } else {
                    throw runtime.newArgumentError("" + o.sym +  "must be true or false.");
                }
            }
        }*/
        // This is here only for backwards compatibility with the original Oj.
        v = ropts.fastARef(runtime.newSymbol("ascii_only"));
        if (Qtrue == v) {
            copts.escape_mode = ASCIIEsc;
        } else if (Qfalse == v) {
            copts.escape_mode = JSONEsc;
        }
    }

    @JRubyMethod(module = true)
    public static IRubyObject mimic_JSON(ThreadContext context, IRubyObject self) {
        // FIXME:
        return null;
    }

    @JRubyMethod(module = true, rest = true)
    public static IRubyObject load(ThreadContext context, IRubyObject self, IRubyObject[] args) {
        OjLibrary oj = resolveOj(self);
        Ruby runtime = context.runtime;
        char mode = oj.default_options.mode;

        if (1 > args.length) {
            throw context.runtime.newArgumentError("Wrong number of arguments to load().");
        }
        if (2 <= args.length) {
            if (!(args[1] instanceof RubyHash)) {
                throw context.runtime.newArgumentError("options must be a hash.");
            }

            RubyHash	ropts = (RubyHash) args[1];
            IRubyObject	v;
            IRubyObject Qnil = runtime.getNil();

            if (Qnil != (v = ropts.fastARef(runtime.newSymbol("mode")))) {
                if (runtime.newSymbol("object") == v) {
                    mode = ObjectMode;
                } else if (runtime.newSymbol("strict") == v) {
                    mode = StrictMode;
                } else if (runtime.newSymbol("compat") == v) {
                    mode = CompatMode;
                } else if (runtime.newSymbol("null") == v) {
                    mode = NullMode;
                } else {
                    throw runtime.newArgumentError(":mode must be :object, :strict, :compat, or :null.");
                }
            }
        }
        // FIXME:
        /*
        switch (mode) {
            case StrictMode:
                return oj_strict_parse(args, self);
            case NullMode:
            case CompatMode:
                return oj_compat_parse(args, self);
            case ObjectMode:
            default:
                break;
        }
        return oj_object_parse(argc, argv, self);
        */
        return null;
    }


 /*
    @JRubyMethod(module = true, rest = true)
    public static IRubyObject load_file(ThreadContext context, IRubyObject self, IRubyObject[] args) {
        OjLibrary oj = resolveOj(self);
        Ruby runtime = context.runtime;
        char		mode = oj.default_options.mode;
        ParseInfo	pi = new ParseInfo(context);

        if (1 > args.length) {
            throw runtime.newArgumentError("Wrong number of arguments to load().");
        }

        // FIXME: This should be Check_Type equivalent but 1.7 does not have it directly.
        if (!(args[0] instanceof RubyString)) {
            throw runtime.newArgumentError("Expected String");
        }

        IRubyObject Qnil = runtime.getNil();
        pi.options = oj.default_options;
        pi.handler = Qnil;

        if (2 <= args.length) {
            RubyHash	ropts = (RubyHash) args[1];
            IRubyObject	v;

            if (Qnil != (v = ropts.fastARef(runtime.newSymbol("mode")))) {
                if (runtime.newSymbol("object") == v) {
                    mode = ObjectMode;
                } else if (runtime.newSymbol("strict") == v) {
                    mode = StrictMode;
                } else if (runtime.newSymbol("compat") == v) {
                    mode = CompatMode;
                } else if (runtime.newSymbol("null") == v) {
                    mode = NullMode;
                } else {
                    throw runtime.newArgumentError(":mode must be :object, :strict, :compat, or :null.");
                }
            }
        }
        String path = args[0].asJavaString();

        InputStream fd = null;
        try {
            fd = new FileInputStream(path);

            switch (mode) {
                case StrictMode:
                    Strict.oj_set_strict_callbacks(pi, oj);
                    return oj_pi_sparse(args, pi, fd);
                case NullMode:
                case CompatMode:
                    Compat.oj_set_compat_callbacks(pi, oj);
                    return oj_pi_sparse(args, pi, fd);
                case ObjectMode:
                default:
                    break;
            }
            Object.oj_set_object_callbacks(pi, oj);

            return oj_pi_sparse(args, pi, fd);
        } catch (IOException e) {
            throw runtime.newIOError(e.getMessage());
        } finally {
            if (fd != null) {
                try { fd.close(); } catch (IOException e) {}
            }
        }
    }
    */

    @JRubyMethod(module = true)
    public static IRubyObject safe_load(ThreadContext context, IRubyObject self, IRubyObject doc, Block block) {
        OjLibrary oj = resolveOj(self);
        ParseInfo pi = new ParseInfo(context);

        pi.options = oj.default_options;
        pi.options.auto_define = No;
        pi.options.sym_key = No;
        pi.options.mode = StrictMode;
        Strict.oj_set_strict_callbacks(pi, oj);

        return Parse.oj_pi_parse(context, new IRubyObject[] { doc }, pi, null, 0, true, block);
    }

    /*
    @JRubyMethod(alias = {"compat_load", "object_load"}, module = true, required = 1, rest = true)
    public static IRubyObject strict_load(ThreadContext context, IRubyObject self, IRubyObject[] args, Block block) {
        OjLibrary oj = resolveOj(self);
        ParseInfo pi = new ParseInfo(context);

        pi.options = oj.default_options;
        pi.handler = context.nil;
        Strict.oj_set_strict_callbacks(pi, oj);

        if (args[0] instanceof RubyString) {
            return Parse.oj_pi_parse(context, args, pi, null, 0, true, block);
        } else {
            return oj_pi_sparse(args, pi, 0);
        }
    }*/


// FIXME:
    /*

    @JRubyMethod(module = true, rest = true)
    public static IRubyObject dump(ThreadContext context, IRubyObject self, IRubyObject[] args) {
        OjLibrary oj = resolveOj(self);
        ByteList		buf = new ByteList();
        Out		out;
        Options	copts = oj.default_options;
        IRubyObject		rstr;

        if (2 == args.length) {
            oj_parse_options(context, args[1], copts);
        }
        out.buf = buf;
        out.end = buf + sizeof(buf) - 10;
        out.allocated = 0;
        oj_dump_obj_to_json(args[0], copts, out);
        if (0 == out.buf) {
            rb_raise(rb_eNoMemError, "Not enough memory.");
        }

        return oj_encode(context.runtime.newString(out.buf));
    }
    */

    /*
    @JRubyMethod(module = true, required=2, rest = true)
    public static IRubyObject to_file(ThreadContext context, IRubyObject self, IRubyObject[] args) {
        OjLibrary oj = resolveOj(self);
        Options copts = oj.default_options;

        if (3 == args.length) {
            oj_parse_options(context, args[2], copts);
        }

        TypeConverter.checkStringType(context.runtime, args[0]);
        oj_write_obj_to_file(args[1], args[0].asJavaString(), copts);

        return context.nil;
    }*/

    /*
    @JRubyMethod(module = true, rest = true)
    public static IRubyObject to_stream(ThreadContext context, IRubyObject self, IRubyObject[] args) {
        OjLibrary oj = resolveOj(self);
        Options copts = oj.default_options;

        if (3 == args.length) {
            oj_parse_options(context, args[2], oj.default_options);
        }
        oj_write_obj_to_stream(args[1], args, copts);

        return context.nil;
    }
    */

    /*
    @JRubyMethod(module = true, rest = true)
    public static IRubyObject register_odd(ThreadContext context, IRubyObject self, IRubyObject[] args) {
        OjLibrary oj = resolveOj(self);
        if (3 > args.length) {
            throw context.runtime.newArgumentError("incorrect number of arguments.");
        }

        if (!(args[0] instanceof RubyClass)) {
            throw context.runtime.newArgumentError("expecting class.");
        }
        if (!(args[0] instanceof RubySymbol)) {
            throw context.runtime.newArgumentError("expecting symbol.");
        }

        if (MAX_ODD_ARGS < args.length - 2) {
            throw context.runtime.newArgumentError("too many members.");
        }

        IRubyObject[] newArgs = new IRubyObject[args.length - 3];
        System.arraycopy(args, 3, newArgs, 0, newArgs.length);

        oj_reg_odd(args[0], args[1], args[2], newArgs);

        return context.nil;
    }
    */

    /*
    @JRubyMethod(module = true, rest = true)
    public static IRubyObject saj_parse(ThreadContext context, IRubyObject self, IRubyObject[] args) {
        Ruby runtime = context.runtime;
        ByteList json = null;
        IRubyObject	input = args[1];


        if (args.length < 2) {
            throw runtime.newArgumentError("Wrong number of arguments to saj_parse.\n");
        }
        if (input instanceof RubyString) {
            json = ((RubyString) input).getByteList();
        } else {
            json = getInput(context, input);
        }
        Saj.parse(args, json);

        return context.nil;
    }
    */

    @JRubyMethod(module = true, rest = true)
    public static IRubyObject sc_parse(ThreadContext context, IRubyObject self, IRubyObject[] args, Block block) {
        OjLibrary oj = resolveOj(self);
        ParseInfo pi = new ParseInfo(context);
        IRubyObject input = args[1];

        pi.options = oj.default_options;
        if (3 == args.length) {
            oj_parse_options(context, args[2], pi.options);
        }

        pi.proc = block;
        pi.handler = args[0];

        pi.start_hash = pi.handler.respondsTo("hash_start") ? oj.startHashDispatch : oj.noopPICall;
        pi.end_hash = pi.handler.respondsTo("hash_end") ? oj.endHashDispatch : oj.noopPICall;
        pi.hash_key = pi.handler.respondsTo("hash_key") ? oj.hashKeyDispatch : oj.noopPICall;
        pi.start_array = pi.handler.respondsTo("array_start") ? oj.startArrayDispatch : oj.noopPICall;
        pi.end_array = pi.handler.respondsTo("array_end") ? oj.endArrayDispatch : oj.noopPICall;
        if (pi.handler.respondsTo("hash_set")) {
            pi.hash_set = oj.scpHashSetDispatch;
            pi.expect_value = true;
        } else {
            pi.hash_set = oj.hashSetNoop;
            pi.expect_value = false;
        }
        if (pi.handler.respondsTo("array_append")) {
            pi.array_append = oj.scpArrayAppendDispatch;
            pi.expect_value = true;
        } else {
            pi.array_append = oj.arrayAppendNoop;
            pi.expect_value = false;
        }
        if (pi.handler.respondsTo("add_value")) {
            pi.add = oj.scpAddDispatch;
            pi.expect_value = true;
        } else {
            pi.add = oj.noopAdd;
            pi.expect_value = false;
        }

        IRubyObject[] newArgs = new IRubyObject[args.length - 1];
        System.arraycopy(args, 1, newArgs, 0, newArgs.length);
        if (input instanceof RubyString) {
            return Parse.oj_pi_parse(context, newArgs, pi, null, 0, true, block);
        } else {
            // FIXME:
//            return oj_pi_sparse(newArgs, pi, 0);
        }
        return null;
    }

    public static ByteList getInput(ThreadContext context, IRubyObject input) {
        Ruby runtime = context.runtime;
        RubyModule clas = input.getMetaClass();

        if (runtime.getClass("StringIO") == clas) {
            input = input.callMethod(context, "string");
        } else if (!Platform.IS_WINDOWS && runtime.getFile() == clas && 0 == input.callMethod(context, "pos").convertToInteger().getLongValue()) {
            input = ((RubyFile) input).read(context);
        } else if (input.respondsTo("read")) {
            input = input.callMethod(context, "read");
        } else {
            throw runtime.newArgumentError("saj_parse() expected a String or IO Object.");
        }

        if (!(input instanceof RubyString)) {
            throw runtime.newArgumentError("Especting a string.");
        }

        return ((RubyString) input).getByteList();
    }
}
