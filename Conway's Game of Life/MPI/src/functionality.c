#include "functionality.h"

int parse_command_line(int argc, char **argv, int ***grid, 
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
           
            allocate_contiguous_grid(grid, *grid_size);
            initialize_grids(*grid, *grid_size, input);
            fclose(input);
            *readfile = true;
        }
    } else if (strcmp(argv[1], "-random") == 0) {
        /* Create a random grid. */
        *grid_size = atoi(argv[2]);
        srand(time(NULL));
    }

    *repetitions = atoi(argv[4]);

    if (strncmp(argv[6], "yes" , 3) == 0) {
        *check_termination = true;
    } else {
        *check_termination = false;
    }

    if (strncmp(argv[argc-1], "-print", 6) == 0) {
        if (!*readfile) {
            allocate_contiguous_grid(grid, *grid_size);
        }
        *print = true;
    } else {
        *print = false;
    }
    return 0;
}

void initialize_grid_random(int **grid, int grid_size) {

    /* Create randomly the initial state */
    for (int i = 1; i < grid_size + 1; i++) {
    for (int j = 1; j < grid_size + 1; j++) {
        grid[i][j] = rand() % 2;
    }
    }
}

void initialize_grids(int **grid, int grid_size, FILE *input) {

    for (int i = 0; i < grid_size; i++) {
    for (int j = 0; j < grid_size; j++) {
        grid[i][j] = fgetc(input) - '0';
        fgetc(input);
    }
        fgetc(input);
    }
}

void print_grid(int **grid, int size) {

    printf("\033[2J");
    printf("\033[H"); 
    for (int i = 0; i < size;    i++) {
    for (int j = 0; j < size; j++) {
        
        if (grid[i][j] == ALIVE) {
            printf("* ");
        } else {
            printf("  ");
        } 
    }
        printf("\033[E");
    }
    fflush(stdout);
}

bool equal_grids(int **grid1, int **grid2, int size) {

    for (int row = 1; row < (size - 1); row++) {
    for (int col = 1; col < (size - 1); col++) {
        if (grid1[row][col] != grid2[row][col]) {
            return false;
        }
    }
    }
    return true;
}

bool empty_grid(int **grid, int size) {

    for (int row = 1; row < (size - 1); row++) {
    for (int col = 1; col < (size - 1); col++) {
        if (grid[row][col] != DEAD) {
            return false;
        }
    }
    }
    return true;
}

/*
 * Allocate a 2-dimensional grid in contiguous memory.
 */
void allocate_contiguous_grid(int ***grid, int grid_size) {

    int *p = malloc(grid_size * grid_size * sizeof(int));
    if (!p) {
        perror("Could not allocate grid.\n");
        exit(EXIT_FAILURE);
    }

    (*grid) = malloc(grid_size * sizeof(int*));
    if (!(*grid)) {
       free(p);
       perror("Could not allocate grid.\n");
       exit(EXIT_FAILURE);
    }

    for (int i = 0; i < grid_size; i++)
       (*grid)[i] = &(p[ i * grid_size]);

    for (int i = 0; i < grid_size; i++) {
    for (int j = 0; j < grid_size; j++) {
        (*grid)[i][j] = 0;
    }
    }
}

void free_contiguous_grid(int ***grid) {

    free(&((*grid)[0][0]));
    free(*grid);
}

/*
 * Count the alive cells around a cell.
 */
