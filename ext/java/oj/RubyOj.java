package oj;

import oj.options.DumpCaller;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.Ruby;
import org.jruby.RubyArray;
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
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.TypeConverter;

import java.util.ArrayList;

import static oj.Options.*;
import static oj.options.NanDump.*;

// FIXME: I think all default_options need to clone for every parse.

/**
 * Created by enebo on 8/25/15.
 */
@JRubyModule(name = "Oj")
public class RubyOj extends RubyModule {
    public static final int MAX_ODD_ARGS = 10;

    public RubyOj(Ruby runtime) {
        super(runtime);
    }

    public static OjLibrary oj(ThreadContext context) {
        return resolveOj(context.runtime.getObject().getConstantAt("Oj"));
    }

    public static OjLibrary resolveOj(IRubyObject self) {
        return (OjLibrary) self.getInternalVariables().getInternalVariable("_oj");
    }

    private static IRubyObject yesNo(ThreadContext context, char option) {
        return (Yes == option) ? context.tru : ((No == option) ? context.fals : context.nil);
    }

    @JRubyMethod(module = true)
    public static IRubyObject default_options(ThreadContext context, IRubyObject self) {
        OjLibrary oj = resolveOj(self);
        Ruby runtime = context.runtime;
        RubyHash opts = RubyHash.newHash(runtime);
        IRubyObject Qnil = context.nil;

        if (oj.default_options.dump_opts.indent_str == ByteList.EMPTY_BYTELIST) {
            opts.fastASet(runtime.newSymbol("indent"), runtime.newFixnum(oj.default_options.indent));
        } else {
            opts.fastASet(runtime.newSymbol("indent"), runtime.newString(oj.default_options.dump_opts.indent_str.dup()));
        }
        opts.fastASet(runtime.newSymbol("second_precision"), runtime.newFixnum(oj.default_options.sec_prec));
        opts.fastASet(runtime.newSymbol("circular"), yesNo(context, oj.default_options.circular));
        opts.fastASet(runtime.newSymbol("class_cache"), yesNo(context, oj.default_options.class_cache));
        opts.fastASet(runtime.newSymbol("auto_define"), yesNo(context, oj.default_options.auto_define));
        opts.fastASet(runtime.newSymbol("symbol_keys"), yesNo(context, oj.default_options.sym_key));
        opts.fastASet(runtime.newSymbol("bigdecimal_as_decimal"), yesNo(context, oj.default_options.bigdec_as_num));
        opts.fastASet(runtime.newSymbol("create_additions"), yesNo(context, oj.default_options.create_ok));
        opts.fastASet(runtime.newSymbol("use_to_json"), yesNo(context, oj.default_options.to_json));
        opts.fastASet(runtime.newSymbol("use_to_hash"), yesNo(context, oj.default_options.to_hash));
        opts.fastASet(runtime.newSymbol("use_as_json"), yesNo(context, oj.default_options.as_json));
        opts.fastASet(runtime.newSymbol("nilnil"), yesNo(context, oj.default_options.nilnil));
        opts.fastASet(runtime.newSymbol("empty_string"), yesNo(context, oj.default_options.empty_string));
        opts.fastASet(runtime.newSymbol("allow_gc"), yesNo(context, oj.default_options.allow_gc));
        opts.fastASet(runtime.newSymbol("quirks_mode"), yesNo(context, oj.default_options.quirks_mode));
        opts.fastASet(runtime.newSymbol("allow_invalid_unicode"), yesNo(context, oj.default_options.allow_invalid));
        opts.fastASet(runtime.newSymbol("allow_nan"), yesNo(context, oj.default_options.allow_nan));
        opts.fastASet(runtime.newSymbol("trace"), yesNo(context, oj.default_options.trace));
        opts.fastASet(runtime.newSymbol("float_precision"), runtime.newFixnum(oj.default_options.float_prec));

        RubySymbol mode = runtime.newSymbol("mode");
        switch (oj.default_options.mode) {
            case StrictMode:
                opts.fastASet(mode, runtime.newSymbol("strict")); break;
            case CompatMode:
                opts.fastASet(mode, runtime.newSymbol("compat")); break;
            case NullMode:
                opts.fastASet(mode, runtime.newSymbol("null")); break;
            case CustomMode:
                opts.fastASet(mode, runtime.newSymbol("custom")); break;
            case RailsMode:
                opts.fastASet(mode, runtime.newSymbol("rails")); break;
            case WabMode:
                opts.fastASet(mode, runtime.newSymbol("wab")); break;
            case ObjectMode:
            default:
                opts.fastASet(runtime.newSymbol("mode"), runtime.newSymbol("object"));
                break;
        }
        RubySymbol escapeMode = runtime.newSymbol("escape_mode"); 
        switch (oj.default_options.escape_mode) {
            case NLEsc:
                opts.fastASet(escapeMode, runtime.newSymbol("newline")); break;
            case JSONEsc:
                opts.fastASet(escapeMode, runtime.newSymbol("json")); break;
            case XSSEsc:
                opts.fastASet(escapeMode, runtime.newSymbol("xss_safe")); break;
            case ASCIIEsc:
                opts.fastASet(escapeMode, runtime.newSymbol("ascii")); break;
            case JXEsc:
                opts.fastASet(escapeMode, runtime.newSymbol("unicode_xss")); break;
            default:
                opts.fastASet(escapeMode, runtime.newSymbol("json")); break;
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
        opts.fastASet(runtime.newSymbol("space"), (ByteList.EMPTY_BYTELIST == oj.default_options.dump_opts.after_sep) ? Qnil : runtime.newString(oj.default_options.dump_opts.after_sep));
        opts.fastASet(runtime.newSymbol("space_before"), (ByteList.EMPTY_BYTELIST == oj.default_options.dump_opts.before_sep) ? Qnil : runtime.newString(oj.default_options.dump_opts.before_sep));
        opts.fastASet(runtime.newSymbol("object_nl"), (ByteList.EMPTY_BYTELIST == oj.default_options.dump_opts.hash_nl) ? Qnil : runtime.newString(oj.default_options.dump_opts.hash_nl));
        opts.fastASet(runtime.newSymbol("array_nl"), (ByteList.EMPTY_BYTELIST == oj.default_options.dump_opts.array_nl) ? Qnil : runtime.newString(oj.default_options.dump_opts.array_nl));

        RubySymbol nan = runtime.newSymbol("nan");
        switch (oj.default_options.dump_opts.nan_dump) {
            case NullNan:	opts.fastASet(nan, runtime.newSymbol("null"));	break;
            case RaiseNan:	opts.fastASet(nan, runtime.newSymbol("raise"));	break;
            case WordNan:	opts.fastASet(nan, runtime.newSymbol("word"));	break;
            case HugeNan:	opts.fastASet(nan, runtime.newSymbol("huge"));	break;
            case AutoNan:
            default:		opts.fastASet(nan, runtime.newSymbol("auto"));	break;
        }
        opts.fastASet(runtime.newSymbol("omit_nil"), oj.default_options.dump_opts.omit_nil ? context.tru : context.fals);
        opts.fastASet(runtime.newSymbol("hash_class"), oj.default_options.hash_class);
        opts.fastASet(runtime.newSymbol("array_class"), oj.default_options.array_class);

        if (oj.default_options.ignore == null) {
            opts.fastASet(runtime.newSymbol("ignore"), Qnil);
        } else {
            RubyArray a = runtime.newArray(oj.default_options.ignore.size());

            for (IRubyObject element: oj.default_options.ignore) {
                a.push(element);
            }

            opts.fastASet(runtime.newSymbol("ignore"), a);
        }

        return opts;
    }

    @JRubyMethod(name = "default_options=", module = true)
    public static IRubyObject set_def_opts(ThreadContext context, IRubyObject self, IRubyObject roptsArg) {
        OjLibrary oj = resolveOj(self);

        if (roptsArg instanceof RubyHash) parse_options(context, roptsArg, oj.default_options);

        return context.nil;
    }

    public static void parse_options(ThreadContext context, IRubyObject roptsArg, Options copts) {
        RubyHash ropts = (RubyHash) TypeConverter.checkHashType(context.runtime, roptsArg);
        Ruby runtime = context.runtime;
        IRubyObject Qtrue = runtime.getTrue();
        IRubyObject Qfalse = runtime.getFalse();
        IRubyObject Qnil = runtime.getNil();
        IRubyObject v;

        if (null != (v = ropts.fastARef(runtime.newSymbol("indent")))) {
            if (v.isNil()) {
                copts.indent = 0;
            } else if (v instanceof RubyFixnum) {
                copts.indent = RubyNumeric.num2int(v);
            } else if (v instanceof RubyString) {
                copts.dump_opts.indent_str = ((RubyString) v).getByteList();
                copts.indent = 0;
            } else {
                throw runtime.newArgumentError(":indent_str must be a Fixnum.");
            }
        }

        if (null != (v = ropts.fastARef(runtime.newSymbol("float_precision")))) {
            if (!(v instanceof RubyFixnum)) {
                throw runtime.newArgumentError(":float_precision must be a Fixnum.");
            }

            int n = (int) ((RubyFixnum) v).getLongValue();
            if (0 >= n) {
                copts.float_fmt = "";
                copts.float_prec = 0;
            } else {
                if (20 < n) n = 20;

                copts.float_fmt = "%0." + n + "g";
                copts.float_prec = n;
            }
        }

        if (null != (v = ropts.fastARef(runtime.newSymbol("second_precision")))) {
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

        if (null != (v = ropts.fastARef(runtime.newSymbol("mode")))) {
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
        if (null != (v = ropts.fastARef(runtime.newSymbol("time_format")))) {
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
        if (null != (v = ropts.fastARef(runtime.newSymbol("escape_mode")))) {
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
        if (null != (v = ropts.fastARef(runtime.newSymbol("bigdecimal_load")))) {
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
                ByteList str = ((RubyString) v).getByteList();

                if (!str.equals(copts.create_id)) {
                    copts.create_id = str;
                }
            } else {
                throw runtime.newArgumentError(":create_id must be string.");
            }
        }

        v = ropts.fastARef(runtime.newSymbol("ascii_only"));
        if (Qtrue == v) {
            copts.escape_mode = ASCIIEsc;
        } else if (Qfalse == v) {
            copts.escape_mode = JSONEsc;
        }

        v = ropts.fastARef(runtime.newSymbol("space"));
        if (v != null && !v.isNil()) {
            copts.dump_opts.after_sep = ((RubyString) TypeConverter.checkStringType(runtime, v)).getByteList();
        }

        v = ropts.fastARef(runtime.newSymbol("space_before"));
        if (v != null && !v.isNil()) {
            copts.dump_opts.before_sep = ((RubyString) TypeConverter.checkStringType(runtime, v)).getByteList();
        }

        v = ropts.fastARef(runtime.newSymbol("object_nl"));
        if (v != null && !v.isNil()) {
            copts.dump_opts.hash_nl = ((RubyString) TypeConverter.checkStringType(runtime, v)).getByteList();
        }

        v = ropts.fastARef(runtime.newSymbol("array_nl"));
        if (v != null && !v.isNil()) {
            copts.dump_opts.array_nl = ((RubyString) TypeConverter.checkStringType(runtime, v)).getByteList();
        }

        v = ropts.fastARef(runtime.newSymbol("nan"));
        if (v != null) {
            if (runtime.newSymbol("null") == v) {
                copts.dump_opts.nan_dump = NullNan;
            } else if (runtime.newSymbol("huge") == v) {
                copts.dump_opts.nan_dump = HugeNan;
            } else if (runtime.newSymbol("word") == v) {
                copts.dump_opts.nan_dump = WordNan;
            } else if (runtime.newSymbol("raise") == v) {
                copts.dump_opts.nan_dump = RaiseNan;
            } else if (runtime.newSymbol("auto") == v) {
                copts.dump_opts.nan_dump = AutoNan;
            } else {
                throw runtime.newArgumentError(":nan must be :null, :huge, :word, :raise, or :auto.");
            }
        }

        copts.dump_opts.use = 0 < copts.dump_opts.indent_str.getRealSize() ||
                0 < copts.dump_opts.after_sep.getRealSize() ||
                0 < copts.dump_opts.before_sep.getRealSize() ||
                0 < copts.dump_opts.hash_nl.getRealSize() ||
                0 < copts.dump_opts.array_nl.getRealSize();

        copts.dump_opts.omit_nil = setTrueFalse(context, ropts, "omit_nil");

        copts.circular = setYesNo(context, ropts, "circular");
        copts.auto_define = setYesNo(context, ropts, "auto_define");
        copts.sym_key = setYesNo(context, ropts, "symbol_keys");
        copts.class_cache = setYesNo(context, ropts, "class_cache");
        copts.bigdec_as_num = setYesNo(context, ropts, "bigdecimal_as_decimal");
        copts.to_hash = setYesNo(context, ropts, "use_to_hash");
        copts.to_json = setYesNo(context, ropts, "use_to_json");
        copts.as_json = setYesNo(context, ropts, "use_as_json");
        copts.nilnil = setYesNo(context, ropts, "nilnil");
        copts.nilnil = setYesNo(context, ropts, "allow_blank");
        copts.empty_string = setYesNo(context, ropts, "empty_string");
        copts.allow_gc = setYesNo(context, ropts, "allow_gc");
        copts.quirks_mode = setYesNo(context, ropts, "quirks_mode");
        copts.allow_invalid = setYesNo(context, ropts, "allow_invalid_unicode");
        copts.allow_nan = setYesNo(context, ropts, "allow_nan");
        copts.trace = setYesNo(context, ropts, "trace");
        copts.create_ok = setYesNo(context, ropts, "create_additions");

        v = ropts.fastARef(runtime.newSymbol("ignore"));
        if (v != null && !v.isNil()) {
            RubyArray array = (RubyArray) TypeConverter.checkArrayType(runtime, v);
            int length = array.size();
            copts.ignore = new ArrayList<>(length);
            for (int i = 0; i < length; i++) {
                // FIXME: we don't know what is in this array...check for module/class
                copts.ignore.add((RubyModule) array.eltInternal(i));
            }
        }
    }

    private static char setYesNo(ThreadContext context, RubyHash options, String symbolName) {
        IRubyObject v = options.fastARef(context.runtime.newSymbol(symbolName));
        if (v == context.nil || v == null) return NotSet;
        if (v == context.tru) return Yes;
        if (v == context.fals) return No;

        throw context.runtime.newArgumentError(symbolName + "must be true, false, or nil");
    }

    private static boolean setTrueFalse(ThreadContext context, RubyHash options, String symbolName) {
        IRubyObject v = options.fastARef(context.runtime.newSymbol(symbolName));
        if (v == context.nil || v == null) return false;
        if (v == context.tru) return true;
        if (v == context.fals) return false;

        throw context.runtime.newArgumentError(symbolName + "must be true, false, or nil");
    }


    @JRubyMethod(module = true, rest = true)
    public static IRubyObject mimic_JSON(ThreadContext context, IRubyObject self, IRubyObject[] args) {
        return RubyMimic.defineMimic(context, resolveOj(self), args);
    }

    @JRubyMethod(module = true, rest = true)
    public static IRubyObject load(ThreadContext context, IRubyObject self, IRubyObject[] args, Block block) {
        OjLibrary oj = resolveOj(self);
        Ruby runtime = context.runtime;
        Options options = oj.default_options.dup(context);
        char mode = options.mode;

        if (1 > args.length) {
            throw context.runtime.newArgumentError("Wrong number of arguments to load().");
        }
        if (2 <= args.length) {
            if (!(args[1] instanceof RubyHash)) {
                throw context.runtime.newArgumentError("options must be a hash.");
            }

            mode = getMode(runtime, mode, (RubyHash) args[1]);
        }

        ParserSource source = processArgs(context, args, options);

        switch (mode) {
            case StrictMode:
                return new StrictParse(source, context, options).parse(oj, true, block);
            case NullMode:
            case CompatMode:
                return new CompatParse(source, context, options).parse(oj, true, block);
            case ObjectMode:
                // is the catch all parser below...
            default:
                break;
        }

        return new ObjectParse(source, context, options).parse(oj, true, block);
    }

    private static char getMode(Ruby runtime, char mode, RubyHash ropts) {
        IRubyObject v;
        if (null != (v = ropts.fastARef(runtime.newSymbol("mode")))) {
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
        return mode;
    }


    @JRubyMethod(module = true, rest = true)
    public static IRubyObject load_file(ThreadContext context, IRubyObject self, IRubyObject[] args, Block block) {
        OjLibrary oj = resolveOj(self);
        Options options = oj.default_options.dup(context);
        Ruby runtime = context.runtime;

        if (1 > args.length) {
            throw runtime.newArgumentError("Wrong number of arguments to load().");
        }

        if (!(args[0] instanceof RubyString)) {
            throw runtime.newArgumentError("Expected String");
        }

        char mode = options.mode;

        if (2 <= args.length) mode = getMode(runtime, mode, (RubyHash) args[1]);

        String path = args[0].asJavaString();

        ParserSource source = new FileParserSource(context, path);

        switch (mode) {
            case StrictMode:
                return new StrictParse(source, context, options).parse(oj, true, block);
            case NullMode:
            case CompatMode:
                return new CompatParse(source, context, options).parse(oj, true, block);
            case ObjectMode:
            default:
                break;
        }

        return new ObjectParse(source, context, options).parse(oj, true, block);
    }

    @JRubyMethod(module = true)
    public static IRubyObject safe_load(ThreadContext context, IRubyObject self, IRubyObject doc, Block block) {
        OjLibrary oj = resolveOj(self);
        Options options = oj.default_options.dup(context);

        options.auto_define = No;
        options.sym_key = No;
        options.mode = StrictMode;
        IRubyObject[] args = new IRubyObject[] { doc };
        ParserSource source = processArgs(context, args, options);

        // FIXME: can remove boxing if we are making or already know some of these arguments.
        return new StrictParse(source, context, options).parse(oj, true, block);
    }

    @JRubyMethod(module = true, required = 1, rest = true)
    public static IRubyObject strict_load(ThreadContext context, IRubyObject self, IRubyObject[] args, Block block) {
        OjLibrary oj = resolveOj(self);
        Options options = oj.default_options.dup(context);
        ParserSource source = processArgs(context, args, options);

        return new StrictParse(source, context, options).parse(oj, true, block);
    }

    @JRubyMethod(module = true, required = 1, rest = true)
    public static IRubyObject compat_load(ThreadContext context, IRubyObject self, IRubyObject[] args, Block block) {
        OjLibrary oj = resolveOj(self);
        Options options = oj.default_options.dup(context);
        ParserSource source = processArgs(context, args, options);

        return new CompatParse(source, context, options).parse(oj, true, block);
    }

    @JRubyMethod(module = true, required = 1, rest = true)
    public static IRubyObject object_load(ThreadContext context, IRubyObject self, IRubyObject[] args, Block block) {
        OjLibrary oj = resolveOj(self);
        Options options = oj.default_options.dup(context);
        ParserSource source = processArgs(context, args, options);

        return new ObjectParse(source, context, options).parse(oj, true, block);
    }

    public static ParserSource processArgs(ThreadContext context, IRubyObject[] args, Options options) {
        Ruby runtime = context.runtime;
        IRubyObject	input;
        ByteList byteSource;

        if (args.length < 1) {
            throw runtime.newArgumentError("Wrong number of arguments to parse.");
        }
        input = args[0];
        if (2 == args.length) {
            RubyOj.parse_options(context, args[1], options);
        }

        if (input instanceof RubyString) {
            byteSource = ((RubyString) input).getByteList();
        } else if (context.nil == input && Yes == options.nilnil) {
            return null;
        } else {
            RubyModule clas = input.getMetaClass();

            if (runtime.getClass("StringIO") == clas) {
                input = input.callMethod(context, "string");
            } else if (!Platform.IS_WINDOWS && runtime.getFile() == clas && 0 == input.callMethod(context, "pos").convertToInteger().getLongValue()) {
                input = ((RubyFile) input).read(context);
            } else if (input.respondsTo("readpartial")) {
                return new ReadParserSource(context, input, "readpartial");
            } else if (input.respondsTo("read")) {
                return new ReadParserSource(context, input, "read");
            } else {
                throw runtime.newArgumentError("strict_parse() expected a String or IO Object.");
            }

            if (!(input instanceof RubyString)) {
                throw runtime.newArgumentError("strict_parse() expected a String or IO Object.");
            }
            byteSource = ((RubyString) input).getByteList();
        }

        return new StringParserSource(byteSource);
    }

    @JRubyMethod(module = true, required = 1, rest = true)
    public static IRubyObject dump(ThreadContext context, IRubyObject self, IRubyObject[] args) {
        OjLibrary oj = resolveOj(self);
        ByteList buf = Parse.newByteList();
        Out out = new Out(oj);
        Options	copts = oj.default_options.dup(context);

        if (copts.mode == CompatMode) copts.dump_opts.nan_dump = WordNan;

        if (2 == args.length) {
            parse_options(context, args[1], copts);
        }
        out.buf = buf;
        out.omit_nil = copts.dump_opts.omit_nil;
        out.caller = DumpCaller.CALLER_DUMP;

        Dump.obj_to_json(context, args[0], copts, out);

        RubyString string = out.asString(context);
        string.setEncoding(UTF8Encoding.INSTANCE);
        return string;
    }

    /*
    @JRubyMethod(module = true, required=2, rest = true)
    public static IRubyObject to_file(ThreadContext context, IRubyObject self, IRubyObject[] args) {
        OjLibrary oj = resolveOj(self);
        Options copts = oj.default_options;

        if (3 == args.length) {
            parse_options(context, args[2], copts);
        }

        TypeConverter.checkStringType(context.runtime, args[0]);
        oj_write_obj_to_file(args[1], args[0].asJavaString(), copts);

        return context.nil;
    }*/

    @JRubyMethod(module = true, rest = true)
    public static IRubyObject to_stream(ThreadContext context, IRubyObject self, IRubyObject[] args) {
        OjLibrary oj = resolveOj(self);
        Options options = oj.default_options.dup(context);

        if (3 == args.length) {
            parse_options(context, args[2], options);
        }
        Dump.oj_write_obj_to_stream(context, oj, args[1], args[0], options);

        return context.nil;
    }

    @JRubyMethod(module = true, rest = true)
    public static IRubyObject register_odd(ThreadContext context, IRubyObject self, IRubyObject[] args) {
        OjLibrary oj = resolveOj(self);
        if (3 > args.length) throw context.runtime.newArgumentError("incorrect number of arguments.");
        if (!(args[0] instanceof RubyModule)) throw context.runtime.newArgumentError("expecting class or module.");
        if (!(args[2] instanceof RubySymbol)) throw context.runtime.newArgumentError("expecting symbol.");

        if (MAX_ODD_ARGS < args.length - 2) {
            throw context.runtime.newArgumentError("too many members.");
        }

        IRubyObject[] newArgs = new IRubyObject[args.length - 3];
        System.arraycopy(args, 3, newArgs, 0, newArgs.length);

        oj.registerOdd((RubyModule) args[0], args[1], (RubySymbol) args[2], newArgs, false);

        return context.nil;
    }

    @JRubyMethod(module = true)
    public static IRubyObject saj_parse(ThreadContext context, IRubyObject self, IRubyObject handler, IRubyObject input) {
        ByteList json = input instanceof RubyString ? ((RubyString) input).getByteList() : getInput(context, input);

        new SajParse(new StringParserSource(json), context).parse(handler);

        return context.nil;
    }

    @JRubyMethod(module = true, required = 2, rest = true)
    public static IRubyObject sc_parse(ThreadContext context, IRubyObject self, IRubyObject[] args, Block block) {
        OjLibrary oj = resolveOj(self);
        Options copts = oj.default_options.dup(context);
        if (3 == args.length) {
            parse_options(context, args[2], copts);
        }

        IRubyObject[] newArgs = new IRubyObject[args.length - 1];
        System.arraycopy(args, 1, newArgs, 0, newArgs.length);
        ParserSource source = processArgs(context, newArgs, copts);

        return new SCParse(source, context, copts, args[0]).parse(oj, true, block);
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
