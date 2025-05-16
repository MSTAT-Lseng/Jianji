#include <jni.h>
#include <cstring>
#include <string>
#include <vector>
#include <sstream>
#include <iomanip>

extern "C" JNIEXPORT jstring JNICALL
bytesToHex(JNIEnv* env, jobject /* this */, jbyteArray byteArray) {

    jsize len = env->GetArrayLength(byteArray);

    jbyte* bytes = env->GetByteArrayElements(byteArray, NULL);
    if (bytes == nullptr) {
        return nullptr; // 内存分配失败
    }

    const char hex_chars[] = "0123456789ABCDEF";

    std::vector<char> result(len * 2 + 1);
    if (result.empty()) {
        env->ReleaseByteArrayElements(byteArray, bytes, JNI_ABORT);
        return nullptr;
    }

    for (jsize i = 0; i < len; ++i) {
        int v = (static_cast<unsigned char>(bytes[i])); // 转为无符号
        result[i * 2] = hex_chars[v >> 4];
        result[i * 2 + 1] = hex_chars[v & 0x0F];
    }

    result[len * 2] = '\0';

    jstring jResult = env->NewStringUTF(result.data());

    env->ReleaseByteArrayElements(byteArray, bytes, JNI_ABORT);

    return jResult;
}

// 宏：检查并清除异常
#define CHECK_EXCEPTION(env) \
    if ((env)->ExceptionCheck()) { \
        (env)->ExceptionClear(); \
        return nullptr; \
    }

