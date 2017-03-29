/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

#pragma once

/** @file CoreClasses.h
 *
 * In CoreClasses.h wrappers for the core classes (jobject, jclass, and jstring) is defined
 * to provide access to corresponding JNI functions + some conveniance.
 */

#include "References-forward.h"
#include "Meta-forward.h"
#include "TypeTraits.h"

#include <memory>

#include <jni.h>

#include <fb/visibility.h>

namespace facebook {
namespace jni {

class JClass;
class JObject;

/// Lookup a class by name. Note this functions returns an alias_ref that
/// points to a leaked global reference.  This is appropriate for classes
/// that are never unloaded (which is any class in an Android app and most
/// Java programs).
///
/// The most common use case for this is storing the result
/// in a "static auto" variable, or a static global.
///
/// @return Returns a leaked global reference to the class
FBEXPORT alias_ref<JClass> findClassStatic(const char* name);

/// Lookup a class by name. Note this functions returns a local reference,
/// which means that it must not be stored in a static variable.
///
/// The most common use case for this is one-time initialization
/// (like caching method ids).
///
/// @return Returns a global reference to the class
FBEXPORT local_ref<JClass> findClassLocal(const char* name);

/// Check to see if two references refer to the same object. Comparison with nullptr
/// returns true if and only if compared to another nullptr. A weak reference that
/// refers to a reclaimed object count as nullptr.
FBEXPORT bool isSameObject(alias_ref<JObject> lhs, alias_ref<JObject> rhs) noexcept;

// Together, these classes allow convenient use of any class with the fbjni
// helpers.  To use:
//
// struct MyClass : public JavaClass<MyClass> {
//   constexpr static auto kJavaDescriptor = "Lcom/example/package/MyClass;";
// };
//
// Then, an alias_ref<MyClass::javaobject> will be backed by an instance of
// MyClass. JavaClass provides a convenient way to add functionality to these
// smart references.
//
// For example:
//
// struct MyClass : public JavaClass<MyClass> {
//   constexpr static auto kJavaDescriptor = "Lcom/example/package/MyClass;";
//
//   void foo() {
//     static auto method = javaClassStatic()->getMethod<void()>("foo");
//     method(self());
//   }
//
//   static local_ref<javaobject> create(int i) {
//     return newInstance(i);
//   }
// };
//
// auto obj = MyClass::create(10);
// obj->foo();
//
// While users of a JavaClass-type can lookup methods and fields through the
// underlying JClass, those calls can only be checked at runtime. It is recommended
// that the JavaClass-type instead explicitly expose it's methods as in the example
// above.

namespace detail {
template<typename JC, typename... Args>
static local_ref<JC> newInstance(Args... args);
}

class MonitorLock;

class FBEXPORT JObject : detail::JObjectBase {
public:
  static constexpr auto kJavaDescriptor = "Ljava/lang/Object;";

  static constexpr const char* get_instantiated_java_descriptor() { return nullptr; }
  static constexpr const char* get_instantiated_base_name() { return nullptr; }

  /// Get a @ref local_ref of the object's class
  local_ref<JClass> getClass() const noexcept;

  /// Checks if the object is an instance of a class
  bool isInstanceOf(alias_ref<JClass> cls) const noexcept;

  /// Get the primitive value of a field
  template<typename T>
  T getFieldValue(JField<T> field) const noexcept;

  /// Get and wrap the value of a field in a @ref local_ref
  template<typename T>
  local_ref<T*> getFieldValue(JField<T*> field) const noexcept;

  /// Set the value of field. Any Java type is accepted, including the primitive types
  /// and raw reference types.
  template<typename T>
  void setFieldValue(JField<T> field, T value) noexcept;

  /// Convenience method to create a std::string representing the object
  std::string toString() const;

  // Take this object's monitor lock
  MonitorLock lock() const noexcept;

  typedef _jobject _javaobject;
  typedef _javaobject* javaobject;

protected:
  jobject self() const noexcept;
private:
  friend void swap(JObject& a, JObject& b) noexcept;
  template<typename>
  friend struct detail::ReprAccess;
  template<typename, typename, typename>
  friend class JavaClass;

  template <typename, typename>
  friend class JObjectWrapper;
};

// This is only to maintain backwards compatibility with things that are
// already providing a specialization of JObjectWrapper. Any such instances
// should be updated to use a JavaClass.
template<>
class JObjectWrapper<jobject> : public JObject {
};


namespace detail {
template <typename, typename Base, typename JType>
struct JTypeFor {
  static_assert(
      std::is_base_of<
        std::remove_pointer<jobject>::type,
        typename std::remove_pointer<JType>::type
      >::value, "");
  using _javaobject = typename std::remove_pointer<JType>::type;
  using javaobject = JType;
};

template <typename T, typename Base>
struct JTypeFor<T, Base, void> {
  // JNI pattern for jobject assignable pointer
  struct _javaobject :  Base::_javaobject {
    // This allows us to map back to the defining type (in ReprType, for
    // example).
    typedef T JniRefRepr;
  };
  using javaobject = _javaobject*;
};
}

// JavaClass provides a method to inform fbjni about user-defined Java types.
// Given a class:
// struct Foo : JavaClass<Foo> {
//   static constexpr auto kJavaDescriptor = "Lcom/example/package/Foo;";
// };
// fbjni can determine the java type/method signatures for Foo::javaobject and
// smart refs (like alias_ref<Foo::javaobject>) will hold an instance of Foo
// and provide access to it through the -> and * operators.
//
// The "Base" template argument can be used to specify the JavaClass superclass
// of this type (for instance, JString's Base is JObject).
//
// The "JType" template argument is used to provide a jni type (like jstring,
// jthrowable) to be used as javaobject. This should only be necessary for
// built-in jni types and not user-defined ones.
template <typename T, typename Base = JObject, typename JType = void>
class FBEXPORT JavaClass : public Base {
  using JObjType = typename detail::JTypeFor<T, Base, JType>;
public:
  using _javaobject = typename JObjType::_javaobject;
  using javaobject = typename JObjType::javaobject;

  using JavaBase = JavaClass;

  static alias_ref<JClass> javaClassStatic();
  static local_ref<JClass> javaClassLocal();
protected:
  /// Allocates a new object and invokes the specified constructor
  /// Like JClass's getConstructor, this function can only check at runtime if
  /// the class actually has a constructor that accepts the corresponding types.
  /// While a JavaClass-type can expose this function directly, it is recommended
  /// to instead to use this to explicitly only expose those constructors that
  /// the Java class actually has (i.e. with static create() functions).
  template<typename... Args>
  static local_ref<T> newInstance(Args... args) {
    return detail::newInstance<T>(args...);
