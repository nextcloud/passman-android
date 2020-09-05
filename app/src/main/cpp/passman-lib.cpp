/**
 *  Passman Android App
 *
 * @copyright Copyright (c) 2016, Sander Brand (brantje@gmail.com)
 * @copyright Copyright (c) 2016, Marcos Zuriaga Miguel (wolfi@wolfi.es)
 * @license GNU AGPL version 3 or any later version
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

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

jstring Java_es_wolfi_app_passman_SJCLCrypto_decryptStringCpp(JNIEnv *env, jclass jthis, jstring cryptogram, jstring key) {
    std::string dt = env->GetStringUTFChars(cryptogram, 0);
    std::string password = env->GetStringUTFChars(key, 0);

    WLF::Crypto::Datagram *t = WLF::Crypto::BASE64::decode((const unsigned char *) dt.c_str(),
                                                           dt.length());

    std::string json = (char *) t->data;
    free(t);

    //__android_log_write(ANDROID_LOG_ERROR, LOG_TAG, json.c_str());

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
    return (jstring) "";
}

jstring Java_es_wolfi_app_passman_SJCLCrypto_encryptStringCpp(JNIEnv *env, jclass jthis, jstring plaintext, jstring key) {
    std::string dt = env->GetStringUTFChars(plaintext, 0);
    std::string password = env->GetStringUTFChars(key, 0);

    //__android_log_write(ANDROID_LOG_ERROR, LOG_TAG, dt.c_str());

    char *result = WLF::Crypto::SJCL::encrypt(const_cast<char *>(dt.c_str()), password);
    //__android_log_write(ANDROID_LOG_ERROR, LOG_TAG, result);

    char *arr_result = &result[0];
    std::string lengthAsString = to_string(strlen(arr_result));

    if (result == NULL) {
        __android_log_write(ANDROID_LOG_ERROR, LOG_TAG, "error encrypting");
    }
    else {
        //__android_log_write(ANDROID_LOG_ERROR, LOG_TAG, "WHOOP!");
        //__android_log_write(ANDROID_LOG_ERROR, LOG_TAG, result);
        WLF::Crypto::Datagram *t = WLF::Crypto::BASE64::encode((const unsigned char *) result,
                                                               strlen(arr_result));

        jstring str = env->NewStringUTF((char *) t->data);

        free(t);
        return str;
    }

    throwJavaException(env, "Error encrypting");
    return NULL;
}

}