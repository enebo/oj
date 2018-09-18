package oj.parse;

import oj.parse.Parse;
import org.jruby.Ruby;
import org.jruby.RubyBignum;
import org.jruby.ext.bigdecimal.RubyBigDecimal;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ConvertBytes;

/**
 * Created by enebo on 8/24/15.
 */
public class NumInfo {
    public static final double OJ_INFINITY = Double.POSITIVE_INFINITY; // This was div by 0.0 in oj C src

    private Parse parse;
    public int str_start;
    public int str_length;
    public boolean infinity = false;
    public boolean neg = false;
    public boolean nan = false;
    public boolean big = false;
    public boolean no_big;
    public int exp = 0;
    public double div = 1;
    public long i = 0;
    public int num = 0;
    public int len = 0;
    public int dec_cnt = 0;
    public boolean hasExp = false;

    public NumInfo(Parse parse) {
        this.parse = parse;
    }

    public void reset() {
        str_start = 0;
        str_length = 0;
        infinity = false;
        neg = false;
        nan = false;
        big = false;
        no_big = false;
        exp = 0;
        div = 1;
        i = 0;
        num = 0;
        len = 0;
        dec_cnt = 0;
        hasExp = false;

    }

    // FIXME: This could potentially use an access point which directly consumed a bytelist (although JRuby needs to add one).
    private IRubyObject newBigDecimal(Ruby runtime, IRubyObject string) {
        return RubyBigDecimal.newInstance(runtime.getCurrentContext(), runtime.getClass("BigDecimal"), string);
    }

    // C: oj_num_as_value
    public IRubyObject toNumber(ThreadContext context) {
        Ruby runtime = context.runtime;
        IRubyObject rnum;

        if (infinity) {
            if (neg) {
                rnum = runtime.newFloat(-OJ_INFINITY);
            } else {
                rnum = runtime.newFloat(OJ_INFINITY);
            }
            } else if (nan) {
                rnum = runtime.newFloat(Double.NaN);
            } else if (1 == div && 0 == exp) { // fixnum
                if (big) {
                    // BIG already makes bytelist once to check for inf/nan so an instance var could be made to not do it twice.
                    if (256 > len) {
                        rnum = ConvertBytes.byteListToInum19(runtime, parse.source.subStr(str_start, str_length), 10, true);
                    } else {
                        rnum = ConvertBytes.byteListToInum19(runtime, parse.source.subStr(str_start, str_length), 10, true);
                    }
                } else {
                    if (neg) {
                        rnum = RubyBignum.bignorm(runtime, RubyBignum.long2big(-i));
                    } else {
                        rnum = RubyBignum.bignorm(runtime, RubyBignum.long2big(i));
                    }
                }
        } else { // decimal
            if (big) {
                    rnum = newBigDecimal(runtime, runtime.newString(parse.source.subStr(str_start, str_length)));
                    if (no_big) {
                        rnum = rnum.callMethod(context, "to_f");
                    }
                } else {
                    double	d = (double)i + (double)num * (1.0 / div);

                    if (neg) {
                        d = -d;
                    }
                    if (0 != exp) {
                        d *= Math.pow(10.0, exp);
                    }
                    rnum = runtime.newFloat(d);
                }
            }
            return rnum;
    }
}
