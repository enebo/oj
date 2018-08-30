package oj;

import oj.dump.DumpOpts;
import org.jruby.RubyModule;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;

import java.util.List;

public class Options {
    public static final char Yes = 'y';
    public static final char No = 'n';
    public static final char NotSet = '\0';

    public static final char StrictMode = 's';
    public static final char ObjectMode = 'o';
    public static final char NullMode = 'n';
    public static final char CompatMode = 'c';
    public static final char RailsMode = 'r';
    public static final char CustomMode = 'C';
    public static final char WabMode = 'w';

    public static final char UnixTime = 'u';
    public static final char UnixZTime = 'z';
    public static final char XmlTime = 'x';
    public static final char RubyTime = 'r';

    public static final char NLEsc = 'n';
    public static final char JSONEsc = 'j';
    public static final char XSSEsc = 'x';
    public static final char ASCIIEsc = 'a';
    public static final char JXEsc = 'r'; // json
    public static final char RailsEsc = 'R'; // rails non escape

    public static final char BigDec = 'b';
    public static final char FloatDec = 'f';
    public static final char AutoDec = 'a';

    public static final char STRING_IO = 'c';
    public static final char STREAM_IO = 's';
    public static final char FILE_IO = 'f';

    public static final ByteList JSON_CLASS = new ByteList(new byte[] {'j', 's', 'o', 'n', '_', 'c', 'l', 'a', 's', 's'});

    public int indent;		// indention for dump; default 2
    public char circular;	// YesNo
    char	auto_define;	// YesNo
    char	sym_key;	// YesNo
    public char escape_mode;	// Escape_Mode
    public char mode;		// Mode
    char	class_cache;	// YesNo
    public char time_format;	// TimeFormat
    public char	bigdec_as_num;	// YesNo
    char	bigdec_load;	// BigLoad
    char    to_hash;	// YesNo
    public char to_json;	// YesNo
    char	as_json;	// YesNo
    char	nilnil;		// YesNo
    char	empty_string;		// YesNo
    char	allow_gc;	// allow GC during parse
    char	quirks_mode;	// allow single JSON values instead of documents
    char    allow_invalid;	// YesNo - allow invalid unicode
    char    create_ok;	// YesNo allow create_id
    char    allow_nan;	// YEsyNo for parsing only
    char    trace;		// YesNo
    ByteList create_id;	// 0 or string
    public int sec_prec;	// second precision when dumping time
    public int float_prec;	// float precision, linked to float_fmt
    public String float_fmt;	// float format for dumping, if empty use Ruby
    IRubyObject hash_class;	// class to use in place of Hash on load
    IRubyObject array_class;	// class to use in place of Array on load
    public DumpOpts dump_opts;
    RxClass str_rx;
    // FIXME: Consider primitive array for this
    public List<RubyModule> ignore; // array of classes to ignore;

    
    public Options(ThreadContext context) {
        indent = 0;
        circular = No;
        auto_define = No;
        sym_key = No;
        escape_mode = JSONEsc;
        mode = ObjectMode;
        class_cache = Yes;
        time_format = UnixTime;
        bigdec_as_num = NotSet;
        bigdec_load = AutoDec;
        to_hash = No;
        to_json = Yes;
        as_json = No;
        nilnil = No;
        empty_string = Yes;
        allow_gc = Yes;
        quirks_mode = Yes;
        allow_invalid = No;
        create_ok = No;
        allow_nan = Yes;
        trace = No;
        create_id = JSON_CLASS;
        sec_prec = 9;
        float_prec = 15;
        float_fmt = "%0.15g";
        hash_class = context.nil;
        array_class = context.nil;
        dump_opts = new DumpOpts();
        str_rx = new RxClass();
    }

    public Options dup(ThreadContext context) {
        Options newOptions = new Options(context);

        newOptions.indent = indent;
        newOptions.circular = circular;
        newOptions.auto_define = auto_define;
        newOptions.sym_key = sym_key;
        newOptions.escape_mode = escape_mode;
        newOptions.mode = mode;
        newOptions.class_cache = class_cache;
        newOptions.time_format = time_format;
        newOptions.bigdec_as_num = bigdec_as_num;
        newOptions.bigdec_load = bigdec_load;
        newOptions.to_hash = to_hash;
        newOptions.to_json = to_json;
        newOptions.as_json = as_json;
        newOptions.nilnil = nilnil;
        newOptions.empty_string = empty_string;
        newOptions.allow_gc = allow_gc;
        newOptions.quirks_mode = quirks_mode;
        newOptions.allow_invalid = allow_invalid;
        newOptions.create_ok = create_ok;
        newOptions.allow_nan = allow_nan;
        newOptions.trace = trace;
        newOptions.create_id = create_id;
        newOptions.sec_prec = sec_prec;
        newOptions.float_prec = float_prec;
        newOptions.float_fmt = float_fmt;
        newOptions.dump_opts = dump_opts;
        newOptions.str_rx = str_rx;

        return newOptions;
    }

    public static Options mimicOptions(ThreadContext context) {
        Options options =  new Options(context);

        options.mode = CompatMode;
        options.class_cache = No;
        options.time_format = RubyTime;
        options.bigdec_as_num = No;
        options.bigdec_load = FloatDec;
        options.to_json = No;
        options.nilnil = Yes;
        options.float_prec = 16;			// float_prec
        options.float_fmt = "%0.16g";		// float_fmt

        return options;
    }
}
