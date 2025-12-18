package com.game.orm.util;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class RandomUtil {

    public static ThreadLocalRandom getRandom() {
        return ThreadLocalRandom.current();
    }

    public static int randomInt() {
        return getRandom().nextInt();
    }

    public static int randomInt(int limit) {
        return getRandom().nextInt(limit);
    }

    public static long randomLong() {
        return getRandom().nextLong();
    }

    public static <T> T random(List<T> list) {
        return random(list, list.size());
    }

    public static <T> T random(List<T> list, int limit) {
        return list.get(randomInt(limit));
    }
}
