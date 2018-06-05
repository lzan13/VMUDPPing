package com.vmloft.develop.app.demo.udptools.scan;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.vmloft.develop.app.demo.udptools.R;
import com.vmloft.develop.app.demo.udptools.Util;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

public class ScanFragment extends Fragment {

    private final String TAG = this.getClass().getSimpleName();

    @BindView(R.id.layout_edit_continuous) LinearLayout continuousLayout;
    @BindView(R.id.edit_port_start) EditText portStartEdit;
    @BindView(R.id.edit_port_end) EditText portEndEdit;
    @BindView(R.id.edit_port_more) EditText portMoreEdit;
    @BindView(R.id.recycler_view) RecyclerView recyclerView;

    private Activity activity;
    private ScanAdapter adapter;

    // UDP Socket 对象
    private DatagramSocket socket = null;
    private Map<Integer, PortBean> portMap = new ConcurrentHashMap<>();
    private List<PortBean> portList = new ArrayList<>();

    private boolean isContinuous = true;
    // 是否在运行中
    private boolean isRunning = false;
    private boolean isSending = false;
    // 运行时间
    private Timer timer;
    private long startTime;
    private long sendTime;

    private String verifyStr = "HI";
    // 发送数据 buffer
    private int byteSize = 64;
    private byte[] buffer;

    private String host = "123.57.172.41";
    private String portMore;
    private int portStart;
    private int portEnd;

    /**
     * 工厂方法
     */
    public static ScanFragment newInstance() {
        ScanFragment fragment = new ScanFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_scan, container, false);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        activity = getActivity();

        ButterKnife.bind(this, getView());

        init();
    }

    private void init() {
        buffer = new byte[byteSize];
        for (int i = 0; i < byteSize; i++) {
            buffer[i] = 0;
        }
        byte[] verifyBytes = verifyStr.getBytes();
        System.arraycopy(verifyBytes, 0, buffer, 8, verifyBytes.length);

        adapter = new ScanAdapter(activity, portList);
        recyclerView.setLayoutManager(new GridLayoutManager(activity, 3));
        recyclerView.addItemDecoration(new ScanItemDecoration(10));
        recyclerView.setAdapter(adapter);
    }

    @OnClick({ R.id.btn_change, R.id.btn_start })
    public void onClick(View view) {
        switch (view.getId()) {
        case R.id.btn_change:
            changeScanMode();
            break;
        case R.id.btn_start:
            startUDPSocket();
            break;
        }
    }

    private void changeScanMode() {
        if (isContinuous) {
            isContinuous = false;
            portMoreEdit.setVisibility(View.VISIBLE);
            continuousLayout.setVisibility(View.INVISIBLE);
        } else {
            isContinuous = true;
            portMoreEdit.setVisibility(View.INVISIBLE);
            continuousLayout.setVisibility(View.VISIBLE);
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
        byte[] portBytes = new byte[4];
        System.arraycopy(receiveData, 10, portBytes, 0, portBytes.length);
        int port = Util.bytesToInt(portBytes);
        PortBean portBean = portMap.get(port);
        if (portBean != null) {
            portBean.setOpen(true);
        }
        Log.i(TAG, "收到端口 " + port);
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
     * 包装数据，主要是将时间戳以及包序号包装进去
     */
    private void wrapBuffer(int port) {
        // 加入时间
        long time = Util.millisTime();
        byte[] timeBytes = Util.longToBytes(time);
        System.arraycopy(timeBytes, 0, buffer, 0, timeBytes.length);

        // 加入端口号
        byte[] orderByte = Util.intToBytes(port);
        System.arraycopy(orderByte, 0, buffer, 10, orderByte.length);
        PortBean portBean = new PortBean(port, time);
        portMap.put(port, portBean);
        Log.i(TAG, "发送 UDP 到端口 " + port);
    }

    private void startSendUDPPacket() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                isSending = true;
                sendTime = Util.millisTime();
                if (isContinuous) {
                    for (int port = portStart; port <= portEnd; port++) {
                        sendUDPPacket(port);
                        try {
                            Thread.sleep(200);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    String[] ports = portMore.split(",");
                    for (String pStr : ports) {
                        int port = Integer.valueOf(pStr);
                        if (port < 0 || port > 65536) {
                            break;
                        }
                        sendUDPPacket(port);
                        try {
                            Thread.sleep(200);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
                isSending = false;
            }
        }).start();
    }

    /**
     * 发送 UDP 数据包
     */
    private void sendUDPPacket(int port) {
        // 创建用于发送消息的DatagramPacket
        InetSocketAddress address = new InetSocketAddress(host, port);
        DatagramPacket sendPacket = new DatagramPacket(buffer, buffer.length, address);
        try {
            wrapBuffer(port);
            sendPacket.setData(buffer);
            // 向服务端发送消息
            socket.send(sendPacket);
        } catch (IOException e) {
            Log.e(TAG, "发送 UDP 数据出错 " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 开始 UDP Socket
     */
    public void startUDPSocket() {
        if (!checkEdit()) {
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // 1.初始化DatagramSocket
                    socket = new DatagramSocket();
                    isRunning = true;
                    startTime = Util.millisTime();
                    startTimer();
                    Log.i(TAG, "UDP Socket 已开始");
                    startSendUDPPacket();
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
        Log.i(TAG, "UDP Socket 已停止");
        if (socket != null) {
            socket.close();
            socket = null;
        }
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
                        refresh();
                        // 等待两秒左右，然后停止 Socket
                        if (startTime + 2 * 1000 < Util.millisTime() - sendTime) {
                            stopUDPSocket();
                        }
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
        refresh();

        isRunning = false;
        isSending = false;

        startTime = 0l;
        sendTime = 0l;

        portMap.clear();
        portList.clear();

        if (timer != null) {
            timer.cancel();
            timer.purge();
            timer = null;
        }
    }

    /**
     * 刷新界面
     */
    private void refresh() {
        if (adapter != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    portList.clear();
                    portList.addAll(portMap.values());
                    adapter.notifyDataSetChanged();
                }
            });
        }
    }

    /**
     * 输入框验证
     */
    private boolean checkEdit() {
        portMore = portMoreEdit.getText().toString();
        String portStartStr = portStartEdit.getText().toString();
        String portEndStr = portEndEdit.getText().toString();
        if (isContinuous) {
            if (TextUtils.isEmpty(portStartStr) || TextUtils.isEmpty(portEndStr)) {
                Log.e(TAG, "端口必须是 1024~65536 之间的数字");
                return false;
            }
            portStart = Integer.valueOf(portStartStr);
            portEnd = Integer.valueOf(portEndStr);

            if (portStart > portEnd || (portStart < 1024 || portEnd > 65536)) {
                Log.e(TAG, "端口必须是 1024~65536 之间的数字");
                return false;
            }
        } else {
            if (TextUtils.isEmpty(portMore)) {
                Log.e(TAG, "端口不能为空");
                return false;
            }
        }
        return true;
    }
}
