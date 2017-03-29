/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

#pragma once

#include <string.h>
#include <type_traits>
#include <stdlib.h>

#include "Common.h"
#include "Exceptions.h"
#include "Meta.h"
#include "MetaConvert.h"

namespace facebook {
namespace jni {

// jobject /////////////////////////////////////////////////////////////////////////////////////////

inline bool isSameObject(alias_ref<JObject> lhs, alias_ref<JObject> rhs) noexcept {
  return internal::getEnv()->IsSameObject(lhs.get(), rhs.get()) != JNI_FALSE;
}

inline local_ref<JClass> JObject::getClass() const noexcept {
  return adopt_local(internal::getEnv()->GetObjectClass(self()));
}

inline bool JObject::isInstanceOf(alias_ref<JClass> cls) const noexcept {
  return internal::getEnv()->IsInstanceOf(self(), cls.get()) != JNI_FALSE;
}

template<typename T>
inline T JObject::getFieldValue(JField<T> field) const noexcept {
  return field.get(self());
}

template<typename T>
inline local_ref<T*> JObject::getFieldValue(JField<T*> field) const noexcept {
  return adopt_local(field.get(self()));
}

template<typename T>
inline void JObject::setFieldValue(JField<T> field, T value) noexcept {
  field.set(self(), value);
}

inline std::string JObject::toString() const {
  static auto method = findClassLocal("java/lang/Object")->getMethod<jstring()>("toString");

  return method(self())->toStdString();
}


// Class is here instead of CoreClasses.h because we need
// alias_ref to be complete.
class MonitorLock {
 public:
  inline MonitorLock() noexcept;
  inline MonitorLock(alias_ref<JObject> object) noexcept;
  inline ~MonitorLock() noexcept;

  inline MonitorLock(MonitorLock&& other) noexcept;
  inline MonitorLock& operator=(MonitorLock&& other) noexcept;

  inline MonitorLock(const MonitorLock&) = delete;
  inline MonitorLock& operator=(const MonitorLock&) = delete;

