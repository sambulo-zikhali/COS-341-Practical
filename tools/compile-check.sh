#!/bin/bash
# usage: tools/compile-check.sh output.cpp expected-output-file
# Compiles with g++ (c++11, gnu++17 -Wall -Wextra) and gcc -lstdc++, runs, compares.
f=$1; exp=$2; ok=1
g++ -std=c++11 "$f" -o /tmp/cc_a || ok=0
g++ -std=gnu++17 -Wall -Wextra "$f" -o /tmp/cc_b || ok=0
gcc "$f" -o /tmp/cc_c -lstdc++ || ok=0
gcc -x c++ "$f" -o /tmp/cc_d -lstdc++ || ok=0
[ $ok = 1 ] || { echo "COMPILE FAIL $f"; exit 1; }
[ "$(/tmp/cc_a)" = "$(cat "$exp")" ] && echo "PASS $f" || { echo "OUTPUT MISMATCH $f"; exit 1; }
