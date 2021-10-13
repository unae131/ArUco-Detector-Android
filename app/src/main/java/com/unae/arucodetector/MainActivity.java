package com.unae.arucodetector;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.Toast;

import java.util.Collections;
import java.util.List;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.aruco.Aruco;
import org.opencv.aruco.DetectorParameters;
import org.opencv.aruco.Dictionary;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import static android.Manifest.permission.CAMERA;

import androidx.appcompat.app.AlertDialog;

public class MainActivity extends Activity implements CvCameraViewListener2 {

    private Mat cameraMatrix;
    private Mat distCoeffs;
    private Mat rgb;
    private Dictionary dictionary;
    private DetectorParameters parameters;

    public native void DetectMarkers(long matAddrInput, long matAddrResult, long params, long dictAddr, long camMatAddr, long distAddr);
//    public native boolean ReadCameraParameters(long camMatAddr, long distAddr);

    static {
        System.loadLibrary("opencv_java4");
        System.loadLibrary("native-lib");
    }

    private CameraBridgeViewBase camera;

    private final BaseLoaderCallback loaderCallback = new BaseLoaderCallback(this){
        @Override
        public void onManagerConnected(int status){
            if(status == LoaderCallbackInterface.SUCCESS){
                String message = "";

                if (loadCameraParams())
                    message = "getString(R.string.success_ocv_loading)";
                else
                    message = "getString(R.string.error_camera_params)";

                camera.enableView();

                Toast.makeText(MainActivity.this,  message,  Toast.LENGTH_SHORT).show();
            }
            else {
                super.onManagerConnected(status);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);

        camera = findViewById(R.id.activity_surface_view);
        camera.setVisibility(SurfaceView.VISIBLE);
        camera.setCvCameraViewListener(this);
        camera.setCameraIndex(0);
    }

    @Override
    public void onResume(){
        super.onResume();

        if(OpenCVLoader.initDebug())
            loaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        else
            Toast.makeText(this, "getString(R.string.error_native_lib)", Toast.LENGTH_LONG).show();
    }

    private boolean loadCameraParams(){
        cameraMatrix = Mat.eye(3, 3, CvType.CV_64FC1);
        distCoeffs = Mat.zeros(1, 5, CvType.CV_64FC1);

        double camData[][] = new double[][]{ new double[]{3.1601688343613614e+03, 0., 2.0316219018707357e+03},
                                                new double[]{0., 3.1579383184227572e+03, 1.5325905756651362e+03},
                                                new double[]{0., 0., 1.}};
        double distData[] = new double[]{4.6129050021544923e-03, 6.3054990312427894e-02,
                                        -1.1576434624863847e-04, -1.7215787506533371e-03,
                                        -2.4976694026384097e-02};
        for (int i=0;i<3;i++)
            cameraMatrix.put(i,0, camData[i]);
        distCoeffs.put(0, 0, distData);

        return true;
//        return ReadCameraParameters(cameraMatrix.getNativeObjAddr(), distCoeffs.getNativeObjAddr()); //CameraParameters.tryLoad(this, cameraMatrix, distCoeffs);
    }

    @Override
    public void onPause(){
        super.onPause();

        if(camera != null)
            camera.disableView();
    }

    @Override
    public void onDestroy(){
        super.onDestroy();

        if (camera != null)
            camera.disableView();
    }

    @Override
    public void onCameraViewStarted(int width, int height){
        rgb = new Mat();
        parameters = DetectorParameters.create();
        dictionary = Aruco.getPredefinedDictionary(Aruco.DICT_5X5_250);

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame){
//        Log.d("camMat", String.valueOf(cameraMatrix.get(0,0)[0]));
//        Log.d("coeffs", distCoeffs.toString());

        DetectMarkers(inputFrame.rgba().getNativeObjAddr(), rgb.getNativeObjAddr(),
                parameters.getNativeObjAddr(), dictionary.getNativeObjAddr(),
                cameraMatrix.getNativeObjAddr(), distCoeffs.getNativeObjAddr());

        return rgb;
    }

    @Override
    public void onCameraViewStopped(){
        rgb.release();
    }

    protected List<? extends CameraBridgeViewBase> getCameraViewList() {
        return Collections.singletonList(camera);
    }

    //여기서부턴 퍼미션 관련 메소드
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 200;


    protected void onCameraPermissionGranted() {
        List<? extends CameraBridgeViewBase> cameraViews = getCameraViewList();
        if (cameraViews == null) {
            return;
        }
        for (CameraBridgeViewBase cameraBridgeViewBase: cameraViews) {
            if (cameraBridgeViewBase != null) {
                cameraBridgeViewBase.setCameraPermissionGranted();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        boolean havePermission = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
                havePermission = false;
            }
        }
        if (havePermission) {
            onCameraPermissionGranted();
        }
    }

    @Override
    @TargetApi(Build.VERSION_CODES.M)
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            onCameraPermissionGranted();
        }else{
            showDialogForPermission("앱을 실행하려면 퍼미션을 허가하셔야합니다.");
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }


    @TargetApi(Build.VERSION_CODES.M)
    private void showDialogForPermission(String msg) {

        AlertDialog.Builder builder = new AlertDialog.Builder( MainActivity.this);
        builder.setTitle("알림");
        builder.setMessage(msg);
        builder.setCancelable(false);
        builder.setPositiveButton("예", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id){
                requestPermissions(new String[]{CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
            }
        });
        builder.setNegativeButton("아니오", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface arg0, int arg1) {
                finish();
            }
        });
        builder.create().show();
    }
}