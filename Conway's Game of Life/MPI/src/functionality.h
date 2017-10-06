#include <mpi.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <time.h>
#include <stdbool.h>
#include <sys/time.h>
#include <string.h>

#define ALIVE 1
#define DEAD  0

#define INNER  2
#define OUTTER 11
#define ALL    1

#define DIMENSIONS 2

#define NUMBER_OF_NEIGHBOURS 8
#define NORTH      0
#define NORTH_EAST 1
#define EAST       2
#define SOUTH_EAST 3
#define SOUTH      4
#define SOUTH_WEST 5
#define WEST       6
#define NORTH_WEST 7

MPI_Datatype type_north_east;
MPI_Datatype type_south_east;
MPI_Datatype type_south_west;
MPI_Datatype type_north_west;
MPI_Datatype type_west_column_send;
MPI_Datatype type_east_column_send;
MPI_Datatype type_west_column_recv;
MPI_Datatype type_east_column_recv;
MPI_Datatype type_north_row_send;
MPI_Datatype type_south_row_send;
MPI_Datatype type_north_row_recv;
MPI_Datatype type_south_row_recv;

void precalculate_sends_recvs(MPI_Request *requests_grid_send, 
                              MPI_Request *requests_grid_receive, 
                              int **local_grid,  int *neighbours, 
                              int subgrid_size, MPI_Comm comm_2D);

void calculate_subarrays(MPI_Datatype *subarrtype, MPI_Datatype *local_grid1_type, 
                        int subgrid_size, int grid_size, int blocks_grid_size, 
                        int *sendcounts, int *displs, int world_rank);

void calculate_neighbours(int *neighbours, MPI_Comm comm_2D, int *my_coords, int *dims);

int parse_command_line(int argc, char **argv, int ***grid, 
                       int *repetitions, 
                       int *grid_size,
                       bool *check_termination, 
                       bool *print,
                       bool *readfile);

void allocate_contiguous_grid (int ***grid, int grid_size);
void free_contiguous_grid     (int ***grid);
void initialize_grids         (int **grid, int grid_size, FILE *input);
void initialize_grid_random(int **grid, int grid_size);

bool equal_grids(int **grid1, int **grid2, int size);
bool empty_grid (int **grid,  int size);

void count_alive(int ***grid, int **new_grid, int size, int cells_group);
void print_grid (int **grid, int size);