void count_alive(int ***grid, int **new_grid, int size, int cells_group) {

    if (cells_group == INNER) {

        for (int row = 2; row < (size - 2); row++) {
        for (int col = 2; col < (size - 2); col++) {
            int sum = (*grid)[row-1][col-1] + (*grid)[row-1][col]   + (*grid)[row-1][col+1] +
                      (*grid)[row][col+1]   + (*grid)[row+1][col+1] + (*grid)[row+1][col]   +
                      (*grid)[row+1][col-1] + (*grid)[row][col-1];
            if (sum <= 1) {
                new_grid[row][col] = DEAD;
            } else if ((sum == 2 || sum == 3) && ((*grid)[row][col] == ALIVE)) {
                new_grid[row][col] = ALIVE;
            } else if ((sum >=4) && (sum <= 8)) {
                new_grid[row][col] = DEAD;
            } else if ((sum == 3) && ((*grid)[row][col] == DEAD)) {
                new_grid[row][col] = ALIVE;
            }   
        }
        }
    } else if (cells_group == OUTTER) {
        int row = 1;
        for (int col = 1; col < (size - 1); col++) {
            int sum = (*grid)[row-1][col-1] + (*grid)[row-1][col]   + (*grid)[row-1][col+1] +
                      (*grid)[row][col+1]   + (*grid)[row+1][col+1] + (*grid)[row+1][col]   +
                      (*grid)[row+1][col-1] + (*grid)[row][col-1];
            if (sum <= 1) {
                new_grid[row][col] = DEAD;
            } else if ((sum == 2 || sum == 3) && ((*grid)[row][col] == ALIVE)) {
                new_grid[row][col] = ALIVE;
            } else if ((sum >=4) && (sum <= 8)) {
                new_grid[row][col] = DEAD;
            } else if ((sum == 3) && ((*grid)[row][col] == DEAD)) {
                new_grid[row][col] = ALIVE;
            } 
        }

        row = size - 2;
        for (int col = 1; col < (size - 1); col++) {
            int sum = (*grid)[row-1][col-1] + (*grid)[row-1][col]   + (*grid)[row-1][col+1] +
                      (*grid)[row][col+1]   + (*grid)[row+1][col+1] + (*grid)[row+1][col]   +
                      (*grid)[row+1][col-1] + (*grid)[row][col-1];
            if (sum <= 1) {
                new_grid[row][col] = DEAD;
            } else if ((sum == 2 || sum == 3) && ((*grid)[row][col] == ALIVE)) {
                new_grid[row][col] = ALIVE;
            } else if ((sum >=4) && (sum <= 8)) {
                new_grid[row][col] = DEAD;
            } else if ((sum == 3) && ((*grid)[row][col] == DEAD)) {
                new_grid[row][col] = ALIVE;
            } 
        }

        int col = 1;
        for (int row = 1; row < (size - 1); row++) {
            int sum = (*grid)[row-1][col-1] + (*grid)[row-1][col]   + (*grid)[row-1][col+1] +
                      (*grid)[row][col+1]   + (*grid)[row+1][col+1] + (*grid)[row+1][col]   +
                      (*grid)[row+1][col-1] + (*grid)[row][col-1];
            if (sum <= 1) {
                new_grid[row][col] = DEAD;
            } else if ((sum == 2 || sum == 3) && ((*grid)[row][col] == ALIVE)) {
                new_grid[row][col] = ALIVE;
            } else if ((sum >=4) && (sum <= 8)) {
                new_grid[row][col] = DEAD;
            } else if ((sum == 3) && ((*grid)[row][col] == DEAD)) {
                new_grid[row][col] = ALIVE;
            } 
        }

        col = size - 2;
        for (int row = 1; row < (size - 1); row++) {
            int sum = (*grid)[row-1][col-1] + (*grid)[row-1][col]   + (*grid)[row-1][col+1] +
                      (*grid)[row][col+1]   + (*grid)[row+1][col+1] + (*grid)[row+1][col]   +
                      (*grid)[row+1][col-1] + (*grid)[row][col-1];
            if (sum <= 1) {
                new_grid[row][col] = DEAD;
            } else if ((sum == 2 || sum == 3) && ((*grid)[row][col] == ALIVE)) {
                new_grid[row][col] = ALIVE;
            } else if ((sum >=4) && (sum <= 8)) {
                new_grid[row][col] = DEAD;
            } else if ((sum == 3) && ((*grid)[row][col] == DEAD)) {
                new_grid[row][col] = ALIVE;
            } 
        }
    } else if (cells_group == ALL) {
        for (int row = 1; row < (size - 1); row++) {
        for (int col = 1; col < (size - 1); col++) {
            int sum = (*grid)[row-1][col-1] + (*grid)[row-1][col]   + (*grid)[row-1][col+1] +
                      (*grid)[row][col+1]   + (*grid)[row+1][col+1] + (*grid)[row+1][col]   +
                      (*grid)[row+1][col-1] + (*grid)[row][col-1];
            if (sum <= 1) {
                new_grid[row][col] = DEAD;
            } else if ((sum == 2 || sum == 3) && ((*grid)[row][col] == ALIVE)) {
                new_grid[row][col] = ALIVE;
            } else if ((sum >=4) && (sum <= 8)) {
                new_grid[row][col] = DEAD;
            } else if ((sum == 3) && ((*grid)[row][col] == DEAD)) {
                new_grid[row][col] = ALIVE;
            }   
        }
        }
    }
}

