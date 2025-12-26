package com.game.orm.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.HashSet;
import java.util.Set;

@Data
@EqualsAndHashCode(callSuper = true)
public class GuildEntity extends AbstractEntity<Long> {
    private String name;
    private int level;

    private long leaderId;
    private Set<Long> memberIds = new HashSet<>();
}
