package com.oeadd.opencvonlyjava;

import android.Manifest;
import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Process;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.tbruyelle.rxpermissions2.RxPermissions;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.dnn.Dnn;
import org.opencv.dnn.Net;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.objdetect.Objdetect;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static final String TAG = MainActivity.class.getSimpleName();
    //    JavaCameraView cameraView;
    private JavaCameraView mOpenCvCameraView;
    private Mat mRgba;
    private Mat mGray;
    private Mat mFilp;
    private Mat mTranspose;
    private ImageView imageViewF;
    private ImageView imageViewB;
    private TextView mArea;
    private TextView mPointSize;
    private TextView mWidth;
    private TextView mHeight;
    private TextView mCardSide;
    private RelativeLayout mMainContent;
    private ImageView mIDCardBg;
    private Button mSide;
    private boolean isIdCardFrontGet = false;
    private boolean isIdCardBackGet = false;
    private boolean isFront = true;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mSide = findViewById(R.id.side);
        imageViewF = findViewById(R.id.img_z);
        imageViewB = findViewById(R.id.img_f);
        mArea = findViewById(R.id.area);
        mPointSize = findViewById(R.id.pointsize);
        mWidth = findViewById(R.id.width);
        mHeight = findViewById(R.id.height);
        mCardSide = findViewById(R.id.cardside);
        mOpenCvCameraView = findViewById(R.id.cameraView);
        mOpenCvCameraView.setVisibility(CameraBridgeViewBase.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mMainContent = findViewById(R.id.mainContent);
        mIDCardBg = findViewById(R.id.idcard_bg);
        mSide.setOnClickListener(v -> {
            if (isFront){
                mSide.setText("反面");
                mIDCardBg.setImageResource(R.mipmap.camera_idcard_back);
            }else {
                mSide.setText("正面");
                mIDCardBg.setImageResource(R.mipmap.camera_idcard_front);
            }
            isFront = !isFront;
        });
//        cameraView = findViewById(R.id.cameraView);
//        cameraView.setCvCameraViewListener(this);
        //权限请求
        RxPermissions rxPermission = new RxPermissions(this);
        rxPermission
                .requestEachCombined(Manifest.permission.CAMERA,
                        Manifest.permission.READ_EXTERNAL_STORAGE)
                .subscribe(permission -> {
                    if (permission.granted) {
                    } else if (permission.shouldShowRequestPermissionRationale) {
                    } else {
                        mOpenCvCameraView.enableView();
                    }
                });
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
                    IDCardCheckUtils.getInstance().init(MainActivity.this);
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        mOpenCvCameraView.disableView();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mGray = new Mat();
        mRgba = new Mat();
        mFilp = new Mat(width, height, CvType.CV_8UC4);
        mTranspose = new Mat(height, width, CvType.CV_8UC4);
    }

    @Override
    public void onCameraViewStopped() {
        mGray.release();
        mRgba.release();
    }
    private Net net;
    private static final String[] classNames = {"background",
            "aeroplane", "bicycle", "bird", "boat",
            "bottle", "bus", "car", "cat", "chair",
            "cow", "diningtable", "dog", "horse",
            "motorbike", "person", "pottedplant",
            "sheep", "sofa", "train", "tvmonitor"};
    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        mGray = inputFrame.gray();
            if (isFront&&!isIdCardFrontGet){
                runOnUiThread(() -> {
                    Bitmap bitmap = IDCardCheckUtils.getInstance().checkIDCard(isFront,inputFrame);
                    if (bitmap != null) {
                        imageViewF.setImageBitmap(bitmap);
                        isIdCardFrontGet = true;
                    }
                });
            }else if (!isFront&&!isIdCardBackGet){
                runOnUiThread(() -> {
                    Bitmap bitmap = IDCardCheckUtils.getInstance().checkIDCard(isFront,inputFrame);
                    if (bitmap != null) {
                        imageViewB.setImageBitmap(bitmap);
                        isIdCardBackGet = true;
                    }
                });
            }
        return mRgba;
    }
}
