# Lamports-mutual-exclusion-algorithm

The application is responsible for generating critical section requests and then
executing critical sections on receiving permission from the mutual exclusion service. Model your
application using the following two parameters: inter-request delay and cs-execution time. The first
parameter denotes the time elapsed between when a node's current request is satisfied and when
it generates the next request. The second parameter denotes the time a node spends in its critical
section. Both inter-request delay and cs-execution time are random variables with exponential 
probability distribution.
