package com.game.orm.entity;

import lombok.Data;

import java.util.HashSet;
import java.util.Set;

@Data
public class GuildEntity extends AbstractEntity<Long> {
    private String name;
    private int level;

    private long leaderId;
    private Set<Long> memberIds = new HashSet<>();
}
