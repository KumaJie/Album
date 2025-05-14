#include <jni.h>

#include <android/log.h>
#include <android/asset_manager_jni.h>
#include <android/bitmap.h>

#include <net.h>
#include <benchmark.h>

#include "log.h"

static ncnn::Net mobilenet;


extern "C" {

JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    LOGD("JNI_OnLoad");
    ncnn::create_gpu_instance();

    return JNI_VERSION_1_4;
}

JNIEXPORT void JNI_OnUnload(JavaVM *vm, void *reserved) {
    LOGD("JNI_OnUnload");
    ncnn::destroy_gpu_instance();
}


JNIEXPORT jboolean JNICALL
Java_hust_album_jni_FeatureExtractor_Init(JNIEnv *env, jobject thiz, jobject mgr) {
    AAssetManager *m = AAssetManager_fromJava(env, mgr);
    mobilenet.opt.use_vulkan_compute = true;
    int ret0 = mobilenet.load_param(m, "mb.ncnn.param");
    int ret1 = mobilenet.load_model(m, "mb.ncnn.bin");
    LOGD("load %d %d", ret0, ret1);
    if (ret0 != 0 || ret1 != 0) {
        return JNI_FALSE;
    }
    return JNI_TRUE;
}
double sum = 0;
JNIEXPORT jfloatArray JNICALL
Java_hust_album_jni_FeatureExtractor_ExtractFeature(JNIEnv *env, jobject thiz, jobject bitmap) {
    double start_time = ncnn::get_current_time();

    ncnn::Mat in = ncnn::Mat::from_android_bitmap_resize(env, bitmap, ncnn::Mat::PIXEL_RGB, 224, 224);
    const float mean_vals[3] = {0.485f*255.f, 0.456f*255.f, 0.406f*255.f};    ///R，G，B
    const float norm_vals[3] = {1/0.229f/255.f, 1/0.224f/255.f, 1/0.225f/255.f};
    in.substract_mean_normalize(mean_vals, norm_vals);
    ncnn::Mat out;
    ncnn::Extractor ex = mobilenet.create_extractor();
    ex.input("in0", in);
    ex.extract("out0", out);

    sum += ncnn::get_current_time() - start_time;
//    LOGD("%.2fms  extract", sum);

    jfloatArray result = env->NewFloatArray(out.w);
    jfloat *result_data = env->GetFloatArrayElements(result, nullptr);
    for (int i = 0; i < out.w; i++) {
        result_data[i] = out[i];
    }
    env->ReleaseFloatArrayElements(result, result_data, 0);
    return result;
}


}

