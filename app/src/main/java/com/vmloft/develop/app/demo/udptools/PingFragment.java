package com.vmloft.develop.app.demo.udptools;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class PingFragment extends Fragment {

    private final String TAG = this.getClass().getSimpleName();

    @BindView(R.id.widget_spinner_host) Spinner hostSpinner;
    @BindView(R.id.widget_spinner_mode) Spinner modeSpinner;
    @BindView(R.id.widget_spinner_buffer) Spinner bufferSpinner;
    @BindView(R.id.widget_spinner_kbps) Spinner kbpsSpinner;
    @BindView(R.id.widget_spinner_time) Spinner timeSpinner;

    @BindView(R.id.layout_edit) LinearLayout editLayout;
    @BindView(R.id.edit_server_host) EditText hostEdit;
    @BindView(R.id.edit_server_port) EditText portEdit;
    @BindView(R.id.btn_test) Button testBtn;
    @BindView(R.id.text_state) TextView stateView;
    @BindView(R.id.text_kbps) TextView kbpsView;
    @BindView(R.id.text_buffer) TextView bufferView;
    @BindView(R.id.text_lost) TextView lostView;
    @BindView(R.id.text_delay) TextView delayView;

    @BindView(R.id.layout_help) RelativeLayout helpLayout;

    private Activity activity;

    // UDP Socket 对象
    private DatagramSocket socket = null;
    // 定时器
    private Timer timer;
    // 是否在运行中
    private boolean isRunning = false;
    private boolean isSending = false;

    /**
     * 服务器地址和端口号等
     * EBS 集群    "123.57.172.41":40000
     * VIP5 集群   "121.196.226.37":40000
     * VIP6 集群   "47.93.78.93":40000
     */
    private String[] serverTitles = { "EBS 集群", "VIP5 集群", "VIP6 集群", "自定义" };
    private String[] hostValues = {
        "123.57.172.41", "121.196.226.37", "47.93.78.93", "172.17.3.112"
    };
    private int[] portValues = { 40000, 40000, 40000, 8899 };
    private String host;
    private int port = 8899;

    /**
     * 测试模式
     */
    private String[] modeTitles = { "音频模式", "视频模式" };

    /**
     * 数据包大小
     */
    private String[] bufferTitles = { "62 byte", "1024 byte" };
    private int bufferSize;

    /**
     * 码率
     */
    private String[] kbpsTitles = {
        "10 kbps", "30 kbps", "100 kbps", "200 kbps", "250 kbps", "300 kbps", "600 kbps",
        "4000 kbps"
    };
    private int kbps;

    /**
     * 时间
     */
    private String[] timeTitles = { "10 sec", "30 sec", "60 sec" };
    private int time;

    // 开始时间
    private long sendTime = 0l;
    private long receiveTime = 0l;
    private long sendKbps;
    private long receiveKbps;
    // 发送数据 buffer
    private byte[] buffer;
    // 校验 buffer 的字段
    private String verifyStr = "HI";
    // 校验顺序
    private int verifyIndex = 0;
    // 乱序的数量
    private int disorders = 0;

    // 发送的包数量
    private long sendPacketCount = 0;
    // 接收的包数量
    private long receivePacketCount = 0;
    // 延迟相关变量
    private long delay, delayLow, delayHigh, delaySum;

    public static PingFragment newInstance() {
        PingFragment fragment = new PingFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_ping, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        activity = getActivity();

        init();
    }

    /**
     * 初始化
     */
    private void init() {

        // 服务器选择列表
        ArrayAdapter<String> hostAdapter = new ArrayAdapter<>(activity, android.R.layout.simple_spinner_dropdown_item, serverTitles);
        hostSpinner.setAdapter(hostAdapter);
        hostSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == serverTitles.length - 1) {
                    editLayout.setVisibility(View.VISIBLE);
                } else {
                    editLayout.setVisibility(View.GONE);
                }
                host = hostValues[position];
                port = portValues[position];
                hostEdit.setText(host);
                portEdit.setText(String.valueOf(port));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // 模式选择列表
        ArrayAdapter<String> modeAdapter = new ArrayAdapter<>(activity, android.R.layout.simple_spinner_dropdown_item, modeTitles);
        modeSpinner.setAdapter(modeAdapter);
        modeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                bufferSpinner.setSelection(position);
                if (position == 0) {
                    kbpsSpinner.setSelection(position);
                } else if (position == 1) {
                    kbpsSpinner.setSelection(4);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // 数据包选择列表
        ArrayAdapter<String> packetAdapter = new ArrayAdapter<>(activity, android.R.layout.simple_spinner_dropdown_item, bufferTitles);
        bufferSpinner.setAdapter(packetAdapter);
        bufferSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String packetStr = bufferTitles[position];
                bufferSize = Integer.valueOf(packetStr.substring(0, packetStr.indexOf(" ")));
                buffer = new byte[bufferSize];
                for (int i = 0; i < bufferSize; i++) {
                    buffer[i] = 0;
                }
                byte[] verifyBytes = verifyStr.getBytes();
                System.arraycopy(verifyBytes, 0, buffer, 8, verifyBytes.length);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // 码率选择列表
        ArrayAdapter<String> kbpsAdapter = new ArrayAdapter<>(activity, android.R.layout.simple_spinner_dropdown_item, kbpsTitles);
        kbpsSpinner.setAdapter(kbpsAdapter);
        kbpsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String kbpsStr = kbpsTitles[position];
                kbps = Integer.valueOf(kbpsStr.substring(0, kbpsStr.indexOf(" ")));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // 时间选择列表
        ArrayAdapter<String> timeAdapter = new ArrayAdapter<>(activity, android.R.layout.simple_spinner_dropdown_item, timeTitles);
        timeSpinner.setAdapter(timeAdapter);
        timeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String timeStr = timeTitles[position];
                time = Integer.valueOf(timeStr.substring(0, timeStr.indexOf(" ")));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    /**
     * 界面控件点击监听
     */
    @OnClick({ R.id.btn_test, R.id.layout_help })
    public void onClick(View v) {
        switch (v.getId()) {
        case R.id.btn_test:
            if (isRunning) {
                stopUDPSocket();
            } else {
                startUDPSocket();
            }
            break;
        case R.id.layout_help:
            helpLayout.setVisibility(View.GONE);
            break;
        }
    }

    /**
     * 校验接收到的数据
     */
    private boolean verifyReceiveData(byte[] receiveData) {
        String receiveVerify = new String(receiveData, 8, 2);
        if (!receiveVerify.equals(verifyStr)) {
            return false;
        }
        byte[] orderBytes = new byte[4];
        System.arraycopy(receiveData, 10, orderBytes, 0, orderBytes.length);
        int orderNum = Util.bytesToInt(orderBytes);
        //Log.i(TAG, "收到的序号 " + orderNum);
        if (verifyIndex > orderNum) {
            disorders++;
            return false;
        }
        verifyIndex = orderNum;
        return true;
    }

    /**
     * 接收 UDP 数据包
     */
    private void receiveUDPPacket() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                // 创建用于接收消息的DatagramPacket
                byte[] receiveData = new byte[1024];
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                while (isRunning) {
                    try {
                        // 接收服务端消息
                        socket.receive(receivePacket);
                        if (!verifyReceiveData(receiveData)) {
                            Log.e(TAG, "验证接收到的包错误");
                        } else {
                            receivePacketCount++;
                            long t = Util.bytesToLong(receiveData);
                            long spend = (Util.microTime() - t) / 1000;
                            if (spend < delayLow || delayLow == 0) {
                                delayLow = spend;
                            }
                            if (spend > delayHigh) {
                                delayHigh = spend;
                            }
                            delaySum += spend;
                            delay = delaySum / receivePacketCount;
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "获取服务器数据出错 " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    /**
     * 计算需要的时间，用来和实际使用时间比较
     */
    private long computeSpend() {
        return (long) (bufferSize * sendPacketCount * 8 / (float) (kbps) * 1000);
    }

    /**
     * 包装数据，主要是将时间戳以及包序号包装进去
     */
    private void wrapBuffer() {
        // 加入时间
        byte[] timeBytes = Util.longToBytes(Util.microTime());
        System.arraycopy(timeBytes, 0, buffer, 0, timeBytes.length);

        // 加入序号
        sendPacketCount++;
        byte[] orderByte = Util.intToBytes((int) sendPacketCount);
        System.arraycopy(orderByte, 0, buffer, 10, orderByte.length);
    }

    /**
     * 发送 UDP 数据包
     */
    private void sendUDPPacket() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                isSending = true;
                sendTime = Util.microTime();
                receiveTime = Util.microTime();
                // 创建用于发送消息的DatagramPacket
                InetSocketAddress address = new InetSocketAddress(host, port);
                DatagramPacket sendPacket = new DatagramPacket(buffer, buffer.length, address);
                while (isRunning && isSending) {
                    try {
                        wrapBuffer();
                        sendPacket.setData(buffer);
                        // 向服务端发送消息
                        socket.send(sendPacket);
                        sendKbps = (long) (bufferSize * sendPacketCount / ((float) (Util.microTime() - sendTime) / 1000 / 1000) * 8);

                        // 计算应该耗费时间，以及实际耗费时间
                        long shouldSpend = computeSpend();
                        long spend = Util.microTime() - sendTime;
                        //Log.i(TAG, "计算 " + shouldSpend + ", 实际 " + spend);
                        if (spend < shouldSpend) {
                            // 这里不适用 Thread.sleep() 而使用 TimeUnit 可以使睡眠控制到微秒级别
                            TimeUnit.MICROSECONDS.sleep(shouldSpend - spend);
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "发送 UDP 数据出错 " + e.getMessage());
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (time * 1000 * 1000 < Util.microTime() - sendTime) {
                        isSending = false;
                    }
                }
            }
        }).start();
    }

    /**
     * 显示统计信息
     */
    private void showStatistics() {
        int spend = (int) ((Util.microTime() - sendTime) / 1000 / 1000);
        // 状态
        stateView.setText(String.format("[%s]", isRunning ? "运行 " + spend + "s" : "空闲"));

        // 码率
        receiveKbps = (long) (bufferSize * receivePacketCount / ((float) (Util.microTime() - receiveTime) / 1000 / 1000) * 8);
        String kbpsStr = String.format("[发/收: %d/%d][目标:%d]", sendKbps, receiveKbps, kbps * 1000);
        kbpsView.setText(kbpsStr);

        // 数据
        String bufferStr = String.format("[发/收: %d/%d][单个:%d]", bufferSize * sendPacketCount, bufferSize * receivePacketCount, bufferSize);
        bufferView.setText(bufferStr);

        // 丢包
        long lostCount = sendPacketCount - receivePacketCount;
        float lostRate = lostCount / (float) sendPacketCount * 100;
        String lostStr = String.format("[发/丢/乱: %d/%d/%d][丢包率:%.2f%%]", sendPacketCount, lostCount, disorders, lostRate);
        lostView.setText(lostStr);

        // 延迟
        String delayStr = String.format("[小/大: %d/%d][平均:%d]", delayLow, delayHigh, delay);
        delayView.setText(delayStr);
    }

    /**
     * 启动定时器，用来计算发送数据的状态，包括数据包大小，延迟，丢包，码率等
     */
    private void startTimer() {
        if (timer == null) {
            timer = new Timer();
            TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    if (isRunning) {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                showStatistics();

                                // 等待两秒左右，然后停止 Socket
                                if ((time + 2) * 1000 * 1000 < Util.microTime() - sendTime) {
                                    stopUDPSocket();
                                }
                            }
                        });
                    }
                }
            };
            timer.schedule(task, 0, 1000);
        }
    }

    /**
     * 停止定时器
     */
    private void stopTimer() {
        isRunning = false;
        isSending = false;

        showStatistics();

        verifyIndex = 0;
        disorders = 0;

        sendPacketCount = 0;
        receivePacketCount = 0;

        sendTime = 0l;
        receiveTime = 0l;
        sendKbps = 0l;
        receiveKbps = 0l;

        delaySum = 0;
        delay = 0;
        delayLow = 0;
        delayHigh = 0;

        if (timer != null) {
            timer.cancel();
            timer.purge();
            timer = null;
        }
    }

    /**
     * 开始 UDP Socket
     */
    public void startUDPSocket() {
        if (!checkEdit()) {
            return;
        }
        testBtn.setText("停止测试");
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // 1.初始化DatagramSocket
                    socket = new DatagramSocket();
                    isRunning = true;
                    startTimer();
                    Log.i(TAG, "UDP Socket 已开始");
                    // 开启发送 UDP 数据线程
                    sendUDPPacket();
                    // 接收 UDP 数据
                    receiveUDPPacket();
                } catch (Exception e) {
                    isRunning = false;
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * 停止 UDP Socket
     */
    public void stopUDPSocket() {
        stopTimer();

        testBtn.setText("开始测试");
        Log.i(TAG, "UDP Socket 已停止");
        if (socket != null) {
            socket.close();
            socket = null;
        }
    }

    /**
     * 输入框验证
     */
    private boolean checkEdit() {
        host = hostEdit.getText().toString();
        port = Integer.valueOf(portEdit.getText().toString());

        if (TextUtils.isEmpty(host)) {
            Log.e(TAG, "服务器地址不能为空");
            return false;
        }
        if (port < 1024 || port > 65536) {
            Log.e(TAG, "端口必须是 1024~65536 之间的数字");
            return false;
        }
        return true;
    }
}
