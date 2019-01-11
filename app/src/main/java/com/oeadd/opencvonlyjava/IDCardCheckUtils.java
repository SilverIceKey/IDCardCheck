package com.oeadd.opencvonlyjava;

import android.graphics.Bitmap;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class IDCardCheckUtils {
    public static Bitmap checkIDCard(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
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
            double finalY = y;
            double finalWidth = width;
            double finalX = x;
            if (points.size() > 1250 && points.size() < 1450 && contourArea > 105000f && contourArea < 130000f) {
                if (height > 430 && height < 466 && width > 260 && width < 286) {
                    Bitmap bitmap2 = Bitmap.createBitmap(mRgba.cols(), mRgba.rows(), Bitmap.Config.ARGB_8888);
                    Utils.matToBitmap(mRgba, bitmap2);
                    Bitmap bitmap = Bitmap.createBitmap(bitmap2, (int) finalX, (int) finalY, (int) (finalWidth), (int) (finalHeight), null, false);
                    return bitmap;
                }
            }
        }
        return null;
    }
}
