#include <jni.h>

#include <faiss/IndexIVFFlat.h>
#include <faiss/IndexFlat.h>
#include <faiss/MetricType.h>
#include <faiss/impl/AuxIndexStructures.h>
#include <faiss/index_io.h>

#include <chrono>
#include <unordered_map>
#include <vector>
#include <filesystem>
#include <fstream>
#include <memory>

#include "log.h"
#include "UnionFind.h"
//
// Created by 67561 on 2025/3/14.
//

std::string debugFile;
std::string indexFile;
bool train = false;

extern "C" {
int getListSize(JNIEnv *env, jobject list);
jobject getListElement(JNIEnv *env, jobject list, int index);
jobject newArrayList(JNIEnv *env);
jobject addToArrayList(JNIEnv *env, jobject arrayList, jobject element);
jobject newInteger(JNIEnv *env, int value);


JNIEXPORT void JNICALL
Java_hust_album_jni_Index_init(JNIEnv *env, jobject thiz) {
    jclass indexClass = env->GetObjectClass(thiz);
    jmethodID getRootMethod = env->GetMethodID(indexClass, "getRoot", "()Ljava/lang/String;");
    jmethodID getTrainMethod = env->GetMethodID(indexClass, "isTrain", "()Z");
    auto root = static_cast<jstring>(env->CallObjectMethod(thiz, getRootMethod));
    train = env->CallBooleanMethod(thiz, getTrainMethod) == 1;
    const char* rootPath = env->GetStringUTFChars(root, nullptr);
    debugFile = std::string(rootPath) + "/debug.log";
    indexFile = std::string(rootPath) + "/index.faiss";

    if (!train && !std::filesystem::exists(std::filesystem::path(indexFile))) {
        train = true;
    }

    LOGD("%s %s %d", rootPath, indexFile.c_str(), train);
}

/**
 *
 * @param env
 * @param thiz
 * @param data 输入数据， n * d
 * @param d 维度
 * @param radius 搜索阈值
 * @return 搜索结果
 */
JNIEXPORT jobject JNICALL
Java_hust_album_jni_Index_match(JNIEnv *env, jobject thiz, jobject data, jint d,
                                jfloat radius) {
    int n = getListSize(env, data);
    std::vector<float> query(n * d);
    auto start = std::chrono::steady_clock::now();
    for (int i = 0; i < n; i++) {
        jobject oneQuery = getListElement(env, data, i);
        if (oneQuery != nullptr) {
            auto oneQueryArray = static_cast<jfloatArray>(oneQuery);
            jsize len = env->GetArrayLength(oneQueryArray);
            jfloat *oneQueryData = env->GetFloatArrayElements(oneQueryArray, nullptr);
            for (int j = 0; j < len; j++) {
                query[i * d + j] = oneQueryData[j];
            }
            env->ReleaseFloatArrayElements(oneQueryArray, oneQueryData, 0);
        }
    }
    LOGD("parse data success time: %lld ms", std::chrono::duration_cast<std::chrono::milliseconds>(
            std::chrono::steady_clock::now() - start).count());

    std::unique_ptr<faiss::IndexIVFFlat> index;
    faiss::RangeSearchResult result(n, true);
    start = std::chrono::steady_clock::now();
    if (train) {
//        注意变量的生命周期，由于被括号包裹所以 quantizer 的在括号结束后就会被释放，导致 index 内部变量指向异常，后续执行搜索时报错。所以这里应该使用动态内存分配
//        faiss::IndexFlatIP quantizer(d);
        auto quantizer = new faiss::IndexFlatIP(d);
        index = std::make_unique<faiss::IndexIVFFlat>(quantizer, d, 256, faiss::METRIC_INNER_PRODUCT);
        //        训练索引
        index->train(n, query.data());
        LOGD("train time: %lld ms", std::chrono::duration_cast<std::chrono::milliseconds>(
                std::chrono::steady_clock::now() - start).count());
        //        添加数据
        start = std::chrono::steady_clock::now();
        index->add(n, query.data());
        LOGD("add time: %lld ms", std::chrono::duration_cast<std::chrono::milliseconds>(
                std::chrono::steady_clock::now() - start).count());
        //        保存索引
        start = std::chrono::steady_clock::now();
        faiss::write_index(index.get(), indexFile.c_str());
        LOGD("write index success, index num %lld, time: %lld ms", index->ntotal, std::chrono::duration_cast<std::chrono::milliseconds>(
                std::chrono::steady_clock::now() - start).count());
    } else {
        auto ivfIndex = dynamic_cast<faiss::IndexIVFFlat*>(faiss::read_index(indexFile.c_str()));
        if (ivfIndex == nullptr) {
            LOGE("load index failed");
            return nullptr;
        }

        index.reset(ivfIndex);
        LOGD("load index success, index num %lld, time: %lld ms", index->ntotal, std::chrono::duration_cast<std::chrono::milliseconds>(
                std::chrono::steady_clock::now() - start).count());
    }

    index->nprobe = 20;

    start = std::chrono::steady_clock::now();

    if (index->is_trained ) {
        index->range_search(n, query.data(), radius, &result);
        LOGD("search time: %lld ms", std::chrono::duration_cast<std::chrono::milliseconds>(
            std::chrono::steady_clock::now() - start).count());
    }
    UnionFind uf(n);
    for (int i = 0; i < n; i++) {
        if (result.lims[i + 1] - result.lims[i] == 1) {
            continue;
        }
        for (size_t j = result.lims[i]; j < result.lims[i + 1]; j++) {
            int label = static_cast<int>(result.labels[j]);
            uf.merge(i, label);
        }
    }
    // 获取并查集中所有分组
    std::unordered_map<int, std::vector<int>> groups;
    for (int i = 0; i < n; i++) {
        groups[uf.find(i)].push_back(i);
    }
    jobject sim = newArrayList(env);
    for (const auto &group : groups) {
        if (group.second.size() == 1) {
            continue;
        }
        jobject oneGroup = newArrayList(env);
        for (int i : group.second) {
            addToArrayList(env, oneGroup, newInteger(env, i));
        }
        addToArrayList(env, sim, oneGroup);
    }
    return sim;
}

int getListSize(JNIEnv *env, jobject list) {
    jclass listClass = env->FindClass("java/util/List");
    jmethodID sizeMethod = env->GetMethodID(listClass, "size", "()I");
    return env->CallIntMethod(list, sizeMethod);
}

jobject getListElement(JNIEnv *env, jobject list, int index) {
    jclass listClass = env->FindClass("java/util/List");
    jmethodID getMethod = env->GetMethodID(listClass, "get", "(I)Ljava/lang/Object;");
    return env->CallObjectMethod(list, getMethod, index);
}

jobject newArrayList(JNIEnv *env) {
    jclass arrayListClass = env->FindClass("java/util/ArrayList");
    jmethodID arrayListConstructor = env->GetMethodID(arrayListClass, "<init>", "()V");
    return env->NewObject(arrayListClass, arrayListConstructor);
}

jobject addToArrayList(JNIEnv *env, jobject arrayList, jobject element) {
    jclass arrayListClass = env->FindClass("java/util/ArrayList");
    jmethodID addMethod = env->GetMethodID(arrayListClass, "add", "(Ljava/lang/Object;)Z");
    env->CallBooleanMethod(arrayList, addMethod, element);
    return arrayList;
}

jobject newInteger(JNIEnv *env, int value) {
    jclass integerClass = env->FindClass("java/lang/Integer");
    jmethodID integerConstructor = env->GetMethodID(integerClass, "<init>", "(I)V");
    return env->NewObject(integerClass, integerConstructor, value);
}
}