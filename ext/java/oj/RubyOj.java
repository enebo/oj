package oj;

import org.jruby.Ruby;
import org.jruby.RubyFixnum;
import org.jruby.RubyHash;
import org.jruby.RubyModule;
import org.jruby.RubyNumeric;
import org.jruby.RubyString;
import org.jruby.anno.JRubyMethod;
import org.jruby.anno.JRubyModule;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.TypeConverter;

import static oj.Options.*;

/**
 * Created by enebo on 8/25/15.
 */
@JRubyModule(name = "Oj")
public class RubyOj extends RubyModule {
    // FIXME: This should be internal variable
    private static Options oj_default_options;

    @JRubyMethod(module = true)
    public static IRubyObject default_options(ThreadContext context, IRubyObject self) {
        Ruby runtime = context.runtime; 
        RubyHash opts = RubyHash.newHash(runtime);
        IRubyObject Qtrue = runtime.getTrue();
        IRubyObject Qfalse = runtime.getFalse();
        IRubyObject Qnil = runtime.getNil();

        opts.fastASet(runtime.newSymbol("indent"), runtime.newFixnum(oj_default_options.indent));
        opts.fastASet(runtime.newSymbol("sec_prec"), runtime.newFixnum(oj_default_options.sec_prec));
        opts.fastASet(runtime.newSymbol("circular"), (Yes == oj_default_options.circular) ? Qtrue : ((No == oj_default_options.circular) ? Qfalse : Qnil));
        opts.fastASet(runtime.newSymbol("class_cache"), (Yes == oj_default_options.class_cache) ? Qtrue : ((No == oj_default_options.class_cache) ? Qfalse : Qnil));
        opts.fastASet(runtime.newSymbol("auto_define"), (Yes == oj_default_options.auto_define) ? Qtrue : ((No == oj_default_options.auto_define) ? Qfalse : Qnil));
        opts.fastASet(runtime.newSymbol("symbol_keys"), (Yes == oj_default_options.sym_key) ? Qtrue : ((No == oj_default_options.sym_key) ? Qfalse : Qnil));
        opts.fastASet(runtime.newSymbol("bigdecimal_as_decimal"), (Yes == oj_default_options.bigdec_as_num) ? Qtrue : ((No == oj_default_options.bigdec_as_num) ? Qfalse : Qnil));
        opts.fastASet(runtime.newSymbol("use_to_json"), (Yes == oj_default_options.to_json) ? Qtrue : ((No == oj_default_options.to_json) ? Qfalse : Qnil));
        opts.fastASet(runtime.newSymbol("nilnil"), (Yes == oj_default_options.nilnil) ? Qtrue : ((No == oj_default_options.nilnil) ? Qfalse : Qnil));
        opts.fastASet(runtime.newSymbol("allow_gc"), (Yes == oj_default_options.allow_gc) ? Qtrue : ((No == oj_default_options.allow_gc) ? Qfalse : Qnil));
        opts.fastASet(runtime.newSymbol("quirks_mode"), (Yes == oj_default_options.quirks_mode) ? Qtrue : ((No == oj_default_options.quirks_mode) ? Qfalse : Qnil));
        opts.fastASet(runtime.newSymbol("float_prec"), runtime.newFixnum(oj_default_options.float_prec));
        
        switch (oj_default_options.mode) {
            case StrictMode:	opts.fastASet(runtime.newSymbol("mode"), runtime.newSymbol("strict"));	break;
            case CompatMode:	opts.fastASet(runtime.newSymbol("mode"), runtime.newSymbol("compat"));	break;
            case NullMode:	opts.fastASet(runtime.newSymbol("mode"), runtime.newSymbol("null"));		break;
            case ObjectMode:
            default:		opts.fastASet(runtime.newSymbol("mode"), runtime.newSymbol("object"));	break;
        }
        switch (oj_default_options.escape_mode) {
            case NLEsc:		opts.fastASet(runtime.newSymbol("escape_mode"), runtime.newSymbol("newline"));
                break;
            case JSONEsc:	opts.fastASet(runtime.newSymbol("escape_mode"), runtime.newSymbol("json"));
                break;
            case XSSEsc:	opts.fastASet(runtime.newSymbol("escape_mode"), runtime.newSymbol("xss_safe"));	break;
            case ASCIIEsc:	opts.fastASet(runtime.newSymbol("escape_mode"), runtime.newSymbol("ascii"));
                break;
            default:		opts.fastASet(runtime.newSymbol("escape_mode"), runtime.newSymbol("json"));		break;
        }
        switch (oj_default_options.time_format) {
            case XmlTime:	opts.fastASet(runtime.newSymbol("time_format"), runtime.newSymbol("xmlschema"));	break;
            case RubyTime:	opts.fastASet(runtime.newSymbol("time_format"), runtime.newSymbol("ruby"));		break;
            case UnixZTime:	opts.fastASet(runtime.newSymbol("time_format"), runtime.newSymbol("unix_zone"));	break;
            case UnixTime:
            default:		opts.fastASet(runtime.newSymbol("time_format"), runtime.newSymbol("unix"));		break;
        }
        switch (oj_default_options.bigdec_load) {
            case BigDec:
                opts.fastASet(runtime.newSymbol("bigdecimal_load"), runtime.newSymbol("bigdecimal"));break;
            case FloatDec:	opts.fastASet(runtime.newSymbol("bigdecimal_load"), runtime.newSymbol("float"));	break;
            case AutoDec:
            default:		opts.fastASet(runtime.newSymbol("bigdecimal_load"), runtime.newSymbol("auto"));
                break;
        }
        opts.fastASet(runtime.newSymbol("create_id"), (null == oj_default_options.create_id) ? Qnil : runtime.newString(oj_default_options.create_id));

        return opts;
    }

