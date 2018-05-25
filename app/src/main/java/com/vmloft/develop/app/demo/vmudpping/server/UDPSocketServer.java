package com.vmloft.develop.app.demo.vmudpping.server;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;

public class UDPSocketServer {

    private static DatagramSocket mSocket;
    private static int port = 9966;

    public static void main(String[] args) {
        try {
            // 1.初始化DatagramSocket，指定端口号
            mSocket = new DatagramSocket(port);
            // 2.创建用于接收消息的DatagramPacket，指定接收数据大小

            // 3.接收客户端消息
            System.out.println("***************服务器开始监听消息***************");
            while (true) {
                byte[] receiveData = new byte[1024];
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                // 在接收到信息之前，一直保持阻塞状态
                mSocket.receive(receivePacket);
                //System.out.println("收到时间戳 " + Util.bytesToLong(receiveData));
                //System.out.println("收到客户端数据:" + new String(receiveData, 0, receivePacket.getLength()));
                SocketAddress remoteAddress = receivePacket.getSocketAddress();
                DatagramPacket sendPacket = new DatagramPacket(receiveData, receivePacket.getLength(), remoteAddress);
                mSocket.send(sendPacket);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            mSocket.close();// 关闭资源
        }
    }
}
