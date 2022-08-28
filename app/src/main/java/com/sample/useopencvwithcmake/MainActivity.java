package com.sample.useopencvwithcmake;


import androidx.appcompat.app.AppCompatActivity;

import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.Toast;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONObject;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Rect;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import com.microsoft.signalr.HubConnection;
import com.microsoft.signalr.HubConnectionBuilder;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableSource;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.functions.Supplier;
import io.reactivex.rxjava3.observers.DisposableObserver;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;


public class MainActivity extends AppCompatActivity
        implements CameraBridgeViewBase.CvCameraViewListener2 {
    private static final String TAG = "opencv";

    //사진 매트릭스 객체
    private Mat matResult;

    //SignalR 통신
    private HubConnection hubConnection;

    //화면에 보여주는 카메라뷰
    private CameraBridgeViewBase mOpenCvCameraView;

    //블루투스 통신 클래스
    private Bluetooth_connect bluetooth_connect;

    //이미지처리 클래스
    private ImageProcess imageProcess;

    //비동기
    private CompositeDisposable disposables;

    //json 객체 만들기
    private JSONObject jsonObject;

    //MQTT 클라이언트 만들기
    private MqttClient mqttClient = null;

    // public native void ConvertRGBtoGray(long matAddrInput, long matAddrResult);
    // OpenCV 네이티브 라이브러리와 C++코드로 빌드된 라이브러리를 읽음

    public native long loadCascade(String cascadeFileName);

    public native double[] detect(long cascadeClassifier_face,
                                  long cascadeClassifier_eye, long matAddrInput, long matAddrResult);


    public long cascadeClassifier_face = 0;
    public long cascadeClassifier_eye = 0;

    public int CURRENT_TIME = 1;

    public String macId;

    //assets 파일들은 바로 쓸 수 없다. 불러오는 메소드
    private void copyFile(String filename) {

        AssetManager assetManager = this.getAssets();
        File outputFile = new File(getFilesDir() + "/" + filename);

        try {
            InputStream inputStream = assetManager.open(filename);
            OutputStream outputStream = new FileOutputStream(outputFile);

            byte[] buffer = new byte[1024];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            inputStream.close();
            outputStream.flush();
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void read_cascade_file() {
        copyFile("haarcascade_frontalface_alt.xml");
        copyFile("haarcascade_eye_tree_eyeglasses.xml");

        Log.d(TAG, "read_cascade_file:");

        cascadeClassifier_face = loadCascade(getFilesDir().getAbsolutePath() + "/haarcascade_frontalface_alt.xml");
        Log.d(TAG, "read_cascade_file:");

        cascadeClassifier_eye = loadCascade(getFilesDir().getAbsolutePath() + "/haarcascade_eye_tree_eyeglasses.xml");
    }

    static {

        System.loadLibrary("opencv_java4");
        System.loadLibrary("native-lib");
    }


    private final BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            if (status == LoaderCallbackInterface.SUCCESS) {
                mOpenCvCameraView.enableView();
            } else {
                super.onManagerConnected(status);
            }
        }
    };

    //화면이 세로모드에서 가로모드로 전환 시 onCreate함수가 다시 호출된다.
    // 만약 전역변수를 설정하고 그 값을 유지하며 항상 사용해야 하는 경우라도 화면이 세로모드에서 가로모드로 변경될 경우 전역변수에 설정한 값이 모두 초기화 된다. 이런 경우 변경된 값을 유지하고 싶다면  savedInstanceState을 이용하는 것이 좋다.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //권한 확인하기
        permissionCheck();

        //signalR 서버 접속하기
        String input = "http://***************";
        hubConnection = HubConnectionBuilder.create(input).build();
        hubConnection.start().blockingAwait();

        //mqtt 클라이언트 아이디 설정 - MAC 주소
        MacAddrThread macAddrThread = new MacAddrThread();
        macAddrThread.start();
        try {
            macAddrThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        macId = macAddrThread.macAddress;


        //mqtt 클라이언트 만들기
        String ServerAddress = "tcp://****************";
        try {
            mqttClient = new MqttClient(ServerAddress, macId, null);
            mqttClient.connect();
        } catch (MqttException e) {
            e.printStackTrace();
        }


        //블루투스 클래스 생성
        bluetooth_connect = new Bluetooth_connect(this);

        //블루투스 키기 및 확인
        bluetooth_connect.bluetoothOn();
        bluetooth_connect.bluetoothCheck();

        //페어링 된 기기 알람띄우기
        bluetooth_connect.listPairedDevices();

        //비동기 클래스
        disposables = new CompositeDisposable();


        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);


        mOpenCvCameraView = findViewById(R.id.activity_surface_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.setCameraIndex(1); // front-camera(1),  back-camera(0)

        //비동기로 라즈베리파이 제어
        sensorScheduler();

        //mqtt 잘보냈는지 확인해보자
        try {
            mqttClient.subscribe("aics/event/topic/1");
            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    Log.d("MQTTService", "Connection Lost");
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    Log.d("MQTTService", "Message Arrived : " + message.toString());
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    Log.d("MQTTService", "Delivery Complete");
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }

        RxJavaPlugins.setErrorHandler(e -> Log.e("RxJava_HOOK", "Undeliverable exception received, not sure what to do" + e.getMessage()));
    }

    //권한 체크
    private void permissionCheck() {

        //객체 생성
        //권한 요청 클래스
        PermissionSupport permissionSupport = new PermissionSupport(this, this);

        permissionSupport.onCheckPermission();
    }

    // permissionCheck 대한 결과 값 받고나서
    @Override
    public void onRequestPermissionsResult(int requestCode, @androidx.annotation.NonNull String[] permissions, @androidx.annotation.NonNull int[] grantResults) {
        if (requestCode == PermissionSupport.MULTIPLE_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "권한 확인", Toast.LENGTH_SHORT).show();
            } else {
                Log.d(TAG, "권한 취소");
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    //구독 메소드 <- sendImage()라는 메소득 진행되면 알아서 이 메소드도 실행
    public void onScheduler() {
        disposables.add(sendImage()
                //run on a background thread
                .subscribeOn(Schedulers.io())
                // Be notified on the main thread)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableObserver<String>() {
                    @Override
                    public void onNext(@NonNull String msg) {
                        Log.d(TAG, "onNext(" + msg + ")");

                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        Log.e(TAG, "onError()", e);
                    }

                    @Override
                    public void onComplete() {
                        Log.d(TAG, "onComplete()");
                    }
                }));
    }

    //이전 시간과 현재시간을 비교해야함.
    public Long current_date;
    public Long past_date;

    //비동기로 사진 정보 전송 메소드
    public Observable<String> sendImage() {
        return Observable.defer(new Supplier<ObservableSource<? extends String>>() {
            @Override
            public ObservableSource<? extends String> get() throws Throwable {
                // Do some long running operation


                //시간 비교를 위해 구분 1일때는 current_date에 저장, 0일 떄는 past_date에 저장된다.
                //아래 '//전송에 성공한 경우, 1-> 0으로 바꾸거나, 0 -> 1 로 바꾸어서 사진이 저장되는 시간을 다른 곳에 저장한다.'라고 저장한 함수가
                //실행 되면 다음 사진이 저장될 때 저장하는 시간을 다른 곳에 넣는다.(current_date -> past_date) or (past_date -> current_date)
                if (CURRENT_TIME == 1) {
                    //현재시간을 구하는 함수, 정확히는 핸드폰이 켜지고 난 이후 부터의 절대적인 시간, 시간 비교를 할 떄 주로 사용된다.
                    current_date = SystemClock.elapsedRealtime();
                } else {
                    past_date = SystemClock.elapsedRealtime();
                }

                //둘의 시간을 비교하는 메소드
                boolean ok = imageProcess.diff_time(current_date, past_date);
                if (!ok) {
                    return Observable.just("pass");
                }


                String date = imageProcess.saveTime();
                String encodedImage = imageProcess.saveImage();
                String type = "eventType";

                //json 객체로 전송
                jsonObject = new JSONObject();
                jsonObject.put("addr", macId);
                jsonObject.put("date", date);
                jsonObject.put("type", type);
                jsonObject.put("Image", encodedImage);

                //json 객체 -> 문자열
                String jsonString = jsonObject.toString();

                //mqtt 전송
                mqttClient.publish("aics/topic/event", new MqttMessage(jsonString.getBytes(StandardCharsets.UTF_8)));


                Log.d("JSONObject", "전송 성공" + jsonObject);


                //전송에 성공한 경우, 1-> 0으로 바꾸거나, 0 -> 1 로 바꾸어서 사진이 저장되는 시간을 다른 곳에 저장한다.
                if (CURRENT_TIME == 1) {
                    CURRENT_TIME = 0;
                } else {
                    CURRENT_TIME = 1;
                }

                return Observable.just(encodedImage);
            }
        });
    }

    //서버에서 서보모터 제어 신호 비동기처리, 구독 메소드로, servoToPi4()라는 메소드가 실행되면 이 메소드도 실행
    public void sensorScheduler() {
        disposables.add(serverToPi4()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableObserver<String>() {
                    @Override
                    public void onNext(@NonNull String s) {
                        Log.d(TAG, "onNext(" + s + ")");
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        Log.e(TAG, "onError()", e);
                    }

                    @Override
                    public void onComplete() {
                        Log.d(TAG, "onComplete()");
                    }
                })
        );
    }

    //서버에서 signalR로 받아서 라즈베리파이로 블루투스 송신
    public Observable<String> serverToPi4() {
        return Observable.defer(new Supplier<ObservableSource<? extends String>>() {
            @Override
            public ObservableSource<? extends String> get() throws Throwable {
                // Do some long running operation

                hubConnection.on("ReceiveMessage", (user, message) -> {
                    //블루투스 제어
                    if (bluetooth_connect.checkThread()) {
                        try {
                            bluetooth_connect.write(message);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }, String.class, String.class);

                return Observable.just("bluetooth success");
            }
        });
    }


    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "onResume :: Internal OpenCV library not found.");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "onResume :: OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }


    public void onDestroy() {
        //화면 보여주기 끊기
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();

        //블루투스 연결 끊기
        try {
            bluetooth_connect.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //SignalR(서버로 부터 모터제어 수신 터널) 끊기
        hubConnection.stop();

        //rxjava(비동기) 통로 비우기
        disposables.clear();

        //mqtt(사진 및 사진 정보 송신 터널) 끊기
        try {
            mqttClient.disconnect();
            mqttClient.close();
        } catch (MqttException e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }

    // implements CameraBridgeViewBase.CvCameraViewListener2 메소드
    /* camera started : 카메라 프리뷰가 시작되면 호출된다.
       camera viewStopped : 카메라 프리뷰가 어떤 이유로 멈추면 호출된다.
       camera frame : 프레임 전달이 필요한 경우 호출 된다.
    */
    @Override
    public void onCameraViewStarted(int width, int height) {

    }

    @Override
    public void onCameraViewStopped() {

    }


    //동그라미 검출 메소드
    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        //inputFrame 을 매트릭스 객체로 변환, openCV에 이용하려고
        Mat matInput = inputFrame.rgba();

        //화면에 보여줄 matResult
        if (matResult == null)

            matResult = new Mat(matInput.rows(), matInput.cols(), matInput.type());


        Core.flip(matInput, matInput, 1);

        double[] doubles;

        //이 배열 안에는  (가장 왼쪽의)X좌표,(가장 왼쪽의)Y좌표,넓이,높이,얼굴의 갯수 가 들어있다.
        doubles = detect(cascadeClassifier_face, cascadeClassifier_eye, matInput.getNativeObjAddr(), matResult.getNativeObjAddr());

        //배열에있는 애들 꺼내오기 + rect 객체에는 int 들어감
        int x = (int) doubles[0];
        int y = (int) doubles[1];
        int width = (int) doubles[2];
        int height = (int) doubles[3];

        //사각형의 좌표 값 저장
        Rect rect = new Rect(x, y, width, height);

        //위의 사각형 좌표(ROI) 만큼 매트릭스를 자름
        Mat faceROI = new Mat(matResult, rect);
        //매트릭스 객체를 가공하는 클래스 (base64를 하거나, 사진의 날짜를 구하는 메소드 등이 있다.)
        imageProcess = new ImageProcess(faceROI);

        //가장 마지막 배열에 얼굴의 갯수가 들어있음.
        double faceSize = doubles[4];

        if (faceSize > 0) {
            onScheduler();
        }

        return matResult;
    }


    protected List<? extends CameraBridgeViewBase> getCameraViewList() {
        return Collections.singletonList(mOpenCvCameraView);
    }


    protected void onCameraPermissionGranted() {
        List<? extends CameraBridgeViewBase> cameraViews = getCameraViewList();
        if (cameraViews == null) {
            return;
        }
        for (CameraBridgeViewBase cameraBridgeViewBase : cameraViews) {
            if (cameraBridgeViewBase != null) {
                cameraBridgeViewBase.setCameraPermissionGranted();
                read_cascade_file();
            }
        }
    }


    @Override
    protected void onStart() {
        super.onStart();
        onCameraPermissionGranted();
    }

}

