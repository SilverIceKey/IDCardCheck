package com.oeadd.opencvonlyjava;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.media.FaceDetector;
import android.view.MenuItem;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.dnn.Dnn;
import org.opencv.dnn.Net;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class IDCardCheckUtils {
    private static IDCardCheckUtils mInstance;
    private double mFinalWidth;
    private double mFinalHeight;
    public static IDCardCheckUtils getInstance() {
        if (mInstance==null){
            synchronized (IDCardCheckUtils.class){
                if (mInstance==null){
                    mInstance = new IDCardCheckUtils();
                }
            }
        }
        return mInstance;
    }
    private Net net;
    private int classId;
    public void init(Context context){
        String proto = getPath("deploy.prototxt", context);
        String weights = getPath("mobilenet_iter_73000.caffemodel", context);
        net = Dnn.readNetFromCaffe(proto, weights);
    }

    public Bitmap checkIDCard(boolean isFront,CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat mRgba = inputFrame.rgba();
        Mat mGray = inputFrame.gray();
        Mat blur = new Mat();
        Mat canny = new Mat();
        List<MatOfPoint> matOfPoint = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.GaussianBlur(mGray, blur, new Size(5, 5), 0);
        Imgproc.Canny(blur, canny, 50, 100, 3, false);
        Imgproc.findContours(canny, matOfPoint, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_NONE);
        double maxVal = 0;
        int maxValIdx = 0;
        for (int contourIdx = 0; contourIdx < matOfPoint.size(); contourIdx++) {
            double contourArea = Imgproc.contourArea(matOfPoint.get(contourIdx));
            if (maxVal < contourArea) {
                maxVal = contourArea;
                maxValIdx = contourIdx;
            }
        }
        if (matOfPoint.size() != 0) {
            double contourArea = Imgproc.contourArea(matOfPoint.get(maxValIdx));
            List<Point> points = matOfPoint.get(maxValIdx).toList();
            double x, y, width, height;
            width = height = 0;
            x = points.get(0).x;
            y = points.get(0).y;
            for (int i = 0; i < points.size(); i++) {
                if (points.get(i).x > width) {
                    width = points.get(i).x;
                }
                if (points.get(i).x < x) {
                    x = points.get(i).x;
                }
                if (points.get(i).y > height) {
                    height = points.get(i).y;
                }
                if (points.get(i).y < y) {
                    y = points.get(i).y;
                }
            }
            width = width - x;
            height = height - y;
            int finalMaxValIdx = maxValIdx;
            double finalHeight = height;
            mFinalHeight = height;
            double finalY = y;
            double finalWidth = width;
            mFinalWidth = width;
            double finalX = x;
            if (points.size() > 1200 && points.size() < 1450 && contourArea > 105000f && contourArea < 130000f) {
                if (height > 430 && height < 466 && width > 260 && width < 286) {
                    Bitmap bitmap2 = Bitmap.createBitmap(mRgba.cols(), mRgba.rows(), Bitmap.Config.RGB_565);
                    Bitmap bitmap3 = Bitmap.createBitmap(mRgba.cols(), mRgba.rows(), Bitmap.Config.ARGB_8888);
                    Utils.matToBitmap(mRgba, bitmap2);
                    Bitmap bitmap = Bitmap.createBitmap(bitmap2, (int) finalX, (int) finalY, (int) (finalWidth), (int) (finalHeight), null, false);
                    bitmap2.recycle();
                    bitmap3.recycle();
                    return bitmap;
//                    if (checkFace(mRgba)&&isFront&&classId!=20){
//                        bitmap2.recycle();
//                        bitmap.recycle();
//                        Utils.matToBitmap(mRgba, bitmap3);
//                        return Bitmap.createBitmap(bitmap3, (int) finalX, (int) finalY, (int) (finalWidth), (int) (finalHeight), null, false);
//                    }else if (checkFace(mRgba)&&!isFront&&classId!=20){
//                        bitmap2.recycle();
//                        bitmap.recycle();
//                        return null;
//                    }if (!checkFace(mRgba)&&!isFront&&classId!=20){
//                        bitmap2.recycle();
//                        bitmap.recycle();
//                        Utils.matToBitmap(mRgba, bitmap3);
//                        return Bitmap.createBitmap(bitmap3, (int) finalX, (int) finalY, (int) (finalWidth), (int) (finalHeight), null, false);
//                    }
                }
            }
        }
        return null;
    }

    public Bitmap checkFaceRGB(boolean isFront,CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat mRgba = inputFrame.rgba();
        checkFace(mRgba);
        return null;
    }

    private static final String[] classNames = {"background",
            "aeroplane", "bicycle", "bird", "boat",
            "bottle", "bus", "car", "cat", "chair",
            "cow", "diningtable", "dog", "horse",
            "motorbike", "person", "pottedplant",
            "sheep", "sofa", "train", "tvmonitor"};
    private boolean checkFace(Mat mRgba){
        final int IN_WIDTH = 300;
        final int IN_HEIGHT = 300;
        final float WH_RATIO = (float)IN_WIDTH / IN_HEIGHT;
        final double IN_SCALE_FACTOR = 0.007843;
        final double MEAN_VAL = 127.5;
        final double THRESHOLD = 0.2;
        // Get a new frame
        Mat frame = mRgba;
        Imgproc.cvtColor(frame, frame, Imgproc.COLOR_RGBA2RGB);
        // Forward image through network.
        Mat blob = Dnn.blobFromImage(frame, IN_SCALE_FACTOR,
                new Size(IN_WIDTH, IN_HEIGHT),
                new Scalar(MEAN_VAL, MEAN_VAL, MEAN_VAL), /*swapRB*/false, /*crop*/false);
        net.setInput(blob);
        Mat detections = net.forward();
        int cols = frame.cols();
        int rows = frame.rows();
        detections = detections.reshape(1, (int)detections.total() / 7);
        for (int i = 0; i < detections.rows(); ++i) {
            double confidence = detections.get(i, 2)[0];
            if (confidence > THRESHOLD) {
                classId = (int)detections.get(i, 1)[0];
                int left   = (int)(detections.get(i, 3)[0] * cols);
                int top    = (int)(detections.get(i, 4)[0] * rows);
                int right  = (int)(detections.get(i, 5)[0] * cols);
                int bottom = (int)(detections.get(i, 6)[0] * rows);
//                 Draw rectangle around detected object.
                Imgproc.rectangle(frame, new Point(left, top), new Point(right, bottom),
                        new Scalar(0, 255, 0));
                String label = classNames[classId] + ": " + confidence;
                int[] baseLine = new int[1];
                Size labelSize = Imgproc.getTextSize(label, Imgproc.FONT_HERSHEY_SIMPLEX, 0.5, 1, baseLine);
                // Draw background for label.
                Imgproc.rectangle(frame, new Point(left, top - labelSize.height),
                        new Point(left + labelSize.width, top + baseLine[0]),
                        new Scalar(255, 255, 255), Imgproc.FILLED);
                // Write class name and confidence.
                Imgproc.putText(frame, label, new Point(left, top),
                        Imgproc.FONT_HERSHEY_SIMPLEX, 0.5, new Scalar(0, 0, 0));
                System.out.println("classId"+classId);
                return classId==0;
            }
        }
        return false;
    }

    private boolean checFace(Bitmap bitmap){
        FaceDetector faceDetector = new FaceDetector((int) mFinalWidth,(int)mFinalHeight,1);
        FaceDetector.Face[] faces = new FaceDetector.Face[1];
        faceDetector.findFaces(bitmap,faces);
        System.out.println("facelength-------------->"+faces.length);
        return faces[0]!=null;
    }

    // Upload file to storage and return a path.
    private static String getPath(String file, Context context) {
        AssetManager assetManager = context.getAssets();
        BufferedInputStream inputStream = null;
        try {
            // Read data from assets.
            inputStream = new BufferedInputStream(assetManager.open(file));
            byte[] data = new byte[inputStream.available()];
            inputStream.read(data);
            inputStream.close();
            // Create copy file in storage.
            File outFile = new File(context.getFilesDir(), file);
            FileOutputStream os = new FileOutputStream(outFile);
            os.write(data);
            os.close();
            // Return a path to file which may be read in common way.
            return outFile.getAbsolutePath();
        } catch (IOException ex) {
//            Log.i(TAG, "Failed to upload a file");
        }
        return "";
    }

}
