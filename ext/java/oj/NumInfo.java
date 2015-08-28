package oj;

import org.jruby.util.ByteList;

/**
 * Created by enebo on 8/24/15.
 */
public class NumInfo {
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
}
