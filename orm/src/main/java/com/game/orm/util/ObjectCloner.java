package com.game.orm.util;

import lombok.extern.slf4j.Slf4j;

import java.io.*;

@Slf4j
public class ObjectCloner {

    public static <T extends Serializable> T clone(Object obj) {
        try {
            // 将对象写入字节流
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
            objectOutputStream.writeObject(obj);

            // 从字节流中读取并创建新的对象
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
            ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);
            return (T) objectInputStream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            log.error("ObjectCloner clone error", e);
            return null;
        }
    }
}
