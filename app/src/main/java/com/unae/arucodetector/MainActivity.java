package com.unae.arucodetector;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.Toast;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.aruco.Aruco;
import org.opencv.aruco.DetectorParameters;
import org.opencv.aruco.Dictionary;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.imgproc.Imgproc;

import static android.Manifest.permission.CAMERA;

import androidx.appcompat.app.AlertDialog;

public class MainActivity extends Activity implements CvCameraViewListener2 {

    private Mat cameraMatrix;
    private Mat distCoeffs;

    private Mat rgb;

    private Mat rvecs;
    private Mat tvecs;

    private MatOfInt ids;
    private List<Mat> corners;
    private Dictionary dictionary;
    private DetectorParameters parameters;

    public native void ConvertRGBtoGray(long matAddrInput, long matAddrResult);


    static {
        System.loadLibrary("opencv_java4");
        System.loadLibrary("native-lib");
    }

    //    private Renderer3D renderer;
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

//        renderer = new Renderer3D(this);

//        SurfaceView surface = findViewById(R.id.main_surface);
//        surface.setTransparent(true);
//        surface.setSurfaceRenderer(renderer);

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
        distCoeffs = Mat.zeros(5, 1, CvType.CV_64FC1);
        return CameraParameters.tryLoad(this, cameraMatrix, distCoeffs);
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
        corners = new LinkedList<>();
        parameters = DetectorParameters.create();
        dictionary = Aruco.getPredefinedDictionary(Aruco.DICT_6X6_50);
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame){
        Mat image = inputFrame.rgba();
        Imgproc.cvtColor(inputFrame.rgba(), rgb, Imgproc.COLOR_RGBA2RGB);
//
        ids = new MatOfInt();
        corners.clear();

        Log.d("asnjvsv", "ASbv");
        Aruco.detectMarkers(inputFrame.gray(), dictionary, corners, ids, parameters);

        if(corners.size()>0){
            Aruco.drawDetectedMarkers(rgb, corners, ids);

            rvecs = new Mat();
            tvecs = new Mat();

            Aruco.estimatePoseSingleMarkers(corners, 0.04f, cameraMatrix, distCoeffs, rvecs, tvecs);

            double r = Math.sqrt(rvecs.dot(rvecs));
            double t = Math.sqrt(tvecs.dot(tvecs));

            for(int i = 0;i<ids.toArray().length;i++){
                transformModel(tvecs.row(0), rvecs.row(0));

                Log.d("rvec", rvecs.row(i).toString());
                Log.d("tvec", tvecs.row(i).toString());


                Calib3d.drawFrameAxes(rgb, cameraMatrix, distCoeffs, rvecs.row(i), tvecs.row(i), 0.02f);
            }

        }

        return rgb;
    }

    @Override
    public void onCameraViewStopped(){
        rgb.release();
    }

    private void transformModel(final Mat tvec, final Mat rvec){
        runOnUiThread(new Runnable(){
            @Override
            public void run(){
                Log.d("tvec_000", (tvec.get(0, 0)[0] * 50) + "");
                Log.d("-tvec_001", (tvec.get(0, 0)[1] * 50) + "");
                Log.d("-tvec_002", (tvec.get(0, 0)[2] * 50) + "");

                Log.d("-rvec_002", (rvec.get(0, 0)[2] * 50) + "");
                Log.d("-rvec_001", (rvec.get(0, 0)[1] * 50) + "");
                Log.d("-rvec_000", (rvec.get(0, 0)[0] * 50) + "");
//                renderer.transform(
//                        tvec.get(0, 0)[0]*50,
//                        -tvec.get(0, 0)[1]*50,
//                        -tvec.get(0, 0)[2]*50,
//
//                        rvec.get(0, 0)[2], //yaw
//                        rvec.get(0, 0)[1], //pitch
//                        rvec.get(0, 0)[0] //roll
//                );
            }
        });
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