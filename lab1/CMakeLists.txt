cmake_minimum_required(VERSION 3.0)

project(lab1)

if (CMAKE_CXX_COMPILER_ID MATCHES "Clang")
	set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -O3 -Werror  -fno-exceptions -fno-rtti -std=c++14 -Wno-undefined-inline")
elseif (GNU)
	set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -O3 -Werror=suggest-override -fno-rtti  -fno-exceptions -std=c++14 -Werror -Wall -Wno-reorder -Wno-unused-parameter -Wextra -std=c++11 -Werror=pedantic")
elseif (Intel)
  # using Intel C++
elseif (MSVC)
  # using Visual Studio C++
endif()

add_executable(algo1 algo1.cpp)
add_executable(algo2 algo2.cpp)

target_include_directories(algo1 PUBLIC include/)
target_include_directories(algo2 PUBLIC include/)
