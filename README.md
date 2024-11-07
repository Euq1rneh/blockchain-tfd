# Blockchain - Distributed Fault Tolerance Project
### Course: MEI/MI/MSI 2024/25

## Project Overview
This project involves implementing a fault-tolerant distributed system using the **Streamlet consensus algorithm** and Java21(**!!!CONFIMAR!!!!**), designed for blockchain replication. The project is divided into two phases:
1. **Phase 1**: Basic implementation of the Streamlet protocol with crash fault tolerance.
2. **Phase 2**: Enhanced implementation including node delays, missed epochs, and crash-recovery capabilities.

## Objective
The goal is to develop a distributed ledger with fault-tolerant properties, simulating a consensus algorithm where transactions are generated and recorded in distributed nodes, ensuring consistency and fault tolerance.

## Project Structure
- **Node Library**: Each node operates as an independent process that creates, orders, and verifies blocks.
- **Streamlet Protocol Implementation**: Nodes communicate through a propose-vote-finalize system, handling epochs to achieve consensus on the blockchain.

## Files in This Repository
- `src/`: Source code files for the project.
- `dist/`: Directory where the executable will be
- `docs/`: Directory containing any project related documents
- `README.md`: This file, explaining how to set up and run the system.
- `compilation-script`: Script to compile all source files into a single jar file
- `config.txt`: Configuration file that contains the necessary parameters to run the program.
- `peers.txt`: Configuration file that contains the `ID`, `IP`, `Port` of each of the nodes in the network

## Installation and Setup
1. **Compilation scrip**: Run the compilation script. This will generate a jar file to the `dist` directory. 
   ```bash
   ./compilation-script
   ```
2. **Check the config files**: Check that the configuration files are correct.  
The `config.txt` should look something like this:
    ```
    start_time=16:56:00 //Time for the program to start
    seed=123456789 //Seed for lider election
    round_duration_sec=2 //Duration of a round in the streamlet protocol
    ```
    The `peers.txt` should look something like this:
    ```
    1 127.0.0.1:11111 //id ip:port
    2 127.0.0.1:22222
    3 127.0.0.1:33333
    4 127.0.0.1:44444
    5 127.0.0.1:55555
    ```
3. **Run the program**: If your peer.txt file contains 5 lines (which correspond to 5 different nodes), you'll need to run 5 instances of the program with the following line (assuming you're in the root directory):
    ```bash
    java -jar /dist/streamlet.jar <peerFile> <configFile> <peerID>
    ```
    Replace `peerFile`, `configFile` with the paths for those files and `peerID` with the appropriate ID contained in your `peerFile`.


