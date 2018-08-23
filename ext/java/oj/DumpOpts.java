package oj;

import oj.options.NanDump;
import org.jruby.util.ByteList;

import static oj.Dump.MAX_DEPTH;

/**
 * Created by enebo on 8/24/15.
 */
public class DumpOpts {
    boolean use;
    ByteList indent_str;
    ByteList before_sep;
    ByteList after_sep;
    ByteList hash_nl;
    ByteList array_nl;
    int	indent_size;
    int before_size;
    int	after_size;
    int	hash_size;
    int	array_size;
    NanDump nan_dump = NanDump.AutoNan;
    boolean omit_nil;
    int max_depth = MAX_DEPTH;
}