/*
 * Calculate the subarrays to scatter to processes. 
 */
void calculate_subarrays(MPI_Datatype *subarrtype, MPI_Datatype *local_grid1_type, 
                        int subgrid_size, int grid_size, int blocks_grid_size, 
                        int *sendcounts, int *displs, int world_rank) {

    int sizes[2]         = {grid_size, grid_size};
    int subgrid_sizes[2] = {subgrid_size, subgrid_size};
    int starts[2]        = {0, 0};
    int dimensions_local_grid_with_halo[2] = {subgrid_size + 2, subgrid_size + 2};
    int subdimensions_local_grid[2]        = {subgrid_size, subgrid_size};
    int starts_local_grid[2]               = {1, 1};

    MPI_Datatype type;
    MPI_Type_create_subarray(2, sizes, subgrid_sizes, starts, MPI_ORDER_C, MPI_INT, &type);
    MPI_Type_create_resized(type, 0,   subgrid_size * sizeof(int), subarrtype);
    MPI_Type_commit(subarrtype);

    MPI_Type_create_subarray(2, dimensions_local_grid_with_halo, subdimensions_local_grid, starts_local_grid, 
                              MPI_ORDER_C, 
                              MPI_INT, local_grid1_type);
    MPI_Type_commit(local_grid1_type);

    if (world_rank == 0) {
        for (int i = 0; i < blocks_grid_size * blocks_grid_size; i++) {
            sendcounts[i] = 1;
        }
        int disp = 0;
        for (int i = 0; i < blocks_grid_size; i++) {
        for (int j = 0; j < blocks_grid_size; j++) {
            displs[i * blocks_grid_size + j] = disp;
            disp += 1;
        }
            disp += ((subgrid_size) - 1) * blocks_grid_size;
        }
    }
}

/*
 * Calculation of neighbours               
 */
