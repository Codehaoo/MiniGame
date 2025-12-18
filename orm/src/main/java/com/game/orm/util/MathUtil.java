package com.game.orm.util;

public class MathUtil {

    public static int findNextPositivePowerOfTwo(int value) {
        assert value > Integer.MIN_VALUE && value < 1073741824;
        return 1 << 32 - Integer.numberOfLeadingZeros(value - 1);
    }

    public static int safeFindNextPositivePowerOfTwo(int value) {
        return value <= 0 ? 1 : (value >= 1073741824 ? 1073741824 : findNextPositivePowerOfTwo(value));
    }
}
