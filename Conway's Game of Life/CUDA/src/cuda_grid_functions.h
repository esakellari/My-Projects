#include <stdio.h>
#include <stdlib.h>
#include <cuda.h>
#include <cuda_runtime_api.h>
#include <time.h>
#include <stdbool.h>
#include <string.h>
#include <sys/time.h>

#define ALIVE 1
#define DEAD 0

#define THREADS_PER_BLOCK 32
#define WRAPPER_SIZE (grid_size + 2) * (grid_size + 2)

void initialize_grids(int **grid, int **resultGrid, int **previousGrid, int N, FILE *input);
bool equalGrids(int *grid1, int *grid2, int N);
bool empty_grid(int *grid, int N);
void print_grid(int *grid, int N);
void copy_wrapper(int *grid, int N);

int parse_command_line(int argc, char **argv, int **grid, int **result_grid, int **previous_grid,
                       int *repetitions,
                       int *grid_size,
                       bool *check_termination, 
                       bool *print,
                       bool *readfile);