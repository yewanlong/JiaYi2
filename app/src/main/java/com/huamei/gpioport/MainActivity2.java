package com.huamei.gpioport;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

import com.alibaba.fastjson.JSONObject;
import com.xuhao.android.libsocket.sdk.ConnectionInfo;
import com.xuhao.android.libsocket.sdk.OkSocketOptions;
import com.xuhao.android.libsocket.sdk.SocketActionAdapter;
import com.xuhao.android.libsocket.sdk.bean.IPulseSendable;
import com.xuhao.android.libsocket.sdk.bean.ISendable;
import com.xuhao.android.libsocket.sdk.bean.OriginalData;
import com.xuhao.android.libsocket.sdk.connection.IConnectionManager;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;

import static com.xuhao.android.libsocket.sdk.OkSocket.open;

public class MainActivity2 extends YBaseActivity implements View.OnClickListener {

    private String type;
    private String gpioOutOpen = "203";
    private String gpioOutClose = "234";
    private IConnectionManager mManager;
    private ConnectionInfo mInfo;
    private OkSocketOptions mOkOptions;
    private int msgId;

    @Override
    protected int getContentView() {
        return R.layout.activity_main2;
    }

    @Override
    protected void initView() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN); //设置全屏的flag
        app.addActivity(this);
        mInfo = new ConnectionInfo(HttpUtils.TCP_IP, HttpUtils.TCP_PRO_IP);
        mOkOptions = new OkSocketOptions.Builder(OkSocketOptions.getDefault())
                .setReconnectionManager(new NoneReconnect())
                .setSinglePackageBytes(1024)
                .build();
        mManager = open(mInfo, mOkOptions);
        mManager.registerReceiver(adapter);
        mManager.connect();
        checkPermission(new String[]{Manifest.permission.READ_PHONE_STATE}, 199);
    }

    public void checkPermission(String[] permissions, int REQUEST_FOR_PERMISSIONS) {
        if (lacksPermissions(permissions)) {
            ActivityCompat.requestPermissions(this,
                    permissions,
                    REQUEST_FOR_PERMISSIONS);
        } else {
            HttpUtils.IMEI = "866960027556866";
//            HttpUtils.IMEI = getSubscriberId(this);
        }
    }

    @Override
    protected void initData() {
        gpioOut(gpioOutOpen);
        gpioOut(gpioOutClose);
    }


    @Override
    protected void initListener() {
        findViewById(R.id.button_open).setOnClickListener(this);
        findViewById(R.id.button_close).setOnClickListener(this);
        findViewById(R.id.button).setOnClickListener(this);

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.button_close:
                controlGpio(true);
                break;
            case R.id.button_open:
                controlGpio(false);
                break;
            case R.id.button:
                startActivity(new Intent(this, MainActivity.class));
                finish();
                break;
        }
    }

    private void gpioOut(String gpio) {
        try {
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream dos = new DataOutputStream(process.getOutputStream());
            //打开gpio引脚74，即status_led连接的引脚
            dos.writeBytes("echo " + gpio + " > /sys/class/gpio/export" + "\n");
            dos.flush();
            //设置引脚功能为输出
            dos.writeBytes("echo out > /sys/class/gpio/gpio" + gpio + "/direction" + "\n");
            dos.flush();
            dos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void controlGpio(boolean isOpen) {
        DataOutputStream dos = null;
        try {
            Process process = Runtime.getRuntime().exec("su");
            dos = new DataOutputStream(process.getOutputStream());
            if (isOpen)
                dos.writeBytes("echo 1 > /sys/class/gpio/gpio" + gpioOutOpen + "/value" + "\n");//开
            else
                dos.writeBytes("echo 0 > /sys/class/gpio/gpio" + gpioOutClose + "/value" + "\n");//关
            dos.flush();
            dos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private Handler mHandler = new Handler();
    private Runnable mRunnableError = new Runnable() {
        @Override
        public void run() {
            mManager.connect();
        }
    };
    private Runnable mRunnableCSQ = new Runnable() {
        @Override
        public void run() {
            socketSend(HttpUtils.getCSQ(msgId, HttpUtils.IMEI));
            msgId++;
            mHandler.postDelayed(mRunnableCSQ, 300000);
        }
    };
    private SocketActionAdapter adapter = new SocketActionAdapter() {

        @Override
        public void onSocketConnectionSuccess(Context context, ConnectionInfo info, String action) {
            Log.i("ywl", "onSocketConnectionSuccess:");
            socketSend(HttpUtils.getCheckIn2(0, HttpUtils.IMEI));
            mHandler.removeCallbacks(mRunnableCSQ);
            mHandler.postDelayed(mRunnableCSQ, 1000);
        }

        @Override
        public void onSocketDisconnection(Context context, ConnectionInfo info, String action, Exception e) {
            if (e != null) {
                mHandler.postDelayed(mRunnableError, 20000);
            }
        }

        @Override
        public void onSocketConnectionFailed(Context context, ConnectionInfo info, String action, Exception e) {
            mHandler.postDelayed(mRunnableError, 20000);
        }

        //接收
        @Override
        public void onSocketReadResponse(Context context, ConnectionInfo info, String action, OriginalData data) {
            super.onSocketReadResponse(context, info, action, data);
            String str = new String(data.getBodyBytes(), Charset.forName("utf-8"));
            ReadResponse(str);
            Log.i("ywl", "onSocketReadResponse:" + str);
        }

        @Override
        public void onSocketWriteResponse(Context context, ConnectionInfo info, String action, ISendable data) {
            super.onSocketWriteResponse(context, info, action, data);
        }

        @Override
        public void onPulseSend(Context context, ConnectionInfo info, IPulseSendable data) {
            super.onPulseSend(context, info, data);
        }
    };

    private void socketSend(String tcpMap) {
        if (!mManager.isConnect()) {
            mManager.connect();
        } else {
            mManager.send(new HandShake(tcpMap));
        }
    }

    private void ReadResponse(String str) {
        JSONObject jsonObject = new JSONObject();
        String[] sourceStrArray = str.split("&");
        for (int i = 0; i < sourceStrArray.length; i++) {
            String[] jsonStr = sourceStrArray[i].split("=");
            if (jsonStr.length == 2) {
                jsonObject.put(jsonStr[0], jsonStr[1]);
            }
        }
        switch (jsonObject.getString("Action")) {
            case "Door":
                type = jsonObject.getString("type");
                if ("1".equals(type)) {
                    controlGpio(false);
                } else {
                    controlGpio(true);
                }
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 199:
                for (int i = 0; i < grantResults.length; i++) {
                    if (i == 0 && grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        HttpUtils.IMEI = getSubscriberId(this);
                    }
                }
                break;
            default:
                break;
        }
    }

}
