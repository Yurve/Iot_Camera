package com.sample.useopencvwithcmake;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;





public class PermissionSupport {

    private final Context context;
    private final Activity activity;


  //  private List permissionList;
    //권한 요청시 발생하는 창에 대한 결과값을 받기 위해 지정해주는 int 형
    //원하는 임의의 숫자 지정
   public final static int MULTIPLE_PERMISSIONS = 1;  //요청에 대한 결과값 확인을 위해 RequestCode를 final로 정의

    //생성자
    public PermissionSupport(Activity activity, Context context) {
        this.activity = activity;
        this.context = context;
    }



    //권한 확인
    public void onCheckPermission(){
        if(ActivityCompat.checkSelfPermission(context,Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
        || ActivityCompat.checkSelfPermission(context,Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
        || ActivityCompat.checkSelfPermission(context,Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            if(ActivityCompat.shouldShowRequestPermissionRationale(activity,Manifest.permission.BLUETOOTH_CONNECT)){
                Toast.makeText(context,"앱 실행을 위해서는 권한을 설정해야 합니다.",Toast.LENGTH_SHORT).show();
            }else{
                ActivityCompat.requestPermissions(activity,new String[]{Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE},MULTIPLE_PERMISSIONS);
            }
        }
    }


}
