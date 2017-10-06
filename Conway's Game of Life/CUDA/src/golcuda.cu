#include "cuda_grid_functions.h"

__global__ void countNeighbours(int *grid, int *resultGrid, int columns, int rows) {
  
	/* ignore the wrapper threads */
	int x = blockDim.x*blockIdx.x + threadIdx.x;

	/*  check if this thread is part of the right wrapper column */
	int rcol = (columns * rows - x ) % columns;
	
	if( ( x < (columns * rows)) && 
	    ( rcol !=1 ) 		    && 
	    ( x % (columns) != 0)   &&
	    ( x > columns )         &&
	    ( x < ((columns * rows - columns) -1))
	   ) {
		int sum = grid[x + 1]       + grid[x - 1] + 
	    		  grid[x + columns] + grid[x + columns + 1] + grid[x + columns - 1] +
	    		  grid[x - columns] + grid[x - columns + 1] + grid[x - columns - 1];

	    if (sum <= 1) {
	    	resultGrid[x] = DEAD;
	    } else if ((sum == 2 || sum == 3) && (grid[x] == ALIVE)) {
	        resultGrid[x] = ALIVE;
	    } else if ((sum >=4) && (sum <= 8)) {
	        resultGrid[x] = DEAD;
	    } else if ((sum == 3) && (grid[x] == DEAD)) {
			resultGrid[x] = ALIVE;
	    } else if ((sum == 2) && (grid[x] == DEAD)) {
	        resultGrid[x] = DEAD;
	    }
	    
	}
    __syncthreads();
}

int main(int argc, char **argv) {

	int *host_grid          = NULL;
	int *previous_host_grid = NULL;
	int *host_grid_result   = NULL;
	int *device_grid        = NULL;
	int *device_grid_result = NULL;

	int repetitions;
	bool check_termination = false;
	bool print             = false;
	bool readfile          = false;
	int grid_size;

	int err = parse_command_line(argc, argv, &host_grid, &host_grid_result, 
														 &previous_host_grid, 
														 &repetitions, 
														 &grid_size, 
														 &check_termination, 
														 &print, &readfile);

	if (err < 0) {
		exit(0);
	}

	printf("grid size: %d\n repetitions: %d\n", grid_size, repetitions);

	copy_wrapper(host_grid, grid_size);

	int number_of_blocks  = (grid_size * grid_size) / THREADS_PER_BLOCK;
	int remaining_threads = (grid_size * grid_size) % THREADS_PER_BLOCK;

	if (remaining_threads > 0) {
		number_of_blocks++;
	}

	printf("Number of blocks: %d\n", number_of_blocks );

	cudaDeviceSynchronize();

	cudaMalloc((void **)&device_grid, 	     WRAPPER_SIZE * sizeof(int));
	cudaMalloc((void **)&device_grid_result, WRAPPER_SIZE * sizeof(int));
	cudaMemset(device_grid_result, 0,        WRAPPER_SIZE * sizeof(int));
	cudaMemcpy(device_grid, host_grid, 	     WRAPPER_SIZE * sizeof(int), cudaMemcpyHostToDevice);

	if (print) {
		print_grid(host_grid, grid_size);
		printf("~~~~");
	}

	clock_t start = clock();

 	for(int rep = 0; rep < repetitions; rep++) {
		countNeighbours<<<number_of_blocks, THREADS_PER_BLOCK>>>(device_grid, device_grid_result, grid_size + 2, grid_size + 2);
		
		cudaDeviceSynchronize();
		cudaMemcpy(host_grid_result, device_grid_result, WRAPPER_SIZE * sizeof(int), cudaMemcpyDeviceToHost);

		if (print) {
			print_grid(host_grid_result, grid_size);
			printf("~~~~");
		}

		/* If we want to check for termination we do it every 10 reps. */
		if (check_termination) {
			if (rep % 10 == 0) {
				if (equalGrids(host_grid_result, host_grid, grid_size) || empty_grid(host_grid_result, grid_size)) {
				printf("Grid unchanged or empty. Exiting...\n");
					break;
				}
			}
		}

		copy_wrapper(host_grid_result, grid_size);
		
		/* Copy again the new grid back to device. */
		cudaMemcpy(device_grid, host_grid_result, WRAPPER_SIZE * sizeof(int), cudaMemcpyHostToDevice);	

		/* Swap for future checks. */
		int *swap;
        swap             = host_grid_result;
        host_grid_result = host_grid;
        host_grid        = swap;
	}

	clock_t end = clock();
	float seconds = (float)(end - start) / CLOCKS_PER_SEC;

    printf("Execution time: %.6f secs. \n", seconds);

    free(host_grid_result);
    free(host_grid);
    cudaFree(device_grid);
    cudaFree(device_grid_result);

	exit(EXIT_SUCCESS);
}