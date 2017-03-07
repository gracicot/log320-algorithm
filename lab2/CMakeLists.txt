cmake_minimum_required(VERSION 2.6)
project(peg)

add_executable(peg peg.cpp)

target_include_directories(peg PUBLIC include/)
