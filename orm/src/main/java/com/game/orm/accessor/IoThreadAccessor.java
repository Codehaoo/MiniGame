package com.game.orm.accessor;

import com.game.orm.base.EventBus;
import com.game.orm.entity.AbstractEntity;

import java.util.*;
import java.util.stream.Collectors;

public class IoThreadAccessor implements IAccessor {

    private final IAccessor delegate;

    public IoThreadAccessor(IAccessor delegate) {
        this.delegate = Objects.requireNonNull(delegate);
    }

    @Override
    public <PK extends Comparable<PK>, E extends AbstractEntity<PK>> E find(PK pk, Class<E> entityClazz) {
        return delegate.find(pk, entityClazz);
    }

    @Override
    public <PK extends Comparable<PK>, E extends AbstractEntity<PK>> List<E> findAll(Class<E> entityClazz) {
        return delegate.findAll(entityClazz);
    }

    @Override
    public <PK extends Comparable<PK>, E extends AbstractEntity<PK>> void insert(E entity) {
        EventBus.execute(entity.ThreadRouteKey(), () -> delegate.insert(entity));
    }

    @Override
    public <PK extends Comparable<PK>, E extends AbstractEntity<PK>> void batchInsert(List<E> entities) {
        entities.stream()
                .collect(Collectors.groupingBy(entity -> EventBus.executorOf(entity.ThreadRouteKey())))
                .forEach((executor, result) -> executor.execute(() -> delegate.batchInsert(result)));
    }

    @Override
    public <PK extends Comparable<PK>, E extends AbstractEntity<PK>> void fullUpdate(E entity) {
        EventBus.execute(entity.ThreadRouteKey(), () -> delegate.fullUpdate(entity));
    }

    @Override
    public <PK extends Comparable<PK>, E extends AbstractEntity<PK>> void update(E entity) {
        EventBus.execute(entity.ThreadRouteKey(), () -> delegate.update(entity));
    }

    @Override
    public <PK extends Comparable<PK>, E extends AbstractEntity<PK>> void batchUpdate(List<E> entities) {
        entities.stream()
                .collect(Collectors.groupingBy(entity -> EventBus.executorOf(entity.ThreadRouteKey())))
                .forEach((executor, result) -> executor.execute(() -> delegate.batchUpdate(result)));
    }

    @Override
    public <PK extends Comparable<PK>, E extends AbstractEntity<PK>> void delete(E entity) {
        EventBus.execute(entity.ThreadRouteKey(), () -> delegate.delete(entity));
    }

    @Override
    public <PK extends Comparable<PK>, E extends AbstractEntity<PK>> void delete(PK pk, Class<E> entityClazz) {
        EventBus.execute(pk, () -> delegate.delete(pk, entityClazz)); // todo 不一定是以pk作为路由键
    }

    @Override
    public <PK extends Comparable<PK>, E extends AbstractEntity<PK>> void batchDelete(List<E> entities) {
        entities.stream()
                .collect(Collectors.groupingBy(entity -> EventBus.executorOf(entity.ThreadRouteKey())))
                .forEach((executor, result) -> executor.execute(() -> delegate.batchDelete(result)));
    }

    @Override
    public <PK extends Comparable<PK>, E extends AbstractEntity<PK>> void batchDelete(List<PK> pks, Class<E> entityClazz) {
        pks.stream()
                .collect(Collectors.groupingBy(pk -> EventBus.executorOf(pk))) // todo 不一定是以pk作为路由键
                .forEach((executor, result) -> executor.execute(() -> delegate.batchDelete(result, entityClazz)));
    }
}