    @JRubyMethod(name = "default_options=")
    public static IRubyObject set_def_opts(ThreadContext context, IRubyObject self, IRubyObject roptsArg) {
//        struct _YesNoOpt	ynos[] = {
//            { circular_sym, &copts.circular },
//            { auto_define_sym, &copts.auto_define },
//            { symbol_keys_sym, &copts.sym_key },
//            { class_cache_sym, &copts.class_cache },
//            { bigdecimal_as_decimal_sym, &copts.bigdec_as_num },
//            { use_to_json_sym, &copts.to_json },
//            { nilnil_sym, &copts.nilnil },
//            { allow_gc_sym, &copts.allow_gc },
//            { quirks_mode_sym, &copts.quirks_mode },
//            { Qnil, 0 }
//        };
//        YesNoOpt	o;

        if (!(roptsArg instanceof RubyHash)) return context.nil;
        RubyHash ropts = (RubyHash) roptsArg;
        Options copts = oj_default_options;
        
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
                int	n;

                if (!(v instanceof RubyFixnum)) {
                    throw runtime.newArgumentError(":float_precision must be a Fixnum.");
                }

                n = (int) ((RubyFixnum) v).getLongValue();
                if (0 >= n) {
                    copts.float_fmt = "";
                    copts.float_prec = 0;
                } else {
                    if (20 < n) {
                        n = 20;
                    }
                    sprintf(copts.float_fmt, "%%0.%dg", n);
                    copts.float_prec = (char) n;
                }
            }
            if (Qnil != (v = ropts.fastARef(runtime.newSymbol("sec_prec")))) {
                int	n;

                if (!(v instanceof RubyFixnum)) {
                    throw runtime.newArgumentError(":second_precision must be a Fixnum.");
                }
                n = RubyNumeric.num2int(v);
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
            for (o = ynos; 0 != o.attr; o++) {
                if (Qnil != (v = ropts.fastARef(o.sym))) {
                    if (Qtrue == v) {
                        o.attr = Yes;
                    } else if (Qfalse == v) {
                        o.attr = No;
                    } else {
                        throw runtime.newArgumentError("%s must be true or false.", rb_id2name(SYM2ID(o.sym)));
                    }
                }
            }
            // This is here only for backwards compatibility with the original Oj.
            v = ropts.fastARef(runtime.newSymbol("ascii_only"));
            if (Qtrue == v) {
                copts.escape_mode = ASCIIEsc;
            } else if (Qfalse == v) {
                copts.escape_mode = JSONEsc;
            }
        }
    }

    @JRubyMethod(module = true)
    public static IRubyObject mimic_JSON(ThreadContext context, IRubyObject self) {
    }

    @JRubyMethod(module = true)
    public static IRubyObject load(ThreadContext context, IRubyObject self) {
    }

    @JRubyMethod(module = true)
    public static IRubyObject load_file(ThreadContext context, IRubyObject self) {
    }

    @JRubyMethod(module = true)
    public static IRubyObject safe_load(ThreadContext context, IRubyObject self) {
    }

    @JRubyMethod(module = true)
    public static IRubyObject strict_load(ThreadContext context, IRubyObject self) {
    }

    @JRubyMethod(module = true)
    public static IRubyObject compat_load(ThreadContext context, IRubyObject self) {
    }

    @JRubyMethod(module = true)
    public static IRubyObject object_load(ThreadContext context, IRubyObject self) {
    }

    @JRubyMethod(module = true)
    public static IRubyObject dump(ThreadContext context, IRubyObject self) {
    }

    @JRubyMethod(module = true)
    public static IRubyObject to_file(ThreadContext context, IRubyObject self) {
    }

    @JRubyMethod(module = true)
    public static IRubyObject to_stream(ThreadContext context, IRubyObject self) {
    }
    
    @JRubyMethod(module = true)
    public static IRubyObject register_odd(ThreadContext context, IRubyObject self) {
    }

    @JRubyMethod(module = true)
    public static IRubyObject saj_parse(ThreadContext context, IRubyObject self) {
    }

    @JRubyMethod(module = true)
    public static IRubyObject sc_parse(ThreadContext context, IRubyObject self) {
    }
}
