package com.game.orm.accessor;

import com.game.orm.entity.AbstractEntity;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.lang.Nullable;

import java.util.List;

public interface IAccessor {

    @Nullable
    <PK extends Comparable<PK>, E extends AbstractEntity<PK>> E find(PK pk, Class<E> entityClazz);

    <PK extends Comparable<PK>, E extends AbstractEntity<PK>> List<E> findAll(Class<E> entityClazz);

    <PK extends Comparable<PK>, E extends AbstractEntity<PK>> void insert(E entity);

    <PK extends Comparable<PK>, E extends AbstractEntity<PK>> void batchInsert(List<E> entities);

    <PK extends Comparable<PK>, E extends AbstractEntity<PK>> void fullUpdate(E entity);

    <PK extends Comparable<PK>, E extends AbstractEntity<PK>> void update(E entity, Update update);

    <PK extends Comparable<PK>, E extends AbstractEntity<PK>> void batchUpdate(List<E> entities, List<Update> updates);

    <PK extends Comparable<PK>, E extends AbstractEntity<PK>> void delete(E entity);

    <PK extends Comparable<PK>, E extends AbstractEntity<PK>> void delete(PK pk, Class<E> entityClazz);

    <PK extends Comparable<PK>, E extends AbstractEntity<PK>> void batchDelete(List<E> entities);

    <PK extends Comparable<PK>, E extends AbstractEntity<PK>> void batchDelete(List<PK> pks, Class<E> entityClazz);
}