 private:
  inline void reset() noexcept;
  alias_ref<JObject> owned_;
};

MonitorLock::MonitorLock() noexcept : owned_(nullptr) {}

MonitorLock::MonitorLock(alias_ref<JObject> object) noexcept
    : owned_(object) {
  internal::getEnv()->MonitorEnter(object.get());
}

void MonitorLock::reset() noexcept {
  if (owned_) {
    internal::getEnv()->MonitorExit(owned_.get());
    if (internal::getEnv()->ExceptionCheck()) {
      abort(); // Lock mismatch
    }
    owned_ = nullptr;
  }
}

MonitorLock::~MonitorLock() noexcept {
  reset();
}

MonitorLock::MonitorLock(MonitorLock&& other) noexcept
    : owned_(other.owned_)
{
  other.owned_ = nullptr;
}

MonitorLock& MonitorLock::operator=(MonitorLock&& other) noexcept {
  reset();
  owned_ = other.owned_;
  other.owned_ = nullptr;
  return *this;
}

inline MonitorLock JObject::lock() const noexcept {
  return MonitorLock(this_);
}

inline jobject JObject::self() const noexcept {
  return this_;
}

inline void swap(JObject& a, JObject& b) noexcept {
  using std::swap;
  swap(a.this_, b.this_);
}

// JavaClass ///////////////////////////////////////////////////////////////////////////////////////

namespace detail {
template<typename JC, typename... Args>
static local_ref<JC> newInstance(Args... args) {
  static auto cls = JC::javaClassStatic();
  static auto constructor = cls->template getConstructor<typename JC::javaobject(Args...)>();
  return cls->newObject(constructor, args...);
}
}


template <typename T, typename B, typename J>
auto JavaClass<T, B, J>::self() const noexcept -> javaobject {
  return static_cast<javaobject>(JObject::self());
}

// jclass //////////////////////////////////////////////////////////////////////////////////////////

namespace detail {

// This is not a real type.  It is used so people won't accidentally
// use a void* to initialize a NativeMethod.
struct NativeMethodWrapper;

}

struct NativeMethod {
  const char* name;
  std::string descriptor;
  detail::NativeMethodWrapper* wrapper;
};

inline local_ref<JClass> JClass::getSuperclass() const noexcept {
  return adopt_local(internal::getEnv()->GetSuperclass(self()));
}

inline void JClass::registerNatives(std::initializer_list<NativeMethod> methods) {
  const auto env = internal::getEnv();

  JNINativeMethod jnimethods[methods.size()];
  size_t i = 0;
  for (auto it = methods.begin(); it < methods.end(); ++it, ++i) {
    jnimethods[i].name = it->name;
    jnimethods[i].signature = it->descriptor.c_str();
    jnimethods[i].fnPtr = reinterpret_cast<void*>(it->wrapper);
  }

  auto result = env->RegisterNatives(self(), jnimethods, methods.size());
  FACEBOOK_JNI_THROW_EXCEPTION_IF(result != JNI_OK);
}

inline bool JClass::isAssignableFrom(alias_ref<JClass> other) const noexcept {
  const auto env = internal::getEnv();
  const auto result = env->IsAssignableFrom(self(), other.get());
  return result;
}

template<typename F>
inline JConstructor<F> JClass::getConstructor() const {
  return getConstructor<F>(jmethod_traits_from_cxx<F>::constructor_descriptor().c_str());
}

template<typename F>
inline JConstructor<F> JClass::getConstructor(const char* descriptor) const {
  constexpr auto constructor_method_name = "<init>";
  return getMethod<F>(constructor_method_name, descriptor);
}

template<typename F>
inline JMethod<F> JClass::getMethod(const char* name) const {
  return getMethod<F>(name, jmethod_traits_from_cxx<F>::descriptor().c_str());
}

template<typename F>
inline JMethod<F> JClass::getMethod(
    const char* name,
    const char* descriptor) const {
  const auto env = internal::getEnv();
  const auto method = env->GetMethodID(self(), name, descriptor);
  FACEBOOK_JNI_THROW_EXCEPTION_IF(!method);
  return JMethod<F>{method};
}

template<typename F>
inline JStaticMethod<F> JClass::getStaticMethod(const char* name) const {
  return getStaticMethod<F>(name, jmethod_traits_from_cxx<F>::descriptor().c_str());
}

template<typename F>
inline JStaticMethod<F> JClass::getStaticMethod(
    const char* name,
    const char* descriptor) const {
  const auto env = internal::getEnv();
  const auto method = env->GetStaticMethodID(self(), name, descriptor);
  FACEBOOK_JNI_THROW_EXCEPTION_IF(!method);
  return JStaticMethod<F>{method};
}

template<typename F>
inline JNonvirtualMethod<F> JClass::getNonvirtualMethod(const char* name) const {
  return getNonvirtualMethod<F>(name, jmethod_traits_from_cxx<F>::descriptor().c_str());
}

template<typename F>
inline JNonvirtualMethod<F> JClass::getNonvirtualMethod(
    const char* name,
    const char* descriptor) const {
  const auto env = internal::getEnv();
  const auto method = env->GetMethodID(self(), name, descriptor);
  FACEBOOK_JNI_THROW_EXCEPTION_IF(!method);
  return JNonvirtualMethod<F>{method};
}

template<typename T>
inline JField<enable_if_t<IsJniScalar<T>(), T>>
JClass::getField(const char* name) const {
  return getField<T>(name, jtype_traits<T>::descriptor().c_str());
}

template<typename T>
inline JField<enable_if_t<IsJniScalar<T>(), T>> JClass::getField(
    const char* name,
    const char* descriptor) const {
  const auto env = internal::getEnv();
  auto field = env->GetFieldID(self(), name, descriptor);
  FACEBOOK_JNI_THROW_EXCEPTION_IF(!field);
  return JField<T>{field};
}

template<typename T>
inline JStaticField<enable_if_t<IsJniScalar<T>(), T>> JClass::getStaticField(
    const char* name) const {
  return getStaticField<T>(name, jtype_traits<T>::descriptor().c_str());
}

template<typename T>
inline JStaticField<enable_if_t<IsJniScalar<T>(), T>> JClass::getStaticField(
    const char* name,
    const char* descriptor) const {
  const auto env = internal::getEnv();
  auto field = env->GetStaticFieldID(self(), name, descriptor);
  FACEBOOK_JNI_THROW_EXCEPTION_IF(!field);
  return JStaticField<T>{field};
}

template<typename T>
inline T JClass::getStaticFieldValue(JStaticField<T> field) const noexcept {
  return field.get(self());
}

template<typename T>
inline local_ref<T*> JClass::getStaticFieldValue(JStaticField<T*> field) noexcept {
  return adopt_local(field.get(self()));
}

template<typename T>
inline void JClass::setStaticFieldValue(JStaticField<T> field, T value) noexcept {
  field.set(self(), value);
}

template<typename R, typename... Args>
inline local_ref<R> JClass::newObject(
    JConstructor<R(Args...)> constructor,
    Args... args) const {
  const auto env = internal::getEnv();
  auto object = env->NewObject(self(), constructor.getId(),
      detail::callToJni(
        detail::Convert<typename std::decay<Args>::type>::toCall(args))...);
  FACEBOOK_JNI_THROW_EXCEPTION_IF(!object);
  return adopt_local(static_cast<R>(object));
}

inline jclass JClass::self() const noexcept {
  return static_cast<jclass>(JObject::self());
}

inline void registerNatives(const char* name, std::initializer_list<NativeMethod> methods) {
  findClassLocal(name)->registerNatives(methods);
}


// jstring /////////////////////////////////////////////////////////////////////////////////////////

inline local_ref<JString> make_jstring(const std::string& modifiedUtf8) {
  return make_jstring(modifiedUtf8.c_str());
}

namespace detail {
// convert to std::string from jstring
template <>
struct Convert<std::string> {
  typedef jstring jniType;
  static std::string fromJni(jniType t) {
    return wrap_alias(t)->toStdString();
  }
  static jniType toJniRet(const std::string& t) {
    return make_jstring(t).release();
  }
  static local_ref<JString> toCall(const std::string& t) {
    return make_jstring(t);
  }
};

// convert return from const char*
template <>
struct Convert<const char*> {
  typedef jstring jniType;
  // no automatic synthesis of const char*.  (It can't be freed.)
  static jniType toJniRet(const char* t) {
    return make_jstring(t).release();
  }
  static local_ref<JString> toCall(const char* t) {
    return make_jstring(t);
  }
};
}

