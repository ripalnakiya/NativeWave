//
// Created by Ripal on 20-03-26.
//
#include <jni.h>
#include "kissfft-131.2.0/kiss_fft.h"
#include <vector>
#include <cmath>

extern "C"
JNIEXPORT jfloatArray JNICALL
Java_com_ripalnakiya_nativewave_AudioRecorder_performFFT(JNIEnv *env, jobject thiz, jfloatArray inputArray) {
    jsize size = env->GetArrayLength(inputArray);
    jfloat *input = env->GetFloatArrayElements(inputArray, 0);

    kiss_fft_cfg cfg = kiss_fft_alloc(size, 0, nullptr, nullptr);

    std::vector<kiss_fft_cpx> in(size), out(size);

    // Prepare input
    for (int i = 0; i < size; i++) {
        in[i].r = input[i];
        in[i].i = 0;
    }

    // Perform FFT
    kiss_fft(cfg, in.data(), out.data());

    // Convert to magnitude
    std::vector<float> magnitude(size);
    for (int i = 0; i < size; i++) {
        magnitude[i] = sqrt(out[i].r * out[i].r + out[i].i * out[i].i);
    }

    // Return result
    jfloatArray result = env->NewFloatArray(size);
    env->SetFloatArrayRegion(result, 0, size, magnitude.data());

    free(cfg);
    env->ReleaseFloatArrayElements(inputArray, input, 0);

    return result;
}
