// Dorja panorama stitch core — a 1:1 port of the JNI engine from
// PanoramaStitching-Android-OpenCV (thePerfectPanorama lineage).
//
// The original pipeline, preserved exactly (same "center of gravity" strategy):
//   1. Downscale every frame to ~1000px before anything else.
//   2. cv::Stitcher in PANORAMA mode (createDefault(true) in 2.4 ≙ create(PANORAMA) in 4.x).
//   3. Registration / seam-estimation / compositing resolutions set to -1
//      (full-quality internal defaults, quality over speed).
//   4. Wave correction ON, horizontal (WAVE_CORRECT_HORIZ).
//   5. Lenient confidence threshold (-1) so weaker matches still stitch.
//
// Only the plumbing changed: we no longer load 2016-era prebuilt
// armeabi-v7a/x86 .so files — this compiles natively for every ABI the
// Gradle build targets (arm64-v8a included), which is what makes it work
// on modern phones.

#include <jni.h>
#include <android/log.h>
#include <android/bitmap.h>

#include <opencv2/core.hpp>
#include <opencv2/imgproc.hpp>
#include <opencv2/stitching.hpp>

#include <algorithm>
#include <vector>

#define LOG_TAG "DorjaPanorama"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace {

// The original scales so that one dimension becomes 1000px. Android ints
// (Bitmap.getPixels) are little-endian ARGB: the raw bytes in memory are
// B,G,R,A — i.e. the Mat is a BGRA image from OpenCV's point of view.
constexpr float kMaxEdge = 1000.0f;

void pixelsToBgr(jint* pixels, jint width, jint height, cv::Mat& out) {
    cv::Mat bgra(height, width, CV_8UC4, reinterpret_cast<void*>(pixels));
    // BGRA -> RGBA-equivalent channel layout for the stitcher, like the
    // original's cvtColor(curimage, newimage, CV_RGBA2RGB).
    cv::cvtColor(bgra, out, cv::COLOR_BGRA2RGB);
}

} // namespace

extern "C" JNIEXPORT jintArray JNICALL
Java_com_example_scanner_OpenCvPanoramaStitcher_nativeStitch(
        JNIEnv* env, jclass /*clazz*/,
        jint frameCount, jintArray widths, jintArray heights,
        jobjectArray pixelFrames) {

    jint ret_ok = 1;
    std::vector<cv::Mat> imgVec;

    jint* wArr = env->GetIntArrayElements(widths, nullptr);
    jint* hArr = env->GetIntArrayElements(heights, nullptr);

    for (int k = 0; k < frameCount; ++k) {
        jintArray frame = static_cast<jintArray>(
                env->GetObjectArrayElement(pixelFrames, k));
        if (frame == nullptr) { continue; }

        // Wrap the pixel buffer without copying, like Mat*(jlong addr) did.
        jint* pixels = env->GetIntArrayElements(frame, nullptr);
        if (pixels == nullptr) {
            env->DeleteLocalRef(frame);
            continue;
        }

        cv::Mat colorFrame;
        pixelsToBgr(pixels, wArr[k], hArr[k], colorFrame);

        // Reduce the resolution for fast computation — the original's
        // 1000px recipe.
        float scale = kMaxEdge / std::max(colorFrame.cols, colorFrame.rows);
        cv::resize(colorFrame, colorFrame,
                   cv::Size(int(scale * colorFrame.cols), int(scale * colorFrame.rows)),
                   0, 0, cv::INTER_AREA);

        imgVec.push_back(colorFrame);

        env->ReleaseIntArrayElements(frame, pixels, JNI_ABORT);
        env->DeleteLocalRef(frame);
    }

    env->ReleaseIntArrayElements(widths, wArr, JNI_ABORT);
    env->ReleaseIntArrayElements(heights, hArr, JNI_ABORT);

    if (imgVec.size() < 2) {
        LOGE("Need at least 2 frames, got %zu", imgVec.size());
        return nullptr;
    }

    // create() returns Ptr<Stitcher> — hold it and call through the pointer.
    cv::Ptr<cv::Stitcher> stitcher = cv::Stitcher::create(cv::Stitcher::PANORAMA);
    // Same settings block as the original JNI (values -1 = internal defaults).
    stitcher->setRegistrationResol(-1);
    stitcher->setSeamEstimationResol(-1);
    stitcher->setCompositingResol(-1);
    stitcher->setPanoConfidenceThresh(-1);
    stitcher->setWaveCorrection(true);
    stitcher->setWaveCorrectKind(cv::detail::WAVE_CORRECT_HORIZ);

    cv::Mat result;
    cv::Stitcher::Status status = stitcher->stitch(imgVec, result);
    if (status != cv::Stitcher::OK) {
        LOGE("Stitcher failed with status %d", static_cast<int>(status));
        return nullptr;
    }

    // Result is RGB (opencv sees it as whatever we passed in); convert back to
    // Android pixel bytes [B,G,R,A] = ARGB int little-endian.
    cv::Mat out;
    cv::cvtColor(result, out, cv::COLOR_RGB2BGRA);

    const jint outW = out.cols;
    const jint outH = out.rows;
    const jint outCount = outW * outH;

    std::vector<jint> pixels;
    pixels.reserve(static_cast<size_t>(outCount));
    const uint8_t* data = out.data;
    for (jint i = 0; i < outCount; ++i) {
        const uint8_t b = data[i * 4 + 0];
        const uint8_t g = data[i * 4 + 1];
        const uint8_t r = data[i * 4 + 2];
        // 0xFF AARRGGBB — matches Bitmap.getPixels/setPixels ints.
        pixels.push_back(
                (0xFFu << 24) | (static_cast<jint>(r) << 16) |
                (static_cast<jint>(g) << 8) | static_cast<jint>(b));
    }

    // First two ints carry the dimensions, the rest are the pixels.
    jintArray outArr = env->NewIntArray(outCount + 2);
    if (outArr == nullptr) {
        env->ThrowNew(env->FindClass("java/lang/OutOfMemoryError"),
                      "panorama pixel buffer");
        return nullptr;
    }
    std::vector<jint> withDims;
    withDims.reserve(outCount + 2);
    LOGI("Stitched %d frames -> %dx%d px panorama", frameCount, outW, outH);
    withDims.push_back(outW);
    withDims.push_back(outH);
    withDims.insert(withDims.end(), pixels.begin(), pixels.end());
    env->SetIntArrayRegion(outArr, 0, static_cast<jsize>(withDims.size()),
                           withDims.data());
    (void)ret_ok;
    return outArr;
}
