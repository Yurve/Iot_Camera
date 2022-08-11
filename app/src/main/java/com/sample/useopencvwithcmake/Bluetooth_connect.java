package com.sample.useopencvwithcmake;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

//블루투스 연결하는 클래스
public class Bluetooth_connect {

    Context context;

    //블루투스
    private BluetoothAdapter bluetoothAdapter;
    private Set<BluetoothDevice> PairedBluetoothDevices;

    private ConnectedBluetoothThread connectedBluetoothThread;
    private BluetoothDevice bluetoothDevice;

    private boolean state = true;
    public int BT_REQUEST_ENABLE = 1;
    final private static UUID BT_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //범용 고유 식별자

    //생성자
    public Bluetooth_connect(Context context) {
        this.context = context;
    }

    //블루투스 키기
    public void bluetoothOn() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    //켰는지 확인하기
    public void bluetoothCheck() throws SecurityException {
        if (bluetoothAdapter == null) {
            Toast.makeText(context, "블루투스를 지원하지 않는 기기 ", Toast.LENGTH_SHORT).show();
        } else {
            Intent bluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            ((MainActivity) context).startActivityForResult(bluetoothIntent, BT_REQUEST_ENABLE);
        }
    }

    //페어링 된 기기 보여주기
    public void listPairedDevices() throws SecurityException {
        if (bluetoothAdapter.isEnabled()) {
            PairedBluetoothDevices = bluetoothAdapter.getBondedDevices();
            if (PairedBluetoothDevices.size() > 0) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle("장치 선택");

                List<String> listBluetoothDevices = new ArrayList<>();
                for (BluetoothDevice device : PairedBluetoothDevices) {
                    listBluetoothDevices.add(device.getName());
                }

                final CharSequence[] items = listBluetoothDevices.toArray(new CharSequence[listBluetoothDevices.size()]);
                listBluetoothDevices.toArray(new CharSequence[listBluetoothDevices.size()]);

                builder.setItems(items, (dialog, item) -> connectSelectDevice(items[item].toString()));
                AlertDialog dialog = builder.create();
                dialog.show();
            } else {
                Toast.makeText(context, "페어링 된 기기가 없습니다", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(context, "블루투스가 비활성화 상태 입니다", Toast.LENGTH_SHORT).show();
            bluetoothCheck();
            listPairedDevices();
        }
    }

    //클릭된 디바이스 블루투스 연결
    public void connectSelectDevice(String selectedDeviceName) throws SecurityException{
        for(BluetoothDevice device : PairedBluetoothDevices){
            if(selectedDeviceName.equals(device.getName())){
                bluetoothDevice = device;
                break;
            }
        }
        try{
            BluetoothSocket bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(BT_UUID);
            bluetoothSocket.connect();
            connectedBluetoothThread = new ConnectedBluetoothThread(bluetoothSocket);
            connectedBluetoothThread.start();
            Toast.makeText(context,"연결 성공!",Toast.LENGTH_SHORT).show();
        }catch (IOException e){
            Toast.makeText(context,"블루투스 연결 중 오류 발생",Toast.LENGTH_SHORT).show();
        }
    }

    public void close() throws IOException{
        connectedBluetoothThread.cancel();
    }

    public void write(String str) throws IOException{
        connectedBluetoothThread.write(str);
    }

    public boolean checkThread() {
        if (connectedBluetoothThread == null) {
            state = false;
        }
        return state;
    }

}
