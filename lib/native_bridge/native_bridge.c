// native_bridge.c (概要)
// コンパイル時: gcc -shared -fPIC native_bridge.c -o libnativebridge.so -lffi -ldl
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <dlfcn.h>
#include <ffi.h>

// シンプルにキャッシュは省略（実用ならキャッシュ推奨）

// ライブラリ開く
void *open_library(const char *path)
{
    void *handle = dlopen(path, RTLD_LAZY);
    if (!handle)
    {
        fprintf(stderr, "dlopen error: %s\n", dlerror());
    }
    return handle;
}

void close_library(void *handle)
{
    if (handle)
        dlclose(handle);
}

void *lookup_symbol(void *handle, const char *name)
{
    if (!handle)
        return NULL;
    return dlsym(handle, name);
}

typedef struct {
    ffi_type *return_type;
    ffi_type **arg_types;
    int nargs;
} ParsedMethodDescriptor;

int parse_method_descriptor(const char *desc, ParsedMethodDescriptor *out)
{
    if (desc[0] != '(') return -1;
    int pos = 1;
    int arg_cap = 16;
    ffi_type **args = malloc(sizeof(ffi_type*) * arg_cap);
    if (!args) return -2;

    while (desc[pos] && desc[pos] != ')')
    {
        ffi_type *t = parse_type_from_descriptor(desc, &pos);
        if (!t)
        {
            free(args);
            return -3;
        }
        if (out->nargs >= arg_cap)
        {
            arg_cap *= 2;
            ffi_type **tmp = realloc(args, sizeof(ffi_type*) * arg_cap);
            if (!tmp)
            {
                free(args);
                return -4;
            }
            args = tmp;
        }
        args[out->nargs++] = t;
    }

    if (desc[pos] != ')')
    {
        free(args);
        return -5;
    }
    pos++;

    ffi_type *ret_type = parse_type_from_descriptor(desc, &pos);
    if (!ret_type)
    {
        free(args);
        return -6;
    }

    out->return_type = ret_type;
    out->arg_types = args;
    return 0;
}

ffi_type *parse_type_from_descriptor(const char *desc, int *pos)
{
    char c = desc[*pos];
    (*pos)++;

    switch(c)
    {
        case 'V': return &ffi_type_void;
        case 'Z': return &ffi_type_uint8;  // boolean
        case 'B': return &ffi_type_sint8;
        case 'C': return &ffi_type_uint16;
        case 'S': return &ffi_type_sint16;
        case 'I': return &ffi_type_sint32;
        case 'J': return &ffi_type_sint64;
        case 'F': return &ffi_type_float;
        case 'D': return &ffi_type_double;
        case 'L': // オブジェクト。void*ポインタとして扱う。';'まで読む
            while(desc[*pos] && desc[*pos] != ';')
                (*pos)++;
            if (desc[*pos] == ';')
                (*pos)++;
            return &ffi_type_pointer;
        case '[': // 配列型もポインタ扱いでOK
            // 配列の中身の型は無視してポインタにする（簡略化）
            while(desc[*pos] == '[')
                (*pos)++;
            if (desc[*pos] == 'L')
            {
                (*pos)++;
                while(desc[*pos] && desc[*pos] != ';')
                    (*pos)++;
                if (desc[*pos] == ';')
                    (*pos)++;
            }
            else
            {
                // プリミティブ型
                (*pos)++;
            }
            return &ffi_type_pointer;
        default:
            return NULL;
    }
}

int invoke_method(long funcPtr, const char *methodDescriptor, jobjectArray args, JNIEnv *env, jobject *ret)
{
    ParsedMethodDescriptor pmd = {0};

    if (parse_method_descriptor(methodDescriptor, &pmd) != 0)
    {
        // パース失敗
        return -1;
    }

    ffi_cif cif;
    if (ffi_prep_cif(&cif, FFI_DEFAULT_ABI, pmd.nargs, pmd.return_type, pmd.arg_types) != FFI_OK)
    {
        free(pmd.arg_types);
        return -2;
    }

    void **native_args = malloc(sizeof(void*) * pmd.nargs);
    // 引数の変換処理を書く。JavaのObject→C型へ（env使う）

    // 戻り値用バッファ（64bitサイズ）
    uint64_t ret_storage = 0;

    ffi_call(&cif, (void *)funcPtr, &ret_storage, native_args);

    // 戻り値をJava Objectに変換（env使う）
    // 例: intなら(*env)->NewObjectなど

    free(native_args);
    free(pmd.arg_types);
    return 0;
}
