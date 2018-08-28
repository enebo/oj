package oj;

import oj.attr_get_func.GetDateTimeSecs;
import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.ArrayList;
import java.util.List;

public class Odd {
    String classname;
    RubyModule clas;
    IRubyObject createObj;
    String createOp;
    String[] attrs;
    AttrGetFunc[] attrFuncs;
    boolean isModule;
    boolean raw;

    private static AttrGetFunc getDateTimeSecs = new GetDateTimeSecs();

    public Odd(Ruby runtime, String classname, String[] attrNames) {
        this.classname = classname;
        this.attrs = attrNames;
        this.clas = (RubyModule) runtime.getObject().getConstant(classname);
        this.createObj = clas;
        this.createOp = "new";
        this.attrFuncs = new AttrGetFunc[attrNames.length];
    }

    public Odd(RubyModule clas, String[] attrNames) {
        this.classname = clas.getName();
        this.attrs = attrNames;
        this.clas = clas;
        this.createObj = clas;
        this.createOp = "new";
        this.attrFuncs = new AttrGetFunc[attrNames.length];
    }

    public static List<Odd> initBuiltinOdds(Ruby runtime) {
        List <Odd> odds = new ArrayList<>(4);

        Odd odd = new Odd(runtime, "Rational", new String[] {"numerator", "denominator"});
        odd.createObj = runtime.getObject();
        odd.createOp = "Rational";
        odds.add(odd);

        odds.add(new Odd(runtime, "Date", new String[] {"year", "month", "day", "start"}));
        odd = new Odd(runtime, "DateTime",
                new String[] {"year", "month", "day", "hour", "min", "sec", "offset", "start"});
        odd.attrFuncs[5] = getDateTimeSecs;
        odds.add(odd);
        odds.add(new Odd(runtime, "Range", new String[] { "begin", "end", "exclude_end?"}));

        return odds;
    }
}
