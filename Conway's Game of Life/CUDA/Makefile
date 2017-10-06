CUDA_INSTALL_PATH = /usr
CC = g++
OPTFLAG = -O2 -fomit-frame-pointer -ftree-vectorize -ftree-vectorizer-verbose=0  -funroll-loops
NVCC = ${CUDA_INSTALL_PATH}/bin/nvcc
INCDIR = -I../common/inc/
FLAGS = ${OPTFLAG} -I${CUDA_INSTALL_PATH}/include -Wall -g ${INCDIR}
NVFLAGS = -O2 -D_FORCE_INLINES -I${CUDA_INSTALL_PATH}/include --compiler-options -fno-strict-aliasing --ptxas-options=-v -g -arch=sm_20 ${INCDIR}
BITS = $(shell getconf LONG_BIT)
ifeq (${BITS},64)
	LIBSUFFIX := 64
endif
LFLAGS = -L${CUDA_INSTALL_PATH}/lib${LIBSUFFIX} -lm -lstdc++ -lcudart
CLEAN_FILES = obj/golcuda.o obj/cuda_grid_functions.o

bin/golcuda: obj/cuda_grid_functions.o obj/golcuda.o 
	${NVCC} ${NVFLAGS} -o $@ $^
	cp $@ ../release

obj/cuda_grid_functions.o: src/cuda_grid_functions.c
	${CC} -c ${FLAGS} -o $@ $^

obj/golcuda.o: src/golcuda.cu
	${NVCC} ${NVFLAGS} -DUNIX -c $^ -o $@

clean:
	\rm -f $(CLEAN_FILES)
	\rm -f ../release/golcuda

rebuild: clean bin/golcuda
