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
  }

  javaobject self() const noexcept;
};

/// Wrapper to provide functionality to jclass references
struct NativeMethod;

class FBEXPORT JClass : public JavaClass<JClass, JObject, jclass> {
 public:
  /// Java type descriptor
  static constexpr const char* kJavaDescriptor = "Ljava/lang/Class;";

  /// Get a @local_ref to the super class of this class
  local_ref<JClass> getSuperclass() const noexcept;

  /// Register native methods for the class.  Usage looks like this:
  ///
  /// classRef->registerNatives({
  ///     makeNativeMethod("nativeMethodWithAutomaticDescriptor",
  ///                      methodWithAutomaticDescriptor),
  ///     makeNativeMethod("nativeMethodWithExplicitDescriptor",
  ///                      "(Lcom/facebook/example/MyClass;)V",
  ///                      methodWithExplicitDescriptor),
  ///  });
  ///
  /// By default, C++ exceptions raised will be converted to Java exceptions.
  /// To avoid this and get the "standard" JNI behavior of a crash when a C++
  /// exception is crashing out of the JNI method, declare the method noexcept.
  void registerNatives(std::initializer_list<NativeMethod> methods);

  /// Check to see if the class is assignable from another class
  /// @pre cls != nullptr
  bool isAssignableFrom(alias_ref<JClass> cls) const noexcept;

  /// Convenience method to lookup the constructor with descriptor as specified by the
  /// type arguments
  template<typename F>
  JConstructor<F> getConstructor() const;

  /// Convenience method to lookup the constructor with specified descriptor
  template<typename F>
  JConstructor<F> getConstructor(const char* descriptor) const;

  /// Look up the method with given name and descriptor as specified with the type arguments
  template<typename F>
  JMethod<F> getMethod(const char* name) const;

  /// Look up the method with given name and descriptor
  template<typename F>
  JMethod<F> getMethod(const char* name, const char* descriptor) const;

  /// Lookup the field with the given name and deduced descriptor
  template<typename T>
  JField<enable_if_t<IsJniScalar<T>(), T>> getField(const char* name) const;

  /// Lookup the field with the given name and descriptor
  template<typename T>
  JField<enable_if_t<IsJniScalar<T>(), T>> getField(const char* name, const char* descriptor) const;

  /// Lookup the static field with the given name and deduced descriptor
  template<typename T>
  JStaticField<enable_if_t<IsJniScalar<T>(), T>> getStaticField(const char* name) const;

  /// Lookup the static field with the given name and descriptor
  template<typename T>
  JStaticField<enable_if_t<IsJniScalar<T>(), T>> getStaticField(
      const char* name,
      const char* descriptor) const;

  /// Get the primitive value of a static field
  template<typename T>
  T getStaticFieldValue(JStaticField<T> field) const noexcept;

  /// Get and wrap the value of a field in a @ref local_ref
  template<typename T>
  local_ref<T*> getStaticFieldValue(JStaticField<T*> field) noexcept;

  /// Set the value of field. Any Java type is accepted, including the primitive types
  /// and raw reference types.
  template<typename T>
  void setStaticFieldValue(JStaticField<T> field, T value) noexcept;

  /// Allocates a new object and invokes the specified constructor
  template<typename R, typename... Args>
  local_ref<R> newObject(JConstructor<R(Args...)> constructor, Args... args) const;

  /// Look up the static method with given name and descriptor as specified with the type arguments
  template<typename F>
  JStaticMethod<F> getStaticMethod(const char* name) const;

  /// Look up the static method with given name and descriptor
  template<typename F>
  JStaticMethod<F> getStaticMethod(const char* name, const char* descriptor) const;

  /// Look up the non virtual method with given name and descriptor as specified with the
  /// type arguments
  template<typename F>
  JNonvirtualMethod<F> getNonvirtualMethod(const char* name) const;

  /// Look up the non virtual method with given name and descriptor
  template<typename F>
  JNonvirtualMethod<F> getNonvirtualMethod(const char* name, const char* descriptor) const;

private:
  jclass self() const noexcept;
};

// Convenience method to register methods on a class without holding
// onto the class object.
void registerNatives(const char* name, std::initializer_list<NativeMethod> methods);

/// Wrapper to provide functionality to jstring references
class FBEXPORT JString : public JavaClass<JString, JObject, jstring> {
 public:
  /// Java type descriptor
  static constexpr const char* kJavaDescriptor = "Ljava/lang/String;";

  /// Convenience method to convert a jstring object to a std::string
  std::string toStdString() const;
};

/// Convenience functions to convert a std::string or const char* into a @ref local_ref to a
/// jstring
FBEXPORT local_ref<JString> make_jstring(const char* modifiedUtf8);
FBEXPORT local_ref<JString> make_jstring(const std::string& modifiedUtf8);

/// Wrapper to provide functionality to jthrowable references
class FBEXPORT JThrowable : public JavaClass<JThrowable, JObject, jthrowable> {
 public:
  static constexpr const char* kJavaDescriptor = "Ljava/lang/Throwable;";
