cmake_minimum_required(VERSION 3.0)

# 设置工程名字和版本号
project(BlockSocket VERSION 1.0)

set(CMAKE_BUILD_TYPE Debug)
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -g")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wall")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -pedantic")
set(CMAKE_CXX_FLAGS_DEBUG "${CMAKE_CXX_FLAGS_DEBUG} -g")

# 添加可执行文件a所需要的源代码
add_executable(client src/client.cc)

# 添加可执行文件c所需要的源代码
add_executable(server src/server.cc)

# 设置生成可执行文件的目录
set(EXECUTABLE_OUTPUT_PATH ${PROJECT_SOURCE_DIR}/build)

# 添加头文件搜索路径
include_directories(${PROJECT_SOURCE_DIR}/include)

# 添加链接库搜索路径
link_directories(${PROJECT_SOURCE_DIR}/lib)