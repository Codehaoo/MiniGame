package com.game.orm.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.core.type.filter.TypeFilter;

import java.util.HashSet;
import java.util.Set;

@Slf4j
public class ClassScannerUtil {

    public static Set<Class<?>> scan(String basePackage, Class<?> assignable) {
        return scan(basePackage, new AssignableTypeFilter(assignable));
    }

    public static Set<Class<?>> scan(String basePackage, TypeFilter includeFilter) {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(includeFilter);
        Set<BeanDefinition> beanDefinitions = scanner.findCandidateComponents(basePackage);
        Set<Class<?>> result = new HashSet<>(beanDefinitions.size());
        for (BeanDefinition beanDefinition : beanDefinitions) {
            try {
                Class<?> clazz = Class.forName(beanDefinition.getBeanClassName());
                result.add(clazz);
            } catch (ClassNotFoundException e) {
                log.warn("{} Class not found", beanDefinition.getBeanClassName(), e);
            }
        }

        return result;
    }
}
