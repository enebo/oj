package oj;

/**
 * Created by enebo on 8/24/15.
 */
public class Options {
    public static final char Yes = 'y';
    public static final char No = 'n';
    public static final char NotSet = '\0';

    public static final char StrictMode = 's';
    public static final char ObjectMode = 'o';
    public static final char NullMode = 'n';
    public static final char CompatMode = 'c';

    public static final char UnixTime = 'u';
    public static final char UnixZTime = 'z';
    public static final char XmlTime = 'x';
    public static final char RubyTime = 'r';

    public static final char NLEsc = 'n';
    public static final char JSONEsc = 'j';
    public static final char XSSEsc = 'x';
    public static final char ASCIIEsc = 'a';

    public static final char BigDec = 'b';
    public static final char FloatDec = 'f';
    public static final char AutoDec = 'a';

    public static final char ArrayNew = 'A';
    public static final char ArrayType = 'a';
    public static final char ObjectNew = 'O';
    public static final char ObjectType = 'o';

    public static final char STRING_IO = 'c';
    public static final char STREAM_IO = 's';
    public static final char FILE_IO = 'f';

    int		indent;		// indention for dump; default 2
    char	circular;	// YesNo
    char	auto_define;	// YesNo
    char	sym_key;	// YesNo
    char	escape_mode;	// Escape_Mode
    char	mode;		// Mode
    char	class_cache;	// YesNo
    char	time_format;	// TimeFormat
    char	bigdec_as_num;	// YesNo
    char	bigdec_load;	// BigLoad
    char	to_json;	// YesNo
    char	nilnil;		// YesNo
    char	allow_gc;	// allow GC during parse
    char	quirks_mode;	// allow single JSON values instead of documents
    String create_id;	// 0 or string
    int		sec_prec;	// second precision when dumping time
    DumpOpts	dump_opts;
    char	float_prec;	// float precision, linked to float_fmt
    String	float_fmt;	// float format for dumping, if empty use Ruby
    
    public Options() {
        indent = 0;
        circular = No;			// circular
        auto_define = No;			// auto_define
        sym_key = No;			// sym_key
        escape_mode = JSONEsc;		// escape_mode
        mode = ObjectMode;		// mode
        class_cache = Yes;		// class_cache
        time_format = UnixZTime;		// time_format
        bigdec_as_num = Yes;		// bigdec_as_num
        bigdec_load = AutoDec;		// bigdec_load
        to_json = Yes;		// to_json
        nilnil = No;			// nilnil
        allow_gc = Yes;		// allow_gc
        quirks_mode = Yes;		// quirks_mode
        create_id = json_class;		// create_id
        sec_prec = 9;			// sec_prec
        dump_opts = null;			// dump_opts
        float_prec = 15;			// float_prec
        float_fmt = "%0.15g";		// float_fmt
    }
}
