package oj;

import oj.options.NanDump;
import org.jruby.util.ByteList;

import static oj.Dump.MAX_DEPTH;

/**
 * Created by enebo on 8/24/15.
 */
public class DumpOpts {
    boolean use;
    ByteList indent_str = ByteList.EMPTY_BYTELIST;
    ByteList before_sep = ByteList.EMPTY_BYTELIST;
    ByteList after_sep = ByteList.EMPTY_BYTELIST;
    ByteList hash_nl = ByteList.EMPTY_BYTELIST;
    ByteList array_nl = ByteList.EMPTY_BYTELIST;
    NanDump nan_dump = NanDump.AutoNan;
    boolean omit_nil;
    int max_depth = MAX_DEPTH;
}
