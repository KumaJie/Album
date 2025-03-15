project("net")

cmake_minimum_required(VERSION 3.22.1)

set(ncnn_DIR ${CMAKE_SOURCE_DIR}/ncnn-20241226-android-vulkan/${ANDROID_ABI}/lib/cmake/ncnn)
find_package(ncnn REQUIRED)

add_library(${CMAKE_PROJECT_NAME} SHARED
    # List C/C++ source files with relative paths to this CMakeLists.txt.
        net.cpp
)

target_link_libraries(${CMAKE_PROJECT_NAME}
    # List libraries link to the target library
    android
    log
    ncnn
)

add_subdirectory(${CMAKE_SOURCE_DIR}/index)



