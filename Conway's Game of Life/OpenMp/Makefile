CC = mpicc
CFLAGS = -O2 -g -Wall -fopenmp -std=c99 -std=gnu99
LFLAGS = -lm -fopenmp
PROG = bin/gol_omp
OBJS = obj/gol_omp_main.o obj/grid_functions.o obj/mpi_helper_functions.o

all : $(OBJS)
	$(CC) $(OBJS) -o $(PROG) $(LFLAGS)

obj/grid_functions.o : src/grid_functions.c
	$(CC) $(CFLAGS) -o obj/grid_functions.o -c src/grid_functions.c

obj/gol_omp_main.o : src/gol_omp_main.c
	$(CC) $(CFLAGS) -o obj/gol_omp_main.o -c src/gol_omp_main.c

obj/mpi_helper_functions.o : src/mpi_helper_functions.c
	$(CC) $(CFLAGS) -o obj/mpi_helper_functions.o -c src/mpi_helper_functions.c
clean :
	rm -f $(OBJS) $(PROG)
cleano :
	rm -f $(OBJS)
