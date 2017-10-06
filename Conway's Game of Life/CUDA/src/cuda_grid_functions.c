#include "cuda_grid_functions.h"

int parse_command_line(int argc, char **argv, int **grid, int **result_grid, int **previous_grid,
                       int *repetitions,
                       int *grid_size,
                       bool *check_termination, 
                       bool *print,
                       bool *readfile) {
    FILE *input;
   
    if (argc == 1) {
            printf("Usage:\n <executable> -readfile <input file> -steps <number of steps> -check_termination <yes or no> <-print> : for reading input from file \n OR \n <executable> -random <grid_size> -steps <number of steps> -check_termination <yes or no>  <-print> \t: for producing random input\n");
            return -1;
    }
    
    /* Read from file. */
    if (strcmp(argv[1], "-readfile") == 0) {
        input = fopen(argv[2], "r");
        if (input == NULL) {
            perror("Error while opening the file.\n");
            return -1;
        } else {
            char size_str[11];
          
            if (fgets(size_str, 11, input) == NULL) {
                perror("Could not read size from file.\n");
                return -1; 
            }
          
            *grid_size = atoi(size_str);
           
            initialize_grids(grid, result_grid, previous_grid, *grid_size, input);
            fclose(input);
            *readfile = true;
        }
    } else if (strcmp(argv[1], "-random") == 0) {
        /* Create a random grid. */
        *grid_size = atoi(argv[2]);
        srand(time(NULL));
        initialize_grids(grid, result_grid, previous_grid, *grid_size, NULL);
    }

    *repetitions = atoi(argv[4]);

    if (strncmp(argv[6], "yes" , 3) == 0) {
        *check_termination = true;
    } else {
        *check_termination = false;
    }

    if (strncmp(argv[argc-1], "-print", 6) == 0) {
        *print = true;
    } else {
        *print = false;
    }
    return 0;
}

/* Allocate and initialize the grids. */
void initialize_grids(int **grid, int **resultGrid, int **previousGrid, int N, FILE *input) {

    *grid         = (int *)malloc((N+2) * (N+2) * sizeof(int));
    *previousGrid = (int *)malloc((N+2) * (N+2) * sizeof(int));
    *resultGrid   = (int *)malloc((N+2) * (N+2) * sizeof(int));
    memset(*resultGrid,   0,      (N+2) * (N+2) * sizeof(int));
    memset(*previousGrid, 0,      (N+2) * (N+2) * sizeof(int));

    /* Initialize wrapper grid with 0's
      i = 0;
      i = N + 2 
    */
    for(int j = 0; j < (N + 2); j++) {
        (*grid)[j]               = 0;
        (*grid)[(N + 1)*(N + 2) + j] = 0;
    }

    /* j = 0;
       j = N + 2 
    */
    for(int i = 1; i < (N + 1); i++) {
        (*grid)[i * (N + 2)]           = 0;
        (*grid)[i * (N + 2) + (N + 1)] = 0;
    }
    
    for(int i = 1; i < (N + 1); i++) {
    for(int j = 1; j < (N + 1); j++) {
        if (input == NULL) {
            (*grid)[i* (N + 2)+j]            = rand() % 2;
            (*previousGrid)[i * (N + 2) + j] = (*grid)[i * (N+2) + j];
        } else {
            (*grid)[i* (N + 2)+j]            = fgetc(input) - '0';
            (*previousGrid)[i * (N + 2) + j] = (*grid)[i * (N + 2) + j];
            fgetc(input);
        }
    }
        if (input != NULL) {
            fgetc(input);
        }
    }
}

/* Τhis function copies the columns, rows, and corners
   in the wrapper cells of the grid.
 */
void copy_wrapper(int *grid, int N) {

    /* Copy the top and bottom row
       to the surrounding cells. 
     */
    for(int j = 1; j < (N + 1); j++) {
        grid[j]               = grid[N*(N+2) + j];
        grid[(N+1)*(N+2) + j] = grid[N+2 + j];
    }

    /* Copy the left and right column
       to the surrounding cells. 
     */
    for(int i = 1; i < (N + 1); i++) {
        grid[i * (N + 2)]           = grid[i*(N+2) + N];
        grid[i * (N + 2) + (N + 1)] = grid[i*(N+2)+1];
    }

    /* Copy the corners. */
    grid[0]                 = grid[N*(N+2) + N];
    grid[N+1]               = grid[(N+1)*(N+1)];
    grid[(N+2)*(N+1)]       = grid[(N+2) + N];
    grid[(N+1)*(N+2) + N+1] = grid[(N+2)+1];
}

/* Check if two grids are equal. */
bool equalGrids(int *grid1, int *grid2, int N) {
    
    for (int i = 1; i < (N + 1); i++) {
    for (int j = 1; j < (N + 1); j++) {
        if (grid1[i * (N + 2) + j] != 
            grid2[i * (N + 2) + j]) {
            return false;
        }
    }
    }
    return true;
}

/* Check if the grid has no more alive cells. */
bool empty_grid(int *grid, int N) {

    for (int i = 1; i < (N + 1); i++) {
    for (int j = 1; j < (N + 1); j++) {
        if (grid[i * (N + 2) + j] != DEAD) {
            return false;
        }
    }
    }
    return true;
}

void print_grid(int *grid, int N) {

    printf("\033[2J");
    printf("\033[H"); 

    for (int i = 1; i < (N+1); i++) {
    for (int j = 1; j < (N+1); j++) {

        if (grid[i* (N+2)+j] == ALIVE) {
            printf("* ");
        } else {
            printf("  ");
        }
    }
        printf("\033[E");
    }
    fflush(stdout);
}