void calculate_neighbours(int *neighbours, MPI_Comm comm_2D, int *my_coords, int *dims) {

    int direct = 0;
    int displ  = 1;

    MPI_Cart_shift(comm_2D, direct, displ, &neighbours[NORTH], &neighbours[SOUTH]);

    direct = 1;

    MPI_Cart_shift(comm_2D, direct, displ, &neighbours[WEST], &neighbours[EAST]);

    int coords_corner[2];

    /* north west */
    if (my_coords[0] == 0) {
        coords_corner[0] = dims[0] - 1;
    } else {
        coords_corner[0] = my_coords[0] - 1;
    }

    if (my_coords[1] == 0 ) {
        coords_corner[1] = dims[1] - 1;  
    } else {
        coords_corner[1] = my_coords[1] - 1;
    }

    MPI_Cart_rank(comm_2D, coords_corner, &neighbours[NORTH_WEST]);

    /* north east */
    if (my_coords[0] == 0) {
        coords_corner[0] = dims[0] - 1;
    } else {
        coords_corner[0] = my_coords[0] - 1;
    }

    if (my_coords[1] == dims[1] - 1) {
        coords_corner[1] = 0;  
    } else {
        coords_corner[1] = my_coords[1] + 1;
    }

    MPI_Cart_rank(comm_2D, coords_corner, &neighbours[NORTH_EAST]);

     /* south east */
    if (my_coords[0] == dims[0] - 1) {
        coords_corner[0] = 0;
    } else {
        coords_corner[0] = my_coords[0] + 1;
    }

    if (my_coords[1] == dims[1] - 1) {
        coords_corner[1] = 0;  
    } else {
        coords_corner[1] = my_coords[1] + 1;
    }

    MPI_Cart_rank(comm_2D, coords_corner, &neighbours[SOUTH_EAST]);

    /* south west */
    if (my_coords[0] == dims[0] - 1) {
        coords_corner[0] = 0;
    } else {
        coords_corner[0] = my_coords[0] + 1;
    }

    if (my_coords[1] == 0) {
        coords_corner[1] = dims[1] - 1;  
    } else {
        coords_corner[1] = my_coords[1] - 1;
    }

    MPI_Cart_rank(comm_2D, coords_corner, &neighbours[SOUTH_WEST]);
}

/*
 * Precalculate the sends and the receives
 */
