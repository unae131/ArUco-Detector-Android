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
Java_com_unae_arucodetector_MainActivity_ConvertRGBtoGray(JNIEnv *env, jobject thiz,
                                                       jlong mat_addr_input,
                                                       jlong mat_addr_result) {
    // TODO: implement DetectMarkers()

    Mat &matInput = *(Mat *)mat_addr_input;
    Mat &matResult = *(Mat *)mat_addr_result;

    //그레이스케일 이미지로 변환한다.
    cvtColor(matInput, matResult, COLOR_RGBA2GRAY);
    //Adaptive Thresholding을 적용하여 이진화 한다.
    Mat binary_image;
    adaptiveThreshold(matResult, binary_image,
                      255, ADAPTIVE_THRESH_GAUSSIAN_C, THRESH_BINARY_INV, 91, 7);
    //threshold(input_gray_image, binary_image, 125, 255, THRESH_BINARY_INV | THRESH_OTSU);

}