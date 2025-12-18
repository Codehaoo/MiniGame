package com.game.orm.entity;

public interface IEntity<PK extends Comparable<PK>> {

    PK id();

    /**
     * Actor模型思想，通过该Key来控制线程细粒度，从而实现逻辑上单线程
     */
    default Object ThreadRouteKey() {
        return id();
    }
}
