package com.vmloft.develop.app.demo.udptools;

import java.nio.ByteBuffer;

public class Util {

    /**
     * int 转 byte
     */
    public static byte[] intToBytes(int i) {
        byte[] bytes = new byte[4];
        bytes[0] = (byte) ((i >> 24) & 0xFF);
        bytes[1] = (byte) ((i >> 16) & 0xFF);
        bytes[2] = (byte) ((i >> 8) & 0xFF);
        bytes[3] = (byte) (i & 0xFF);
        return bytes;
    }

    /**
     * byte 转 int
     */
    public static int bytesToInt(byte[] bytes) {
        return bytes[3] & 0xFF | (bytes[2] & 0xFF) << 8 | (bytes[1] & 0xFF) << 16 | (bytes[0] & 0xFF) << 24;
    }

    /**
     * long 转 byte
     */
    public static byte[] longToBytes(long l) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(8);
        byteBuffer.putLong(l);
        return byteBuffer.array();
    }

    /**
     * byte 转 long
     */
    public static long bytesToLong(byte[] bytes) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(8);
        byteBuffer.put(bytes, 0, 8);
        byteBuffer.flip();
        return byteBuffer.getLong();
    }

    /**
     * 当前系统时间毫秒值
     */
    public static long millisTime() {
        return System.currentTimeMillis();
    }

    /**
     * 获取纳秒级别时间
     */
    public static long microTime() {
        return System.nanoTime() / 1000;
    }
}
