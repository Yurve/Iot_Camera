package com.sample.useopencvwithcmake;

import android.bluetooth.BluetoothSocket;

import java.io.IOException;
import java.io.OutputStream;

//블루투스 스레드를 만드는 클래스, 라즈베리파이에서 값을 받아올일이 없기 때문에 inputStream 은 지웠음.
public class ConnectedBluetoothThread extends Thread {
    private final BluetoothSocket mmSocket;
    private final OutputStream outputStream;

    //생성자
    public ConnectedBluetoothThread(BluetoothSocket socket) throws IOException {
        mmSocket = socket;
        outputStream = socket.getOutputStream();
    }

    public void write(String string) throws IOException {
        //바이트 변환
        byte[] bytes = string.getBytes();
        //전송
        outputStream.write(bytes);
    }

    public void cancel() throws IOException {
        mmSocket.close();
    }


}
