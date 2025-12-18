package com.game.orm.base;

import com.game.orm.util.MathUtil;
import com.game.orm.util.RandomUtil;
import com.game.orm.util.ThreadUtil;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class TaskBus {

    /**
     * 适用于CPU密集型任务
     */
    private static final int EXECUTOR_SIZE = MathUtil.safeFindNextPositivePowerOfTwo(Runtime.getRuntime().availableProcessors());
    private static final int EXECUTOR_MASK = EXECUTOR_SIZE - 1;

    private static final ExecutorService[] executors = new ExecutorService[EXECUTOR_SIZE];

    static {
        for (int i = 0; i < executors.length; i++) {
            CustomizableThreadFactory namedThreadFactory = new CustomizableThreadFactory("task-p" + i);
            ExecutorService executor = Executors.newSingleThreadExecutor(namedThreadFactory);
            executors[i] = executor;
        }
    }

    private static long selectExecutorHash(Object argument) {
        long hash;
        if (argument == null) {
            hash = RandomUtil.randomLong();
        } else if (argument instanceof Number) {
            hash = ((Number) argument).longValue();
        } else {
            hash = argument.hashCode();
        }
        return hash;
    }

    private static ExecutorService executorOf(long hash) {
        return executors[Math.abs(Math.toIntExact(hash & EXECUTOR_MASK))];
    }

    public static ExecutorService executorOf(Object argument) {
        return executorOf(selectExecutorHash(argument));
    }

    public static void execute(Object argument, Runnable runnable) {
        executorOf(argument).execute(ThreadUtil.safeRunnable(runnable));
    }

}
