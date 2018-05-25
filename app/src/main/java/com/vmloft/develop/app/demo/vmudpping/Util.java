package com.vmloft.develop.app.demo.vmudpping;

import java.nio.ByteBuffer;

public class Util {

    /**
     * long 整型数转字节数组
     */
    public static byte[] longToBytes(long l) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(8);
        byteBuffer.putLong(l);
        return byteBuffer.array();
    }

    /**
     * 字节数组转 long
     */
    public static long bytesToLong(byte[] bytes) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(8);
        byteBuffer.put(bytes, 0, 8);
        byteBuffer.flip();
        return byteBuffer.getLong();
    }

    public static long millisTime() {
        return System.currentTimeMillis();
    }

    /**
     * 获取系统纳秒级别时间
     */
    public static long microTime() {
        return System.nanoTime() / 1000;
    }
}
