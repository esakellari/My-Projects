CC = mpicc
CFLAGS = -O2 -g -Wall -std=c99 -std=gnu99
LFLAGS = -lm
PROG = bin/gol_mpi
OBJS = obj/gol_mpi_main.o obj/functionality.o

all : $(OBJS)
	$(CC) $(OBJS) -o $(PROG) $(LFLAGS)

obj/functionality.o : src/functionality.c
	$(CC) $(CFLAGS) -o obj/functionality.o -c src/functionality.c

obj/gol_mpi_main.o : src/gol_mpi_main.c
	$(CC) $(CFLAGS) -o obj/gol_mpi_main.o -c src/gol_mpi_main.c

clean :
	rm -f $(OBJS) $(PROG)
cleano :
	rm -f $(OBJS)