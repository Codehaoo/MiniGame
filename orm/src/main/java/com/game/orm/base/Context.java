package com.game.orm.base;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Component
public class Context implements ApplicationRunner, DisposableBean {

    private static Context instance;
    private static List<Container> containers;

    @Autowired
    private ApplicationContext applicationContext;

    public static <T> T getBean(Class<T> type) {
        return instance.applicationContext.getBean(type);
    }

    public static <T> T getBean(String name, Class<T> type) {
        return instance.applicationContext.getBean(name, type);
    }

    public static <T> Map<String, T> getBeansOfType(Class<T> type) {
        return instance.applicationContext.getBeansOfType(type);
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        instance = this;
        containers = getBeansOfType(Container.class).values().stream().sorted(Comparator.comparingInt(Container::priority).reversed()).toList();
        containers.forEach(Container::start);
    }

    @Override
    public void destroy() throws Exception {
        // 倒序销毁
        for (int i = containers.size() - 1; i >= 0; i--) {
            containers.get(i).stop();
        }
    }
}
