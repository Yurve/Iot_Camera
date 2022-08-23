package com.sample.useopencvwithcmake;

import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketAddress;

public class MacAddrThread extends Thread{
    String macAddress;

    @Override
    public void run() {
        // https://stackoverflow.com/a/41822127
       try{
           Socket socket = new Socket();
           SocketAddress endpoint = new InetSocketAddress("**********", 8085);
           socket.connect(endpoint);
           InetAddress localAddress = socket.getLocalAddress();

           NetworkInterface ni = NetworkInterface.getByInetAddress(localAddress);
           byte[] mac = ni.getHardwareAddress();

           StringBuilder s = new StringBuilder();
           if (mac != null) {
               for (int j = 0; j < mac.length; j++) {
                   String part = String.format("%02X%s", mac[j], (j < mac.length - (1)) ? ":" : "");
                   s.append(part);
               }
               String macAddress = s.toString();
               this.macAddress = macAddress;
               Log.d("MAC", " MAC: " + macAddress);
           } else {
               Log.d("MAC", " Address doesn't exist or is not accessible.");
           }
           socket.close();
       } catch (IOException e) {
           e.printStackTrace();
       }
    }
}
