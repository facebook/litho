/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho.config

import android.os.Build
import java.io.BufferedReader
import java.io.File
import java.io.FileFilter
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader

object DeviceInfoUtils {

  /**
   * The default return value of any method in this class when an error occurs or when processing
   * fails (Currently set to -1). Use this to check if the information about the device in question
   * was successfully obtained.
   */
  const val DEVICEINFO_UNKNOWN: Int = -1
  const val NUM_CORES_NOT_SET: Int = -2

  private var numCores = NUM_CORES_NOT_SET

  @get:JvmStatic
  val numberOfCPUCores: Int
    /**
     * Reads the number of CPU cores from the first available information from
     * `/sys/devices/system/cpu/possible`, `/sys/devices/system/cpu/present`, then
     * `/sys/devices/system/cpu/`.
     *
     * @return Number of CPU cores in the phone, or DEVICEINFO_UKNOWN = -1 in the event of an error.
     */
    get() {
      if (numCores != NUM_CORES_NOT_SET) {
        return numCores
      }
      if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.GINGERBREAD_MR1) {
        // Gingerbread doesn't support giving a single application access to both cores, but a
        // handful of devices (Atrix 4G and Droid X2 for example) were released with a dual-core
        // chipset and Gingerbread; that can let an app in the background run without impacting
        // the foreground application. But for our purposes, it makes them single core.
        return 1
      }
      var cores: Int
      try {
        cores = getCoresFromFileInfo("/sys/devices/system/cpu/possible")
        if (cores == DEVICEINFO_UNKNOWN) {
          cores = getCoresFromFileInfo("/sys/devices/system/cpu/present")
        }
        if (cores == DEVICEINFO_UNKNOWN) {
          cores = coresFromCPUFileList
        }
      } catch (e: SecurityException) {
        cores = DEVICEINFO_UNKNOWN
      } catch (e: NullPointerException) {
        cores = DEVICEINFO_UNKNOWN
      }
      numCores = cores
      return cores
    }

  @JvmStatic
  fun hasMultipleCores(): Boolean {
    val numCores = numberOfCPUCores
    return numCores > 1
  }

  /**
   * Tries to read file contents from the file location to determine the number of cores on device.
   *
   * @param fileLocation The location of the file with CPU information
   * @return Number of CPU cores in the phone, or DEVICEINFO_UKNOWN = -1 in the event of an error.
   */
  private fun getCoresFromFileInfo(fileLocation: String): Int {
    var inputStream: InputStream? = null
    return try {
      inputStream = FileInputStream(fileLocation)
      val buf = BufferedReader(InputStreamReader(inputStream))
      val fileContents = buf.readLine()
      buf.close()
      getCoresFromFileString(fileContents)
    } catch (e: IOException) {
      DEVICEINFO_UNKNOWN
    } finally {
      if (inputStream != null) {
        try {
          inputStream.close()
        } catch (e: IOException) {
          // Do nothing.
        }
      }
    }
  }

  /**
   * Converts from a CPU core information format to number of cores.
   *
   * @param str The CPU core information string, in the format of "0-N"
   * @return The number of cores represented by this string
   */
  @JvmStatic
  fun getCoresFromFileString(str: String?): Int =
      if (str == null || !str.matches("0-[\\d]+$".toRegex())) {
        DEVICEINFO_UNKNOWN
      } else {
        str.substring(2).toInt() + 1
      }

  private val coresFromCPUFileList: Int
    get() {
      val cpuFiles = File("/sys/devices/system/cpu/").listFiles(CPU_FILTER)
      return checkNotNull(cpuFiles).size
    }

  private val CPU_FILTER = FileFilter { pathname ->
    val path = pathname.name
    // regex is slow, so checking char by char.
    if (path.startsWith("cpu")) {
      return@FileFilter (3 until path.length).none { i -> !Character.isDigit(path[i]) }
    }
    false
  }
}
