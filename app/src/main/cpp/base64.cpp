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

#include "base64.h"
#include <openssl/bio.h>
#include <openssl/evp.h>

using namespace WLF::Crypto;

static const char base64_table[] = {
        'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
        'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
        'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
        'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '+', '/', '\0'
};

static const char base64_pad = '=';

static const short base64_reverse_table[256] = {
        -2, -2, -2, -2, -2, -2, -2, -2, -2, -1, -1, -2, -2, -1, -2, -2,
        -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2,
        -1, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, 62, -2, -2, -2, 63,
        52, 53, 54, 55, 56, 57, 58, 59, 60, 61, -2, -2, -2, -2, -2, -2,
        -2,  0,  1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14,
        15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, -2, -2, -2, -2, -2,
        -2, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40,
        41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, -2, -2, -2, -2, -2,
        -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2,
        -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2,
        -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2,
        -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2,
        -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2,
        -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2,
        -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2,
        -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2
};

Datagram* BASE64::decode(const unsigned char *str, int length) {
    Datagram* ret = new Datagram;
    const unsigned char *current = str;
    int ch, i = 0, j = 0, k;

    ret->data = (unsigned char *)malloc(length+1);
    if (ret->data == NULL) {
        fprintf(stderr, "out of memory!\n");
        exit(1);
    }

    while ((ch = *current++) != '\0' && length-- > 0) {
        if (ch == base64_pad) {
            if (*current != '=' && (i % 4) == 1) {
                free(ret->data);
                return NULL;
            }
            continue;
        }

        ch = base64_reverse_table[ch];
        if ((1 && ch < 0) || ch == -1) {
            continue;
        } else if (ch == -2) {
            free(ret->data);
            return NULL;
        }

        switch(i % 4) {
            case 0:
                ret->data[j] = ch << 2;
                break;
            case 1:
                ret->data[j++] |= ch >> 4;
                ret->data[j] = (ch & 0x0f) << 4;
                break;
            case 2:
                ret->data[j++] |= ch >>2;
                ret->data[j] = (ch & 0x03) << 6;
                break;
            case 3:
                ret->data[j++] |= ch;
                break;
        }
        i++;
    }

    k = j;

    if (ch == base64_pad) {
        switch(i % 4) {
            case 1:
                free(ret->data);
                return NULL;
            case 2:
                k++;
            case 3:
                ret->data[k] = 0;
        }
    }

    ret->data[j] = '\0';
    ret->length = j;
    return ret;
}

Datagram* BASE64::encode(const unsigned char *str, int length) {
    Datagram* ret = new Datagram;
    const unsigned char *current = str;
    unsigned char *p;

    if ((length + 2) < 0 || ((length + 2) / 3) >= (1 << (sizeof(int) * 8 - 2))) {
        ret->data = NULL;
        ret->length = 0;
        return ret;
    }

    ret->data = (unsigned char *)malloc((((length + 2) / 3) * 4)*(sizeof(char))+(1));
    if (ret->data == NULL) {
        fprintf(stderr, "out of memory!\n");
        exit(1);
    }
    p = ret->data;

    while (length > 2) {
        *p++ = base64_table[current[0] >> 2];
        *p++ = base64_table[((current[0] & 0x03) << 4) + (current[1] >> 4)];
        *p++ = base64_table[((current[1] & 0x0f) << 2) + (current[2] >> 6)];
        *p++ = base64_table[current[2] & 0x3f];

        current += 3;
        length -= 3;
    }

    if (length != 0) {
        *p++ = base64_table[current[0] >> 2];
        if (length > 1) {
            *p++ = base64_table[((current[0] & 0x03) << 4) + (current[1] >> 4)];
            *p++ = base64_table[(current[1] & 0x0f) << 2];
            *p++ = base64_pad;
        } else {
            *p++ = base64_table[(current[0] & 0x03) << 4];
            *p++ = base64_pad;
            *p++ = base64_pad;
        }
    }
    ret->length = (int)(p - ret->data);
    *p = '\0';
    return ret;
}
