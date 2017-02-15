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

#include "SJCL.h";
#include <openssl/evp.h>
#include <JSON.h>
#include "base64.h"
#include <android/log.h>

#define LOG_TAG "SJCL"

void handleErrors(const char* error){
    __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, (const char*)"FUCK THIS SHIT GOT AN ERROR: %s", error);
}

int decryptccm(unsigned char *ciphertext, int ciphertext_len, unsigned char *aad,
               int aad_len, unsigned char *tag, unsigned char *key, unsigned char *iv,
               unsigned char *plaintext
) {
    EVP_CIPHER_CTX *ctx;
    int len;
    int plaintext_len;
    int ret;

    /* Create and initialise the context */
    if(!(ctx = EVP_CIPHER_CTX_new())) handleErrors("Error initializing context");

    /* Initialise the decryption operation. */
    if(1 != EVP_DecryptInit_ex(ctx, EVP_aes_256_ccm(), NULL, NULL, NULL)) handleErrors("Error setting crypto mode");

    int lol = 2;
    if (ciphertext_len >= 1<<16) lol++;
    if (ciphertext_len >= 1<<24) lol++;

    if(1 != EVP_CIPHER_CTX_ctrl(ctx, EVP_CTRL_CCM_SET_IVLEN, 15-lol, NULL)) handleErrors("Error setting IV Length");

    /* Set expected tag value. */
    if(1 != EVP_CIPHER_CTX_ctrl(ctx, EVP_CTRL_CCM_SET_TAG, 8, tag)) handleErrors("Error setting TAG value");

    /* Initialise key and IV */
    if(1 != EVP_DecryptInit_ex(ctx, NULL, NULL, key, iv)) handleErrors("Error setting KEY and IV");

    /* Provide the total ciphertext length
     */
    if(1 != EVP_DecryptUpdate(ctx, NULL, &len, NULL, ciphertext_len)) handleErrors("Error setting cyphertext length");

    /* Provide any AAD data. This can be called zero or more times as
     * required
     */
    if(1 != EVP_DecryptUpdate(ctx, NULL, &len, aad, aad_len)) handleErrors("Error setting AAD data");

    /* Provide the message to be decrypted, and obtain the plaintext output.
     * EVP_DecryptUpdate can be called multiple times if necessary
     */
    ret = EVP_DecryptUpdate(ctx, plaintext, &len, ciphertext, ciphertext_len);

    plaintext_len = len;

    /* Clean up */
    EVP_CIPHER_CTX_free(ctx);

    if(ret > 0)
    {
        /* Success */
        return plaintext_len;
    }
    else
    {
        /* Verify failed */
        return -1;
    }
}

/**
 * Casts an WString to an standard char array, beware, it does not care about encoding!
 * It just discards the wide part of the chars!
 */
char* wstring_to_char(wstring str) {
    char* c = (char *) malloc(sizeof(char) * str.length() + 1);
    const wchar_t* data = str.c_str();
    for (int i = 0; i <= str.length(); i++) {
        c[i] = (char) data[i];
    }
    return c;
}

using namespace WLF::Crypto;

char* SJCL::decrypt(string sjcl_json, string key) {
    JSONValue *data = JSON::Parse(sjcl_json.c_str());

    if (data == NULL || ! data->IsObject()) {
        __android_log_write(ANDROID_LOG_ERROR, LOG_TAG, "Error parsing the SJCL JSON");
        return NULL;
    }

    JSONObject food = data->AsObject();

    int iter        = 0,
        key_size    = 0,
        tag_size    = 0;
    char *iv_64, *salt_64, *cyphertext, *adata;

    // Extract the requiered values from the JSON
    iv_64       = wstring_to_char(food[L"iv"]->AsString());
    adata       = wstring_to_char(food[L"adata"]->AsString());
    salt_64     = wstring_to_char(food[L"salt"]->AsString());
    cyphertext  = wstring_to_char(food[L"ct"]->AsString());

    iter        = (int) food[L"iter"]->AsNumber();
    key_size    = (int) food[L"ks"]->AsNumber();
    tag_size    = (int) food[L"ts"]->AsNumber();

    tag_size /= 8; // Make it bytes!
    key_size /= 8; // Make it bytes

    // The actual cryptogram includes the tag size, so we need to take this into account later on!
    Datagram* cryptogram = BASE64::decode((unsigned char *) cyphertext, strlen(cyphertext));
    int cyphertext_data_length = cryptogram->length - tag_size;

    Datagram* salt = BASE64::decode((unsigned char *) salt_64, strlen(salt_64));
    Datagram* iv_raw = BASE64::decode((unsigned char *) iv_64, strlen(iv_64));
//    Datagram* aadata = BASE64::decode((const unsigned char *) "", 0); // Not sure if this is required since we don't use adata

    unsigned char* derived_key = (unsigned char*) malloc(sizeof(unsigned char) * key_size);

    // Assuming plaintext will always be smaller than the sjcl cyphertext which includes the tag and padding and stuff
    unsigned char* plaintext = (unsigned char*) malloc(sizeof(unsigned char) * strlen(cyphertext));
    // Ensure plaintext ends up null terminated
    for (int i = 0; i < strlen(cyphertext); i++) plaintext[i] = '\0';
    string s = string("The allocated string is: ") + string((char*)plaintext);

    /* PBKDF2 Key derivation with SHA256 as SJCL does by default */
    PKCS5_PBKDF2_HMAC(key.c_str(), key.length(), salt->data, salt->length, iter, EVP_sha256(), key_size, derived_key);

    char* ret = NULL;
    // Decrypt the data
    if (0 < decryptccm(cryptogram->data, cyphertext_data_length, (unsigned char *) adata, strlen(adata),
       &cryptogram->data[cyphertext_data_length], derived_key, iv_raw->data, plaintext)
    ) {
        // Try to make strings strings instead of json encoded strings
        JSONValue *result = JSON::Parse((char *) plaintext);
        if (result->IsString()) {
            ret =  wstring_to_char(result->AsString());
            free(plaintext);
            free(result);
        }
        else {
            ret = (char *) plaintext;
        }
    }

    // Free up resources
    free(iv_64);
    free(adata);
    free(salt_64);
    free(cyphertext);
    free(data);
    free(derived_key);
//    free(food);

    return ret;
}
