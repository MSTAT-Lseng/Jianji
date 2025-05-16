cmake_minimum_required(VERSION 4.0.2) # 使用你安装或指定的CMake版本

project("jjnative") # 库名

add_library( # 定义你的原生库
        jjnative
        SHARED # or STATIC
        backup-lib.cpp
)

find_library( # options
        log-lib
        log
)

target_link_libraries( # 链接库
        jjnative
        ${log-lib} # options
)