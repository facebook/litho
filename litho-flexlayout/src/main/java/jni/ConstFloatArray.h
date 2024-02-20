// (c) Meta Platforms, Inc. and affiliates. Confidential and proprietary.

#include "ScopedLocalRef.h"

namespace facebook {
namespace flexlayout {
namespace jni {

/**
 RAII wrapper around Java float array.
 */
struct ConstFloatArray {
  ConstFloatArray(JNIEnv* env, ScopedLocalRef<jfloatArray> array)
      : _env{env}, _array{std::move(array)} {
    _data = env->GetFloatArrayElements(_array.get(), nullptr);
    _size = env->GetArrayLength(_array.get());
  }

  ConstFloatArray(ConstFloatArray&& other) noexcept
      : _env{other._env},
        _array{std::move(other._array)},
        _data{other._data},
        _size{other._size} {
    other._env = nullptr;
    other._data = nullptr;
    other._size = 0;
  }

  ConstFloatArray(const ConstFloatArray&) = delete;
  auto operator=(const ConstFloatArray&) -> ConstFloatArray& = delete;

  auto size() const {
    return _size;
  }

  auto operator[](jsize idx) const -> jfloat {
    return _data[idx];
  }

  ~ConstFloatArray() {
    if (!_array || _data == nullptr) {
      return;
    }
    // Safe to use JNI_ABORT (which won't copy back the array) as we guarantee
    // the array was never mutated
    _env->ReleaseFloatArrayElements(_array.get(), _data, JNI_ABORT);
  }

 private:
  JNIEnv* _env;
  ScopedLocalRef<jfloatArray> _array;
  jfloat* _data;
  jsize _size;
};

} // namespace jni
} // namespace flexlayout
} // namespace facebook
