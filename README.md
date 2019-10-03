# Java-Banking-Client-Server
A java banking client-server that takes advantage of concurrency

Description
A server to handle accounts is to be built. Any number of (trusted) brokers are allowed
to connect to the server and perform a number of actions detailed below. Each account
consists of an account number, and a pair of floating point values. The first floating point
value is the amount of Arian, and the second is the amount of Pres currently in the account.
The server is to listen on port number 4242 on localhost. The brokers will connect on
that port.

Task
Develop a multithreaded server with a thread for each broker connecting to the server.
The server should maintain all opened accounts and a current conversion rate between
Arian and Pres. Commands from the brokers must be accepted at any time. Reasonable
responses should be issued for any malformed command. Balances may be negative.

Test1 creates 900 clients that tries to do one convert command each, while a separate client changes the rate repeatedly. This was intended to catch if the rate was read more than once while converting which would cause inconsistencies. This test seems to have been sensitive to outside issues due to the high use of sockets. It has therefore been given low weight.

Test2 creates 10 clients doing 1000 converts each while a separate client checks the state repeatedly for consistency.

Test3 creates 8 clients, in the first phase they are used to transfer between 2 accounts checking for potential deadlocks. In the second phase the same number of transfers are made but without contention, so this should perform faster. In both phases there is a check that all transfers have been performed and none lost.

Test4 creates 8 clients that do transfers while a separate clients tests the state for consistency.
