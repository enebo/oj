package oj;

/**
 * Created by enebo on 8/24/15.
 */
public class Options {
    public static char Yes = 'y';
    public static char No = 'n';
    public static char NotSet = '\0';

    int		indent;		// indention for dump, default 2
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
    int	create_id_len;	// length of create_id
    int		sec_prec;	// second precision when dumping time
    DumpOpts	dump_opts;
    char	float_prec;	// float precision, linked to float_fmt
    char[]	float_fmt;	// float format for dumping, if empty use Ruby
}
