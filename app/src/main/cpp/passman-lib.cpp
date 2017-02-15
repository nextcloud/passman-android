#include <jni.h>
#include <string>
#include <android/log.h>
#include <openssl/evp.h>
#include <JSON.h>

#include <limits>
#include <stdexcept>
#include "base64.h"
#include "SJCL.h"

#define LOG_TAG "Passman lib"

void throwJavaException(JNIEnv *env, const char *msg)
{
    // You can put your own exception here
    jclass c = env->FindClass("java/lang/RuntimeException");

    if (NULL == c)
    {
        //B plan: null pointer ...
        c = env->FindClass("java/lang/NullPointerException");
    }

    env->ThrowNew(c, msg);
}

extern "C" {

jstring Java_es_wolfi_app_passman_SJCLCrypto_decryptString(JNIEnv *env, jobject jthis, jstring cryptogram, jstring key) {
    std::string dt = env->GetStringUTFChars(cryptogram, 0);
    std::string password = env->GetStringUTFChars(key, 0);

    WLF::Crypto::Datagram *t = WLF::Crypto::BASE64::decode((const unsigned char *) dt.c_str(),
                                                           dt.length());

    std::string json = (char *) t->data;
    free(t);

//    __android_log_write(ANDROID_LOG_ERROR, LOG_TAG, json.c_str());

    char *result = WLF::Crypto::SJCL::decrypt(json, password);

    if (result == NULL) {
        __android_log_write(ANDROID_LOG_ERROR, LOG_TAG, "error decrypting");
    }
    else {
//        __android_log_write(ANDROID_LOG_ERROR, LOG_TAG, "WHOOP!");
//        __android_log_write(ANDROID_LOG_ERROR, LOG_TAG, result);
        jstring str = env->NewStringUTF(result);
        free(result);
        return str;
    }

    throwJavaException(env, "Error decrypting");
    return NULL;
}

}