extern "C"
JNIEXPORT jstring JNICALL
Java_m20_simple_bookkeeping_api_backup_BackupCreator_getAppSignature(
        JNIEnv *env,
        jobject thiz,
        jobject context,
        jstring package_name,
        jstring algorithm
        ) {

    (void)thiz; // 未使用参数

    jclass contextClass = env->GetObjectClass(context);
    CHECK_EXCEPTION(env);

    // 获取 Build.VERSION.SDK_INT
    jclass buildVersionClass = env->FindClass("android/os/Build$VERSION");
    CHECK_EXCEPTION(env);
    jfieldID sdkIntField = env->GetStaticFieldID(buildVersionClass, "SDK_INT", "I");
    int sdkInt = env->GetStaticIntField(buildVersionClass, sdkIntField);
    env->DeleteLocalRef(buildVersionClass);
    CHECK_EXCEPTION(env);

    // 获取 PackageManager
    jmethodID getPackageManagerMid = env->GetMethodID(
            contextClass,
            "getPackageManager",
            "()Landroid/content/pm/PackageManager;");
    jobject packageManager = env->CallObjectMethod(context, getPackageManagerMid);
    env->DeleteLocalRef(contextClass);
    if (!packageManager || env->ExceptionCheck()) {
        env->ExceptionClear();
        return nullptr;
    }

    jclass pmClass = env->GetObjectClass(packageManager);
    CHECK_EXCEPTION(env);

    // 根据 SDK 版本选择正确的 flags
    jint flags = 0;
    if (sdkInt >= 28) {
        jfieldID signingCertificatesFid = env->GetStaticFieldID(
                pmClass,
                "GET_SIGNING_CERTIFICATES",
                "I");
        if (!signingCertificatesFid) {
            env->DeleteLocalRef(pmClass);
            return nullptr;
        }
        flags = env->GetStaticIntField(pmClass, signingCertificatesFid);
    } else {
        jclass pmInterfaceClass = env->FindClass("android/content/pm/PackageManager");
        if (!pmInterfaceClass) {
            env->DeleteLocalRef(pmClass);
            return nullptr;
        }
        jfieldID signaturesFid = env->GetStaticFieldID(pmInterfaceClass,
                                                       "GET_SIGNATURES", "I");
        if (!signaturesFid) {
            env->DeleteLocalRef(pmInterfaceClass);
            env->DeleteLocalRef(pmClass);
            return nullptr;
        }
        flags = env->GetStaticIntField(pmInterfaceClass, signaturesFid);
        env->DeleteLocalRef(pmInterfaceClass);
    }

    jmethodID getPackageInfoMid = env->GetMethodID(
            pmClass,
            "getPackageInfo",
            "(Ljava/lang/String;I)Landroid/content/pm/PackageInfo;");
    jobject packageInfo = env->CallObjectMethod(
            packageManager,
            getPackageInfoMid,
            package_name,
            flags);
    env->DeleteLocalRef(pmClass);
    if (!packageInfo || env->ExceptionCheck()) {
        env->ExceptionClear();
        return nullptr;
    }

    jclass packageInfoClass = env->GetObjectClass(packageInfo);
    CHECK_EXCEPTION(env);

    jobjectArray signersArray = nullptr;

    if (sdkInt >= 28) {
        jclass signingInfoClass = env->FindClass("android/content/pm/SigningInfo");
        jfieldID signingInfoField = env->GetFieldID(
                packageInfoClass,
                "signingInfo",
                "Landroid/content/pm/SigningInfo;");
        jobject signingInfo = env->GetObjectField(
                packageInfo,
                signingInfoField);
        jmethodID getApkContentsSignersMid = env->GetMethodID(
                signingInfoClass,
                "getApkContentsSigners",
                "()[Landroid/content/pm/Signature;");
        signersArray = (jobjectArray)env->CallObjectMethod(
                signingInfo,
                getApkContentsSignersMid);
        env->DeleteLocalRef(signingInfoClass);
    } else {
        jfieldID signaturesField = env->GetFieldID(
                packageInfoClass,
                "signatures",
                "[Landroid/content/pm/Signature;");
        signersArray = (jobjectArray)env->GetObjectField(
                packageInfo,
                signaturesField);
    }

    env->DeleteLocalRef(packageInfoClass);
    if (!signersArray || env->GetArrayLength(signersArray) == 0) {
        return nullptr;
    }

    jobject firstSigner = env->GetObjectArrayElement(signersArray, 0);
    jclass signatureClass = env->GetObjectClass(firstSigner);
    jmethodID toByteArrayMid = env->GetMethodID(
            signatureClass,
            "toByteArray",
            "()[B");
    jbyteArray signatureBytes = (jbyteArray)env->CallObjectMethod(
            firstSigner,
            toByteArrayMid);
    env->DeleteLocalRef(signatureClass);
    if (!signatureBytes || env->ExceptionCheck()) {
        env->ExceptionClear();
        return nullptr;
    }

    // 使用 MessageDigest 计算摘要
    jclass mdClass = env->FindClass("java/security/MessageDigest");
    jmethodID getInstanceMid = env->GetStaticMethodID(
            mdClass,
            "getInstance",
            "(Ljava/lang/String;)Ljava/security/MessageDigest;");
    jstring algo = algorithm ? algorithm : env->NewStringUTF("SHA-256");
    jobject messageDigest = env->CallStaticObjectMethod(
            mdClass,
            getInstanceMid,
            algo);
    if (!messageDigest || env->ExceptionCheck()) {
        env->ExceptionClear();
        return nullptr;
    }

    jmethodID updateMid = env->GetMethodID(mdClass, "update", "([B)V");
    env->CallVoidMethod(messageDigest, updateMid, signatureBytes);

    jmethodID digestMid = env->GetMethodID(mdClass, "digest", "()[B");
    jbyteArray digestArray = (jbyteArray)env->CallObjectMethod(
            messageDigest,
            digestMid);
    if (!digestArray || env->ExceptionCheck()) {
        env->ExceptionClear();
        return nullptr;
    }

    // 转换为十六进制字符串
    jstring hexString = bytesToHex(env, nullptr, digestArray);

    // 清理局部引用
    env->DeleteLocalRef(digestArray);
    env->DeleteLocalRef(messageDigest);
    env->DeleteLocalRef(mdClass);

    return hexString;
}