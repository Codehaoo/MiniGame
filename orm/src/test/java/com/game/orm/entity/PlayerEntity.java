package com.game.orm.entity;

import lombok.Data;
import org.springframework.data.mongodb.core.index.Indexed;

@Data
public class PlayerEntity extends AbstractEntity<Long> {
    @Indexed(name = "playerName_idx")
    private String name;
    private int level;
    private int exp;
}
