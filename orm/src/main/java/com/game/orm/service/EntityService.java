package com.game.orm.service;

import com.game.orm.base.TaskBus;
import com.game.orm.accessor.IAccessor;
import com.game.orm.base.Container;
import com.game.orm.entity.AbstractEntity;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class EntityService implements Container {

    @Autowired
    private IAccessor accessor;
    private final Map<Class<? extends AbstractEntity<?>>, Cache<Object, AbstractEntity<?>>> entityCache = new ConcurrentHashMap<>();

    @Override
    public void start() {
        // ClassScannerUtil.scan("com.game.orm", AbstractEntity.class);
    }

    @Override
    public void stop() {
        persisterEntity();
    }

    @Scheduled(cron = "0 */1 * * * *")
    public void persisterEntity() {
        entityCache.values().forEach(cache -> {
            cache.asMap().values().forEach(entity -> {
                TaskBus.execute(entity.ThreadRouteKey(), () -> {
                    if (entity.checkUpdateFields()) {
                        accessor.update(entity);
                    }
                });
            });
        });
    }

    public <PK extends Comparable<PK>, E extends AbstractEntity<PK>> E get(PK id, Class<E> entityClazz) {
        Cache<Object, AbstractEntity<?>> cache = entityCache.computeIfAbsent(entityClazz, _ -> Caffeine.newBuilder()
                .expireAfterAccess(3, TimeUnit.MINUTES)
                .maximumSize(3000)
                .removalListener((k, v, removalCause) -> {
                    accessor.fullUpdate((E) v);
                })
                .build());
        return (E) cache.get(id, k -> load((PK) k, entityClazz));
    }

    private <PK extends Comparable<PK>, E extends AbstractEntity<PK>> E load(PK Id, Class<E> clazz) {
        try {
            E e = accessor.find(Id, clazz);
            // todo 计算hashcode和jsonNode
            if (e == null) {
                e = clazz.getDeclaredConstructor().newInstance();
                e.setId(Id);
                accessor.insert(e);
            }
            return e;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                 NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public <PK extends Comparable<PK>, E extends AbstractEntity<PK>> void update(E entity) {
        if (entity.checkUpdateFields()) {
            accessor.update(entity);
        }
    }
}
