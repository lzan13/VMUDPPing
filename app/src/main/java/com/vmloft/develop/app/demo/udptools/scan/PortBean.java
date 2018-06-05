package com.vmloft.develop.app.demo.udptools.scan;

public class PortBean {

    // 端口号
    private int port = 1024;
    // 是否打开
    private boolean isOpen = false;
    // 验证时间
    private long time;

    public PortBean(int port, long time) {
        setPort(port);
        setTime(time);
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public boolean isOpen() {
        return isOpen;
    }

    public void setOpen(boolean open) {
        isOpen = open;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }
}
