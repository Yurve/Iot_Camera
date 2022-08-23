package com.sample.useopencvwithcmake;

import android.graphics.Bitmap;
import android.util.Base64;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

//이미지 가공을 위한 메소드
public class ImageProcess {
    Mat matResult;

    //생성자
    public ImageProcess(Mat mat){
        matResult = mat;
    }

    //현재시간을 구하는 메소드
    public String saveTime(){
        //현재 시간
        return dateName(System.currentTimeMillis());
    }


    //사진을 저장하는 메소드 
    public String saveImage() {

        //사진의 색 변환 (원래 openCv는 반전되어있음 RGB가 아니라 BGR로 되어있음 갤러리에 저장할 때 사용)
        // Imgproc.cvtColor(matResult, matResult, Imgproc.COLOR_BGR2RGBA);

        //매트릭스 객체 비트맵 변환
        Bitmap bitmap = Bitmap.createBitmap(matResult.cols(), matResult.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(matResult, bitmap);

        //원본파일 1024*576 인데 이상태로 보내면 너무 커서 비율에 맞게 크기를 줄인다.
        bitmap = Bitmap.createScaledBitmap(bitmap,533,300,true);


      return bitToString(bitmap);

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
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream);

        //바이트 배열로 받기
        byte[] image = byteArrayOutputStream.toByteArray();

        //String 으로 반환
         return Base64.encodeToString(image, Base64.NO_WRAP);
    }


    //시간 차이를 비교하는 메소드
    public boolean diff_time(Long current_date, Long past_date) {
        //초기에는 past_date 가 없다. 그냥 true 리턴하자
        if (past_date == null) {
            return true;
        }

        //시간차이 구하기
        long difference = Math.abs(current_date - past_date);
        //3초 이내라면 사진 전송을 하지 않는다.
        if(difference <3000 ){
            return false;
        }else return true;
    }
}
