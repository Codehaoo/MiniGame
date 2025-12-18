package com.game.orm.test;

import com.game.orm.entity.PlayerEntity;
import com.game.orm.service.EntityService;
import com.game.orm.util.RandomUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

@SpringBootTest
public class EntityServiceTest {

    @Autowired
    private EntityService service;

    @Test
    public void testGetPlayerEntity() {
        PlayerEntity playerEntity = service.get(10001L, PlayerEntity.class);
        System.out.println(playerEntity);
    }

    private void updatePlayerEntity(long playerId) {
        List<String> names = new ArrayList<>(List.of("小明", "小黑", "小兰", "小红"));
        PlayerEntity playerEntity = service.get(playerId, PlayerEntity.class);
        names.remove(playerEntity.getName());
        playerEntity.setName(RandomUtil.random(names));
        playerEntity.setLevel(playerEntity.getLevel() == Integer.MAX_VALUE ? 0 : playerEntity.getLevel() + 1);
        System.out.println(playerEntity);
    }

    @Test
    public void testUpdatePlayerEntity() throws InterruptedException {
        updatePlayerEntity(10001L);
        Thread.sleep(120_000L);
    }

    @Test
    public void testUpdatePlayerEntityNow() {
        PlayerEntity playerEntity = service.get(10001L, PlayerEntity.class);
        updatePlayerEntity(playerEntity.id());
        service.update(playerEntity);
    }
}