void precalculate_sends_recvs(MPI_Request *requests_grid_send, 
                              MPI_Request *requests_grid_receive, 
                              int **local_grid,  int *neighbours, 
                              int subgrid_size, MPI_Comm comm_2D) {

    int dimensions[2]           = {subgrid_size + 2, subgrid_size + 2};
    int subdimensions_row[2]    = {1, subgrid_size};
    int subdimensions_column[2] = {subgrid_size, 1};
    int subdimensions_corner[2] = {1, 1};

    int starts_north_row_send[2] = {1, 1};
    int starts_south_row_send[2] = {subgrid_size, 1};
    int starts_north_row_recv[2] = {0, 1};
    int starts_south_row_recv[2] = {subgrid_size + 1, 1};

    int starts_east_send[2] = {1, subgrid_size};
    int starts_west_send[2] = {1, 1};
    int starts_west_recv[2] = {1, 0};
    int starts_east_recv[2] = {1, subgrid_size + 1};

    int starts_ne[2] = {1, subgrid_size};
    int starts_se[2] = {subgrid_size, subgrid_size};
    int starts_sw[2] = {subgrid_size, 1};
    int starts_nw[2] = {1, 1};

    MPI_Type_create_subarray(2, dimensions, subdimensions_row, starts_north_row_send, MPI_ORDER_C, MPI_INT, &type_north_row_send);
    MPI_Type_create_subarray(2, dimensions, subdimensions_row, starts_south_row_send, MPI_ORDER_C, MPI_INT, &type_south_row_send);
    MPI_Type_create_subarray(2, dimensions, subdimensions_row, starts_north_row_recv, MPI_ORDER_C, MPI_INT, &type_north_row_recv);
    MPI_Type_create_subarray(2, dimensions, subdimensions_row, starts_south_row_recv, MPI_ORDER_C, MPI_INT, &type_south_row_recv);

    MPI_Type_create_subarray(2, dimensions, subdimensions_column, starts_east_send, MPI_ORDER_C, MPI_INT, &type_east_column_send);
    MPI_Type_create_subarray(2, dimensions, subdimensions_column, starts_west_send, MPI_ORDER_C, MPI_INT, &type_west_column_send);
    MPI_Type_create_subarray(2, dimensions, subdimensions_column, starts_west_recv, MPI_ORDER_C, MPI_INT, &type_west_column_recv);
    MPI_Type_create_subarray(2, dimensions, subdimensions_column, starts_east_recv, MPI_ORDER_C, MPI_INT, &type_east_column_recv);

    MPI_Type_create_subarray(2, dimensions, subdimensions_corner, starts_ne, MPI_ORDER_C, MPI_INT, &type_north_east);
    MPI_Type_create_subarray(2, dimensions, subdimensions_corner, starts_se, MPI_ORDER_C, MPI_INT, &type_south_east);
    MPI_Type_create_subarray(2, dimensions, subdimensions_corner, starts_sw, MPI_ORDER_C, MPI_INT, &type_south_west);
    MPI_Type_create_subarray(2, dimensions, subdimensions_corner, starts_nw, MPI_ORDER_C, MPI_INT, &type_north_west);

    MPI_Type_commit(&type_west_column_recv);
    MPI_Type_commit(&type_east_column_recv);
    MPI_Type_commit(&type_east_column_send);
    MPI_Type_commit(&type_west_column_send);

    MPI_Type_commit(&type_north_west);
    MPI_Type_commit(&type_south_east);
    MPI_Type_commit(&type_south_west);
    MPI_Type_commit(&type_north_east);
   
    MPI_Type_commit(&type_north_row_send);
    MPI_Type_commit(&type_south_row_send);
    MPI_Type_commit(&type_south_row_recv);
    MPI_Type_commit(&type_north_row_recv);

    /* Send rows */

    MPI_Send_init(&local_grid[0][0], 1, type_north_row_send, neighbours[NORTH], 0, comm_2D, &requests_grid_send[0]);
    MPI_Send_init(&local_grid[0][0], 1, type_south_row_send, neighbours[SOUTH], 0, comm_2D, &requests_grid_send[1]);

    /* Send columns */
        
    MPI_Send_init(&local_grid[0][0], 1, type_east_column_send, neighbours[EAST], 0, comm_2D, &requests_grid_send[2]);
    MPI_Send_init(&local_grid[0][0], 1, type_west_column_send, neighbours[WEST], 0, comm_2D, &requests_grid_send[3]);

    /* Send corners */
  
    MPI_Send_init(&local_grid[0][0], 1, type_north_east, neighbours[NORTH_EAST], 0, comm_2D, &requests_grid_send[4]);
    MPI_Send_init(&local_grid[0][0], 1, type_south_east, neighbours[SOUTH_EAST], 0, comm_2D, &requests_grid_send[5]);
    MPI_Send_init(&local_grid[0][0], 1, type_south_west, neighbours[SOUTH_WEST], 0, comm_2D, &requests_grid_send[6]);
    MPI_Send_init(&local_grid[0][0], 1, type_north_west, neighbours[NORTH_WEST], 0, comm_2D, &requests_grid_send[7]);

    /* Receive rows */

    MPI_Recv_init(&local_grid[0][0], 1, type_south_row_recv, neighbours[SOUTH], 0, comm_2D, &requests_grid_receive[0]);
    MPI_Recv_init(&local_grid[0][0], 1, type_north_row_recv, neighbours[NORTH], 0, comm_2D, &requests_grid_receive[1]);
 
    /* Receive columns */
                
    MPI_Recv_init(&local_grid[0][0], 1, type_west_column_recv, neighbours[WEST], 0, comm_2D, &requests_grid_receive[2]);
    MPI_Recv_init(&local_grid[0][0], 1, type_east_column_recv, neighbours[EAST], 0, comm_2D, &requests_grid_receive[3]);
       
    /* Receive corners */

    MPI_Recv_init(&local_grid[subgrid_size + 1][0],                1, MPI_INT, neighbours[SOUTH_WEST], 0, comm_2D, &requests_grid_receive[4]);
    MPI_Recv_init(&local_grid[0][0],                               1, MPI_INT, neighbours[NORTH_WEST], 0, comm_2D, &requests_grid_receive[5]);
    MPI_Recv_init(&local_grid[0][subgrid_size + 1],                1, MPI_INT, neighbours[NORTH_EAST], 0, comm_2D, &requests_grid_receive[6]);
    MPI_Recv_init(&local_grid[subgrid_size + 1][subgrid_size + 1], 1, MPI_INT, neighbours[SOUTH_EAST], 0, comm_2D, &requests_grid_receive[7]);
}