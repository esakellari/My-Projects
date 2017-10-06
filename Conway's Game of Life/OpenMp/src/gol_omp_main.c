#include "functionality.h"

int main(int argc, char** argv) {

    int **grid      = NULL;
    int repetitions = 0;
    int grid_size   = 0;
    int threads_num = 0;
    bool print             = false;
    bool check_termination = false;
    bool readfile = false;
        
    MPI_Init(NULL, NULL);

    int world_size;
    int world_rank;

    MPI_Comm_size(MPI_COMM_WORLD, &world_size);
    MPI_Comm_rank(MPI_COMM_WORLD, &world_rank);

    int error;
    if (world_rank == 0) {
        error = parse_command_line(argc, argv, &grid, 
                                               &repetitions,
                                               &threads_num,
                                               &grid_size, 
                                               &check_termination, 
                                               &print, 
                                               &readfile);
    }
  
    /* Inform the other processes to terminate in case of error. */
    MPI_Bcast(&error, 1, MPI_INT, 0, MPI_COMM_WORLD);

    if (error < 0) {
        return 0;
    }

    int buf[6] = {grid_size, repetitions, threads_num, check_termination, print, readfile};
    MPI_Bcast(buf, 6, MPI_INT, 0, MPI_COMM_WORLD);
    grid_size         = buf[0];
    repetitions       = buf[1];
    threads_num       = buf[2];
    check_termination = buf[3];
    print             = buf[4];
    readfile          = buf[5];

    int dims[DIMENSIONS]    = {0, 0};
    int periods[DIMENSIONS] = {true, true};
    int my_coords[DIMENSIONS];
    MPI_Comm comm_2D;

    MPI_Dims_create(world_size,     DIMENSIONS, dims);
    MPI_Cart_create(MPI_COMM_WORLD, DIMENSIONS, dims, periods, true, &comm_2D);
    MPI_Comm_rank(comm_2D, &world_rank);
    MPI_Cart_coords(comm_2D, world_rank, DIMENSIONS, my_coords);

    char processor_name[MPI_MAX_PROCESSOR_NAME];
    int name_len;
    MPI_Get_processor_name(processor_name, &name_len);

    printf("Proccess %d runs on machine %s\n", world_rank, processor_name);

    int *neighbours = malloc(NUMBER_OF_NEIGHBOURS * sizeof(int));

    int blocks_grid_size = dims[0];

    /*   Calculation of neighbours    */
    calculate_neighbours(neighbours, comm_2D, my_coords, dims);

    int subgrid_size = grid_size / blocks_grid_size;

    int **local_grid1 = NULL;
    int **local_grid2 = NULL;
    allocate_contiguous_grid(&local_grid1, subgrid_size + 2);
    allocate_contiguous_grid(&local_grid2, subgrid_size + 2);

    MPI_Datatype subarray_type;
    MPI_Datatype local_grid1_type;

    int *sendcounts = NULL; 
    int *displs     = NULL; 

    if (readfile || print) {
        sendcounts = malloc(blocks_grid_size * blocks_grid_size * sizeof(int));
        displs     = malloc(blocks_grid_size * blocks_grid_size * sizeof(int));
       
        calculate_subarrays(&subarray_type, &local_grid1_type, 
                        subgrid_size, grid_size, blocks_grid_size, 
                        sendcounts, displs, world_rank);
        if (world_rank != 0) {
            allocate_contiguous_grid(&grid, grid_size);
        }
    } 

    if (readfile) {
        MPI_Scatterv(&grid[0][0], sendcounts, displs, subarray_type, &local_grid1[0][0],
                 1, local_grid1_type,
                 0, comm_2D);
    } else {
        /* Each process randomly initializes its subarray. */
        initialize_grid_random(local_grid1, subgrid_size);
    }

    MPI_Request *requests_grid1_send;
    MPI_Request *requests_grid1_receive;
    MPI_Request *requests_grid2_send;
    MPI_Request *requests_grid2_receive;
    int rep;
    int check_rep = 1;

    /* Calculate the datatypes of rows and columns I will 
       send and receive with the other processes.       
     */
    requests_grid1_send    = malloc (NUMBER_OF_NEIGHBOURS * sizeof(MPI_Request));
    requests_grid1_receive = malloc (NUMBER_OF_NEIGHBOURS * sizeof(MPI_Request));
    requests_grid2_send    = malloc (NUMBER_OF_NEIGHBOURS * sizeof(MPI_Request));
    requests_grid2_receive = malloc (NUMBER_OF_NEIGHBOURS * sizeof(MPI_Request));

    precalculate_sends_recvs(requests_grid1_send, requests_grid1_receive, local_grid1, neighbours, subgrid_size, comm_2D);
    precalculate_sends_recvs(requests_grid2_send, requests_grid2_receive, local_grid2, neighbours, subgrid_size, comm_2D);

    double starttime = 0;
    double endtime   = 0;

     /* Make sure all the processes start together. */
    MPI_Barrier(comm_2D);

    starttime = MPI_Wtime();

    /* The number of repetitions is the number of generations. */
    for (rep = 0; rep < repetitions; rep++) {
        if (print) {
            if (world_rank == 0) {
                print_grid(grid, grid_size);
                printf("---\n");
                usleep(200000);
            }
        }

        if ((rep % 2) == 0) {
            MPI_Startall(NUMBER_OF_NEIGHBOURS, requests_grid1_send);
            MPI_Startall(NUMBER_OF_NEIGHBOURS, requests_grid1_receive);
        } else {
            MPI_Startall(NUMBER_OF_NEIGHBOURS, requests_grid2_send);
            MPI_Startall(NUMBER_OF_NEIGHBOURS, requests_grid2_receive);
        }  

        /* if subgrid size > 2 we can calculate the inner cells without 
           having information about the neighbouring cells. 
         */
        if (subgrid_size > 2) {
            count_alive(&local_grid1, local_grid2, subgrid_size + 2, INNER, threads_num);          
        }

        /* Wait for the send's and the receive's to complete. */
        if ((rep % 2) == 0) {
            MPI_Waitall(NUMBER_OF_NEIGHBOURS, requests_grid1_receive, MPI_STATUSES_IGNORE);
        } else {
            MPI_Waitall(NUMBER_OF_NEIGHBOURS, requests_grid2_receive, MPI_STATUSES_IGNORE);
        }

        /* When you have received all the neighbouring cells,
           either calculate the outter columns, all the whole grid. 
         */
        if (subgrid_size > 2) {
            count_alive(&local_grid1, local_grid2, subgrid_size + 2, OUTTER, threads_num);
        } else if (subgrid_size == 2) {
            count_alive(&local_grid1, local_grid2, subgrid_size + 2, ALL, threads_num);
        }

        if ((rep % 2) == 0) {
            MPI_Waitall(NUMBER_OF_NEIGHBOURS, requests_grid1_send, MPI_STATUSES_IGNORE);
        } else {
            MPI_Waitall(NUMBER_OF_NEIGHBOURS, requests_grid2_send, MPI_STATUSES_IGNORE);
        }

        if (check_termination) {
            int local_sum = 0;
            int global_sum;

            /* Check if you need to terminate once every 10 reps */
            if (check_rep % 10 == 0) {

                /* First check if the current grid is empty */
                if (!empty_grid(local_grid2, subgrid_size + 2)) {
                    local_sum = 1;
                }

                MPI_Allreduce(&local_sum, &global_sum, 1, MPI_INT, MPI_SUM, comm_2D);

                if (global_sum == 0) {
                    break;
                }

                local_sum = 0;

                /* Then check if the new grid is the same with the previous grid. */
                if (!equal_grids(local_grid1, local_grid2, subgrid_size + 2)) {
                    local_sum = 1;
                }
          
                MPI_Allreduce(&local_sum, &global_sum, 1, MPI_INT, MPI_SUM, comm_2D);

                if (global_sum == 0) {
                    break;
                }
            }

            check_rep++;
        }

        int **swap;
        swap        = local_grid1;
        local_grid1 = local_grid2;
        local_grid2 = swap;

        if (print) {
            MPI_Gatherv(&local_grid1[0][0], 1, local_grid1_type, &grid[0][0], sendcounts, displs, subarray_type, 0, comm_2D);
        }
    }

    endtime = MPI_Wtime();
    double execution_time =  endtime - starttime;
    double maxtime;
    MPI_Reduce(&execution_time, &maxtime, 1, MPI_DOUBLE, MPI_MAX, 0, comm_2D);

    if ( world_rank == 0) {
        printf("Execution time: %f seconds\n", maxtime);
        printf("Repetitions: %d\n", rep);
    }

    for (int i = 0; i < NUMBER_OF_NEIGHBOURS; i++) {
        MPI_Request_free(&requests_grid1_send[i]);
        MPI_Request_free(&requests_grid1_receive[i]);
        MPI_Request_free(&requests_grid2_send[i]);
        MPI_Request_free(&requests_grid2_receive[i]);
    }

    MPI_Type_free(&type_north_row_send);
    MPI_Type_free(&type_south_row_send);
    MPI_Type_free(&type_north_row_recv);
    MPI_Type_free(&type_south_row_recv);

    MPI_Type_free(&type_west_column_send);
    MPI_Type_free(&type_east_column_send);
    MPI_Type_free(&type_west_column_recv);
    MPI_Type_free(&type_east_column_recv);

    MPI_Type_free(&type_north_east);
    MPI_Type_free(&type_south_east);
    MPI_Type_free(&type_south_west);
    MPI_Type_free(&type_north_west);

    if (readfile || print) {
        MPI_Type_free(&subarray_type);
        MPI_Type_free(&local_grid1_type);
        free_contiguous_grid(&grid);
    }

    free_contiguous_grid(&local_grid1);
    free_contiguous_grid(&local_grid2); 

    MPI_Finalize();
}