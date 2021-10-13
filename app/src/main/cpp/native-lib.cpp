#include <jni.h>
#include <opencv2/opencv.hpp>
#include <opencv2/aruco.hpp>
#include <opencv2/highgui.hpp>

using namespace std;
using namespace cv;

//#include <string>

//extern "C" JNIEXPORT jstring JNICALL
//Java_com_unae_arucodetector_MainActivity_stringFromJNI(
//        JNIEnv* env,
//        jobject /* this */) {
//    std::string hello = "Hello from C++";
//    return env->NewStringUTF(hello.c_str());
//}

//extern "C"
//JNIEXPORT void JNICALL
//Java_com_unae_arucodetector_MainActivity_ConvertRGBtoGray(JNIEnv *env, jobject thiz,
//                                                          jlong mat_addr_input,
//                                                          jlong mat_addr_result) {
//    // TODO: implement ConvertRGBtoGray()
//    Mat &matInput = *(Mat *)mat_addr_input;
//    Mat &matResult = *(Mat *)mat_addr_result;
//
//    cvtColor(matInput, matResult, COLOR_RGBA2GRAY);
//}

extern "C"
JNIEXPORT void JNICALL
Java_com_unae_arucodetector_MainActivity_DetectMarkers(JNIEnv *env, jobject thiz,
                                                       jlong mat_addr_input,
                                                       jlong mat_addr_result,
                                                       jlong params,
                                                       jlong dict_addr,
                                                       jlong mat_addr_cam,
                                                       jlong mat_addr_dist) {
    // TODO: implement DetectMarkers()
    Mat &matInput = *(Mat *)mat_addr_input;
    Mat &matResult = *(Mat *)mat_addr_result;
    Ptr<aruco::DetectorParameters> &detectorParams = *(Ptr<aruco::DetectorParameters> *)params;
    Ptr<aruco::Dictionary> &dictionary = *(Ptr<aruco::Dictionary> *)dict_addr;
    Mat &camMatrix = *(Mat *)mat_addr_cam;
    Mat &distCoeffs = *(Mat *)mat_addr_dist;

    float markerLength= 0011;
    cvtColor(matInput, matInput, COLOR_RGBA2RGB); //그레이스케일 이미지로 변환한다.
    cvtColor(matInput, matResult, COLOR_RGBA2GRAY); //그레이스케일 이미지로 변환한다.

    vector< int > ids;
    vector< vector< Point2f > > corners, rejected;
    vector< Vec3d > rvecs, tvecs;

    // detect markers and estimate pose
    aruco::detectMarkers(matResult, dictionary, corners, ids, detectorParams, rejected);
    if(ids.size() > 0)
        aruco::estimatePoseSingleMarkers(corners, markerLength, camMatrix, distCoeffs, rvecs,
                                         tvecs);

    // draw results
    matInput.copyTo(matResult);
    if(ids.size() > 0) {
        aruco::drawDetectedMarkers(matResult, corners, ids);

        for(unsigned int i = 0; i < ids.size(); i++)
            aruco::drawAxis(matResult, camMatrix, distCoeffs, rvecs[i], tvecs[i],
                            markerLength * 0.5f);
    }

}
extern "C"
JNIEXPORT jboolean JNICALL
Java_com_unae_arucodetector_MainActivity_ReadCameraParameters(JNIEnv *env, jobject thiz,
                                                              jlong mat_addr_cam,
                                                              jlong mat_addr_dist) {
    // TODO: implement ReadCameraParameters()
    Mat &camMatrix = *(Mat *)mat_addr_cam;
    Mat &distCoeffs = *(Mat *)mat_addr_dist;

    FileStorage fs("camera.yaml", FileStorage::READ);

    if(!fs.isOpened())
        return false;

    fs["camera_matrix"] >> camMatrix;
    fs["distortion_coefficients"] >> distCoeffs;

    return true;

}