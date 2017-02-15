//
// Created by wolfi on 23/01/17.
//

#ifndef BASE64_H
#define BASE64_H

#include <cstdint>

namespace WLF {
    namespace Crypto {
        struct Datagram {
            unsigned char* data;
            int length;
        };

        class BASE64{

        public:
            static Datagram* encode(const unsigned char *str, int length);

            static Datagram* decode(const unsigned char *str, int length);
        };

    }
}

#endif //PASSMAN_BASE64_H
