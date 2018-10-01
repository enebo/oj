package oj;

import org.joni.Regex;
import org.jruby.RubyClass;

public class RxClass {
    public Regex regex; // rrx in rxclass.c (also somewhat rx since we support regexp on windows)
    public RubyClass clas;
    public String error; // Error if this
}
