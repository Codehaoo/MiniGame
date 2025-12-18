package com.game.orm.accessor;

import com.game.orm.entity.AbstractEntity;
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.mongodb.core.query.UpdateDefinition;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Slf4j
@Component
public class MongoAccessor implements IAccessor {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public <PK extends Comparable<PK>, E extends AbstractEntity<PK>> E find(PK pk, Class<E> entityClazz) {
        return mongoTemplate.findById(pk, entityClazz);
    }

    @Override
    public <PK extends Comparable<PK>, E extends AbstractEntity<PK>> List<E> findAll(Class<E> entityClazz) {
        return mongoTemplate.findAll(entityClazz);
    }

    @Override
    public <PK extends Comparable<PK>, E extends AbstractEntity<PK>> void insert(E entity) {
        try {
            mongoTemplate.insert(entity);
        } catch (Exception e) {
            log.error("{} insert error", entity.getClass().getSimpleName(), e);
        }
    }

    @Override
    public <PK extends Comparable<PK>, E extends AbstractEntity<PK>> void batchInsert(List<E> entities) {
        if (CollectionUtils.isEmpty(entities)) {
            return;
        }
        try {
            Collection<E> result = mongoTemplate.insertAll(entities);
            if (result.size() != entities.size()) {
                log.error("{} insert error, expected insert {}, inserted {}", entities.getFirst().getClass().getSimpleName(), entities.size(), result.size());
            }
        } catch (Exception e) {
            log.error("{} insert error", entities.getFirst().getClass().getSimpleName(), e);
        }
    }

    @Override
    public <PK extends Comparable<PK>, E extends AbstractEntity<PK>> void fullUpdate(E entity) {
        try {
            mongoTemplate.save(entity);
        } catch (Exception e) {
            log.error("{} full update error", entity.getClass().getSimpleName(), e);
        }
    }

    @Override
    public <PK extends Comparable<PK>, E extends AbstractEntity<PK>> void update(E entity, Update update) {
        try {
            UpdateResult result = mongoTemplate.updateFirst(new Query(Criteria.where("_id").is(entity.id())), update, entity.getClass());
            if (result.getModifiedCount() != 1) {
                log.error("{} update error, expected update 1, updated {}", entity.getClass().getSimpleName(), result.getModifiedCount());
            }
        } catch (Exception e) {
            log.error("{} update error", entity.getClass().getSimpleName(), e);
        }
    }

    @Override
    public <PK extends Comparable<PK>, E extends AbstractEntity<PK>> void batchUpdate(List<E> entities, List<Update> updates) {
        if (CollectionUtils.isEmpty(entities) || CollectionUtils.isEmpty(updates)) {
            return;
        }
        List<Pair<Query, UpdateDefinition>> pairs = new ArrayList<>(updates.size());
        for (int i = 0; i < updates.size(); i++) {
            pairs.add(Pair.of(new Query(Criteria.where("_id").is(entities.get(i).id())), updates.get(i)));
        }
        try {
            // ORDERED: 有序执行，遇到错误停止
            // UNORDERED: 无序执行，遇到错误继续
            BulkOperations bulkOps = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, entities.getFirst().getClass());
            BulkWriteResult result = bulkOps.updateMulti(pairs).execute();
            if (result.getModifiedCount() != pairs.size()) {
                log.error("{} update error, expected update {}, updated {}", entities.getFirst().getClass().getSimpleName(), pairs.size(), result.getModifiedCount());
            }
        } catch (Exception e) {
            log.error("{} update error", entities.getFirst().getClass().getSimpleName(), e);
        }
    }

    @Override
    public <PK extends Comparable<PK>, E extends AbstractEntity<PK>> void delete(E entity) {
        delete(entity.id(), entity.getClass());
    }

    @Override
    public <PK extends Comparable<PK>, E extends AbstractEntity<PK>> void delete(PK pk, Class<E> entityClazz) {
        try {
            DeleteResult result = mongoTemplate.remove(new Query(Criteria.where("_id").is(pk)), entityClazz);
            if (result.getDeletedCount() != 1) {
                log.error("{} delete error, expected delete 1, deleted {}", entityClazz.getSimpleName(), result.getDeletedCount());
            }
        } catch (Exception e) {
            log.error("{} delete error", entityClazz.getSimpleName(), e);
        }
    }

    @Override
    public <PK extends Comparable<PK>, E extends AbstractEntity<PK>> void batchDelete(List<E> entities) {
        if (CollectionUtils.isEmpty(entities)) {
            return;
        }
        List<PK> pks = entities.stream().map(AbstractEntity::id).toList();
        batchDelete(pks, entities.getFirst().getClass());
    }

    @Override
    public <PK extends Comparable<PK>, E extends AbstractEntity<PK>> void batchDelete(List<PK> pks, Class<E> entityClazz) {
        if (CollectionUtils.isEmpty(pks)) {
            return;
        }
        try {
            // ORDERED: 有序执行，遇到错误停止
            // UNORDERED: 无序执行，遇到错误继续
            BulkOperations bulkOps = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, entityClazz);
            BulkWriteResult result = bulkOps.remove(new Query(Criteria.where("_id").in(pks))).execute();
            if (result.getDeletedCount() != pks.size()) {
                log.error("{} delete error, expected delete {}, deleted {}", entityClazz.getSimpleName(), pks.size(), result.getDeletedCount());
            }
        } catch (Exception e) {
            log.error("{} delete error", entityClazz.getSimpleName(), e);
        }
    }
}
