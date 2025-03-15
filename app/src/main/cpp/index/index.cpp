#include <jni.h>

#include <faiss/IndexIVFFlat.h>
#include <faiss/IndexFlat.h>
#include <faiss/MetricType.h>

#include <chrono>

#include "log.h"
//
// Created by 67561 on 2025/3/14.
//


extern "C"
JNIEXPORT jintArray JNICALL
Java_hust_album_jni_Index_match(JNIEnv *env, jobject thiz, jfloatArray data, jint d, jint k) {
    faiss::IndexFlatIP quantizer(d);
    faiss::IndexIVFFlat index(&quantizer, d, 50, faiss::METRIC_INNER_PRODUCT);

    int n = env->GetArrayLength(data) / d;
    float *data_ptr = env->GetFloatArrayElements(data, nullptr);

    auto start = std::chrono::steady_clock::now();
    index.train(n, data_ptr);
    LOGD("train time: %lld ms", std::chrono::duration_cast<std::chrono::milliseconds>(
            std::chrono::steady_clock::now() - start).count());
    start = std::chrono::steady_clock::now();
    index.add(n, data_ptr);
    LOGD("add time: %lld ms", std::chrono::duration_cast<std::chrono::milliseconds>(
            std::chrono::steady_clock::now() - start).count());

    auto *labels = new faiss::idx_t[n * k];
    auto *distances = new float[n * k];
    index.nprobe = 5;

    start = std::chrono::steady_clock::now();
    index.search(n, data_ptr, k, distances, labels);
    LOGD("search time: %lld ms", std::chrono::duration_cast<std::chrono::milliseconds>(
            std::chrono::steady_clock::now() - start).count());

    jintArray ret = env->NewIntArray(n * k);
    jint *pRet = env->GetIntArrayElements(ret, nullptr);
    for (int i = 0; i < n * k; i++) {
        pRet[i] = labels[i];
    }
    env->ReleaseIntArrayElements(ret, pRet, 0);
    return ret;

}