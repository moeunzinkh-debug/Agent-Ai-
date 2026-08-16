cmake_minimum_required(VERSION 3.22.1)
project(agentai_llama CXX C)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

include(FetchContent)

# Pinned to a known-good upstream tag so builds are reproducible.
# Fetched at configure time because this repository cannot add git submodules
# (the CI checkout step is not editable from here).
FetchContent_Declare(
    llama_cpp
    GIT_REPOSITORY https://github.com/ggml-org/llama.cpp.git
    GIT_TAG        b10453
    GIT_SHALLOW    TRUE
    GIT_PROGRESS   TRUE
)

# Inference-only build: skip everything that pulls in extra deps or slows the build.
set(LLAMA_BUILD_TESTS    OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_TOOLS    OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_SERVER   OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_COMMON   OFF CACHE BOOL "" FORCE)
set(LLAMA_CURL           OFF CACHE BOOL "" FORCE)
set(LLAMA_OPENSSL        OFF CACHE BOOL "" FORCE)

# Statically link ggml/llama into our single .so. Avoids dlopen-based backend
# discovery, which is fragile on older Android releases.
set(BUILD_SHARED_LIBS    OFF CACHE BOOL "" FORCE)
set(GGML_BACKEND_DL      OFF CACHE BOOL "" FORCE)
set(GGML_NATIVE          OFF CACHE BOOL "" FORCE)
set(GGML_OPENMP          OFF CACHE BOOL "" FORCE)
set(GGML_LLAMAFILE       ON  CACHE BOOL "" FORCE)

FetchContent_MakeAvailable(llama_cpp)

add_library(agentai_llama SHARED agentai_llama.cpp)

target_link_libraries(agentai_llama
    PRIVATE
        llama
        ggml
        android
        log
)
