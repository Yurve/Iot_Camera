package com.sample.useopencvwithcmake;

import android.graphics.Bitmap;
import android.provider.ContactsContract;
import android.util.Base64;

import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;
import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ImageProcess {
    Mat matResult;

    //생성자
    public ImageProcess(Mat mat){
        matResult = mat;
    }


    //사진을 저장하는 메소드 
    public String saveImage() {

        //현재 시간
        String date = dateName(System.currentTimeMillis());

        //사진의 색 변환 (원래 openCv는 반전되어있음 RGB가 아니라 BGR로 되어있음 갤러리에 저장할 때 사용)
        // Imgproc.cvtColor(matResult, matResult, Imgproc.COLOR_BGR2RGBA);

        //매트릭스 객체 비트맵 변환
        Bitmap bitmap = Bitmap.createBitmap(matResult.cols(), matResult.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(matResult, bitmap);

        //비트맵을 base64로 인코딩
        String base64String = bitToString(bitmap);

        //json 으로 만들기
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("date", date);
            jsonObject.put("eventType", "face");
            jsonObject.put("picture", base64String);  //현재 너무 길어서 그런지 못보냄
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return new Gson().toJson(jsonObject);
    }

    //날짜, 시간을 구하는 메소드
    private String dateName(long dateTaken) {
        Date date = new Date(dateTaken);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH.mm.ss", Locale.KOREA);
        return dateFormat.format(date);
    }

    //비트맵 객체를 문자열로 변환하는 메소드
    private String bitToString(Bitmap bitmap) {
        //바이트 보낼 통로
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        //비트맵을 압축해서 전송
        bitmap.compress(Bitmap.CompressFormat.JPEG, 1, byteArrayOutputStream);

        //바이트 배열로 받기
        byte[] image = byteArrayOutputStream.toByteArray();

        //String 으로 반환
        return Base64.encodeToString(image, Base64.NO_WRAP);
    }

}
