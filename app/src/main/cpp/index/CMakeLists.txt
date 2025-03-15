project("index")

add_library(faiss SHARED IMPORTED)
set_target_properties(faiss PROPERTIES IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/faiss/lib/libfaiss.so)

add_library(omp SHARED IMPORTED)
set_target_properties(omp PROPERTIES IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/libomp.so)

add_library(${PROJECT_NAME} SHARED
    # List C/C++ source files with relative paths to this CMakeLists.txt.
    index.cpp
)

target_include_directories(${PROJECT_NAME} PRIVATE
    # List include directories for the target library
    ${CMAKE_SOURCE_DIR}/faiss/include)

target_include_directories(${PROJECT_NAME} PRIVATE
    # List include directories for the target library
    ${CMAKE_SOURCE_DIR})

target_link_libraries(${PROJECT_NAME}
    # List libraries link to the target library
    android
    log
    faiss
    omp
)