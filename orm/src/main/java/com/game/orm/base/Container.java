package com.game.orm.base;

public interface Container {

    void start();

    void stop();

    default int priority() {
        return 0;
    }
}
