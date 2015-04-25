package eecs547.tac.aa;

import java.text.DecimalFormat;
import java.util.Random;

/**
 *
 * @author Augie
 */
public class Util {

    public static final Random RANDOM = new Random();
    public static final DecimalFormat DECIMAL_FORMAT_3 = new DecimalFormat("#.###");

    static {
        RANDOM.setSeed(System.currentTimeMillis());
    }

    public static final double round3(double d) {
        return Double.valueOf(DECIMAL_FORMAT_3.format(d));
    }

    public static final void debug(String s) {
        System.out.println(s);
    }

    public static final void debug(Exception e) {
        e.printStackTrace();
    }
}
