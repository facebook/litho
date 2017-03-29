// Copyright 2004-present Facebook. All Rights Reserved.

#include <fb/lyra.h>

#include <ios>
#include <memory>
#include <vector>

#include <dlfcn.h>
#include <unwind.h>

using namespace std;

namespace facebook {
namespace lyra {

namespace {

class IosFlagsSaver {
  ios_base& ios_;
  ios_base::fmtflags flags_;

 public:
  IosFlagsSaver(ios_base& ios)
  : ios_(ios),
    flags_(ios.flags())
  {}

  ~IosFlagsSaver() {
    ios_.flags(flags_);
  }
};

