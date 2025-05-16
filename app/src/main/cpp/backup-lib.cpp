#include <jni.h>
#include <cstring>
#include <string>
#include <vector>

extern "C" JNIEXPORT jstring JNICALL
bytesToHex(JNIEnv* env,
           jobject /* this */,
           jbyteArray byteArray) {

    // 获取字节数组长度
    jsize len = env->GetArrayLength(byteArray);

    // 获取原始字节数组指针
    jbyte* bytes = env->GetByteArrayElements(byteArray, NULL);
    if (bytes == nullptr) {
        return env->NewStringUTF(""); // 如果获取失败，返回空字符串
    }

    // 十六进制字符表
    static const char hex_chars[] = "0123456789ABCDEF";

    // 分配结果字符串内存（每个字节对应两个字符 + 末尾 '\0'）
    char* result = new char[len * 2 + 1];

    // 转换每个字节为两个十六进制字符
    for (jsize i = 0; i < len; ++i) {
        int v = (bytes[i] & 0xFF); // 确保无符号
        result[i * 2] = hex_chars[v >> 4];     // 高位
        result[i * 2 + 1] = hex_chars[v & 0x0F]; // 低位
    }

    // 添加字符串结束符
    result[len * 2] = '\0';

    // 创建并返回 Java 字符串
    jstring jResult = env->NewStringUTF(result);

    // 释放资源
    delete[] result;
    env->ReleaseByteArrayElements(byteArray, bytes, 0);

    return jResult;
}

extern "C"
JNIEXPORT jstring JNICALL
Java_m20_simple_bookkeeping_api_backup_BackupCreator_getAppSignature(JNIEnv *env, jobject thiz,
                                                                     jobject context,
                                                                     jstring package_name,
                                                                     jstring algorithm) {
    // 获取当前 Android 版本
    jclass contextClass = env->GetObjectClass(context);
    jclass buildVersionClass = env->FindClass("android/os/Build$VERSION");
    jfieldID sdkIntField = env->GetStaticFieldID(buildVersionClass, "SDK_INT", "I");
    int sdkInt = env->GetStaticIntField(buildVersionClass, sdkIntField);

    // 获取 PackageManager
    jmethodID getPackageManagerMid = env->GetMethodID(contextClass, "getPackageManager", "()Landroid/content/pm/PackageManager;");
    jobject packageManager = env->CallObjectMethod(context, getPackageManagerMid);
    if (!packageManager || env->ExceptionCheck()) {
        env->ExceptionClear();
        return nullptr;
    }

    jclass pmClass = env->GetObjectClass(packageManager);
    jmethodID getPackageInfoMid = env->GetMethodID(pmClass, "getPackageInfo", "(Ljava/lang/String;I)Landroid/content/pm/PackageInfo;");

    // 确定 flags
    jint flags = 0;
    if (sdkInt >= 28) {
        jfieldID getSigningCertificatesFid = env->GetStaticFieldID(pmClass, "GET_SIGNING_CERTIFICATES", "I");
        flags = env->GetStaticIntField(pmClass, getSigningCertificatesFid);
    } else {
        jclass packageManagerInterfaceClass = env->FindClass("android/content/pm/PackageManager");
        if (packageManagerInterfaceClass == nullptr) {
            if (env->ExceptionCheck()) {
                env->ExceptionClear();
            }
            return nullptr;
        }
        jfieldID getSignaturesFid = env->GetStaticFieldID(packageManagerInterfaceClass, "GET_SIGNATURES", "I");
        if (getSignaturesFid == nullptr) {
            env->DeleteLocalRef(packageManagerInterfaceClass);
            if (env->ExceptionCheck()) {
                env->ExceptionClear();
            }
            return nullptr;
        }
        flags = env->GetStaticIntField(packageManagerInterfaceClass, getSignaturesFid);
        if (env->ExceptionCheck()) {
            env->ExceptionClear();
            env->DeleteLocalRef(packageManagerInterfaceClass);
            return nullptr;
        }
        env->DeleteLocalRef(packageManagerInterfaceClass);
    }

    jobject packageInfo = env->CallObjectMethod(packageManager, getPackageInfoMid, package_name, flags);
    if (!packageInfo || env->ExceptionCheck()) {
        env->ExceptionClear();
        return nullptr;
    }

    jclass packageInfoClass = env->GetObjectClass(packageInfo);
    jobjectArray signersArray = nullptr;

    if (sdkInt >= 28) {
        jclass signingInfoClass = env->FindClass("android/content/pm/SigningInfo");
        jobject singingInfo = env->GetObjectField(packageInfo, env->GetFieldID(packageInfoClass, "signingInfo", "Landroid/content/pm/SigningInfo;"));
        jmethodID getApkContentsSignersMid = env->GetMethodID(signingInfoClass, "getApkContentsSigners", "()[Landroid/content/pm/Signature;");
        signersArray = (jobjectArray) env->CallObjectMethod(singingInfo, getApkContentsSignersMid);
    } else {
        jfieldID signaturesFid = env->GetFieldID(packageInfoClass, "signatures", "[Landroid/content/pm/Signature;");
        signersArray = (jobjectArray) env->GetObjectField(packageInfo, signaturesFid);
    }

    if (!signersArray || env->GetArrayLength(signersArray) == 0) {
        return nullptr;
    }

    jobject firstSigner = env->GetObjectArrayElement(signersArray, 0);
    jclass signatureClass = env->GetObjectClass(firstSigner);
    jmethodID toByteArrayMid = env->GetMethodID(signatureClass, "toByteArray", "()[B");
    jbyteArray signatureBytes = (jbyteArray) env->CallObjectMethod(firstSigner, toByteArrayMid);
    if (!signatureBytes || env->ExceptionCheck()) {
        env->ExceptionClear();
        return nullptr;
    }

    // 获取 MessageDigest
    jclass mdClass = env->FindClass("java/security/MessageDigest");
    jmethodID getInstanceMid = env->GetStaticMethodID(mdClass, "getInstance", "(Ljava/lang/String;)Ljava/security/MessageDigest;");
    jstring algo = algorithm ? algorithm : env->NewStringUTF("SHA-256");
    jobject messageDigest = env->CallStaticObjectMethod(mdClass, getInstanceMid, algo);
    if (!messageDigest || env->ExceptionCheck()) {
        env->ExceptionClear();
        return nullptr;
    }

    jmethodID updateMid = env->GetMethodID(mdClass, "update", "([B)V");
    env->CallVoidMethod(messageDigest, updateMid, signatureBytes);

    jmethodID digestMid = env->GetMethodID(mdClass, "digest", "()[B");
    jbyteArray digestArray = (jbyteArray) env->CallObjectMethod(messageDigest, digestMid);
    if (!digestArray || env->ExceptionCheck()) {
        env->ExceptionClear();
        return nullptr;
    }

    // 调用 bytesToHex
    jstring hexString = bytesToHex(env, nullptr, digestArray);

    return hexString;
}