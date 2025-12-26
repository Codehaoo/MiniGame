package com.game.orm.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.index.Indexed;

@Data
@EqualsAndHashCode(callSuper = true)
public class PlayerEntity extends AbstractEntity<Long> {
    @Indexed(name = "playerName_idx")
    private String name;
    private int level;
    private int exp;
}
