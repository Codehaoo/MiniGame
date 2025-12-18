package com.game.orm.util;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ThreadUtil {

    public static Runnable safeRunnable(Runnable runnable) {
        return () -> {
            try {
                runnable.run();
            } catch (Exception e) {
                log.error("execute exception", e);
            } catch (Throwable t) {
                log.error("execute error", t);
            }
        };
    }
}
