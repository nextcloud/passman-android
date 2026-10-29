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
#include <openssl/crypto.h>
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

/**
 * Copies the Java string into a std::string without an intermediate JNI buffer, which couldn't be wiped.
 */
std::string toStdString(JNIEnv *env, jstring str)
{
    jsize length = env->GetStringUTFLength(str);
    // one more byte in case the VM adds a terminating null character
    std::string result(length + 1, '\0');
    env->GetStringUTFRegion(str, 0, env->GetStringLength(str), &result[0]);
    result.resize(length);
    return result;
}

void wipe(std::string &secret)
{
    OPENSSL_cleanse(&secret[0], secret.size());
}

extern "C" {

jstring Java_es_wolfi_app_passman_SJCLCrypto_decryptStringCpp(JNIEnv *env, jclass jthis, jstring cryptogram, jstring key) {
    std::string dt = toStdString(env, cryptogram);
    std::string password = toStdString(env, key);

    WLF::Crypto::Datagram *t = WLF::Crypto::BASE64::decode((const unsigned char *) dt.c_str(),
                                                           dt.length());

    std::string json = (char *) t->data;
    free(t);

    //__android_log_write(ANDROID_LOG_ERROR, LOG_TAG, json.c_str());

    char *result = WLF::Crypto::SJCL::decrypt(json, password);
    wipe(password);

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
    std::string dt = toStdString(env, plaintext);
    std::string password = toStdString(env, key);

    //__android_log_write(ANDROID_LOG_ERROR, LOG_TAG, dt.c_str());

    char *result = WLF::Crypto::SJCL::encrypt(const_cast<char *>(dt.c_str()), password);
    wipe(dt);
    wipe(password);
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