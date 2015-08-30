package oj;

import org.jruby.Ruby;
import org.jruby.RubyBignum;
import org.jruby.ext.bigdecimal.RubyBigDecimal;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.ConvertBytes;

/**
 * Created by enebo on 8/24/15.
 */
public class NumInfo {
    static final double OJ_INFINITY = Double.POSITIVE_INFINITY; // This was div by 0.0 in oj C src

    public ByteList str;
    public boolean infinity = false;
    public boolean neg = false;
    public boolean nan = false;
    public boolean big = false;
    public boolean no_big;
    public double exp = 0;
    public double div = 1;
    public int i = 0;
    public int num = 0;
    public int len = 0;
    public int dec_cnt = 0;
    public boolean hasExp = false;


    // FIXME: This could potentially use an access point which directly consumed a bytelist (although JRuby needs to add one).
    private IRubyObject newBigDecimal(Ruby runtime, IRubyObject string) {
        return RubyBigDecimal.newBigDecimal(runtime.getBasicObject(), new IRubyObject[]{string}, Block.NULL_BLOCK);
    }
    
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
                    if (256 > len) {
                        rnum = ConvertBytes.byteListToInum19(runtime, str, 10, true);
                    } else {
                        rnum = ConvertBytes.byteListToInum19(runtime, str, 10, true);
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
                    rnum = newBigDecimal(runtime, runtime.newString(str));
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
