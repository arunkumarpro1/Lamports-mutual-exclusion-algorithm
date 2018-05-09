# Lamports-mutual-exclusion-algorithm

Implement a mutual exclusion service using Lamport's distributed mutual exclusion algorithm. Model your application using the following two parameters: inter-request delay and cs-execution time. There are two main function calls

1) csEnter() allows an application to request permission to start executing its critical section. The function
call is blocking and returns only when the invoking application can execute its critical section.

2) csLeave() allows an application to inform the service that it has finished executing its critical section.

Each process or node consists of two separate modules. The top module implements the application (requests and executes critical
sections). The bottom module implements the mutual exclusion service. The two modules interact using csEnter() and csExit() functions.

Application: The application is responsible for generating critical section requests and then executing critical sections on receiving permission from the mutual exclusion service. The application is modelled using the following two parameters: inter-request delay and cs-execution time. The first parameter denotes the time elapsed between when a node's current request is satisfied and when it generates the next request. The second parameter denotes the time a node spends in its critical section. Assume that both inter-request delay and cs-execution time are random variables with exponential probability distribution.

The program uses a plain-text formatted configuration file in the following format: Only lines which begin with an unsigned integer are considered to be valid. Lines which are not valid are ignored. The configuration file must contain contain n + 1 valid lines. The first valid line of the configuration file contains four tokens. The first token is the number of nodes (n) in the system. The second token is the mean value for inter-request delay (in milliseconds). The third token is the mean value for cs-execution time (in milliseconds). The fourth token is the number of requests each node should generate. After the first valid line, the next n lines must consist of three tokens. The first token must be the node ID. The second token must be the host-name of the machine on which the node runs. The third token must be the port on which the node listens for incoming connections. The # character will denote a comment. On any valid line, any characters after a # character are ignored. 

Example configuration file

5 20 10 1000
0 dc02 1234
1 dc03 1233
2 dc04 1233
3 dc05 1232
4 dc06 1233
2
