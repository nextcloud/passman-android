#include <string>

using namespace std;
namespace WLF {
    namespace Crypto {
        class SJCL {
        public:
            static char* decrypt(string sjcl_json, string key);
            static char* encrypt(char* message, string key);
        };
    }
}