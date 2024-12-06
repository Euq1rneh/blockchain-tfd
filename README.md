# Blockchain - Distributed Fault Tolerance Project
### Course: MEI/MI/MSI 2024/25

## Project Overview
This project involves implementing a fault-tolerant distributed system using the **Streamlet consensus algorithm** and Java, designed for blockchain replication. The project is divided into two phases:
1. **Phase 1**: Basic implementation of the Streamlet protocol with crash fault tolerance.
2. **Phase 2**: Enhanced implementation including node delays, missed epochs, and crash-recovery capabilities.

The algorithm used works in epochs, an epoch is 2Δ rounds of equal duration. We considered Δ to be the duration of a round.

A logger is used to simplify the debbuging process and a log file is created for each node.  

A text file is created for each node containing part of the blockchain to reduce memory usage. This file is only updated if there is an excess of 10 blocks in memory while trying to finalize a new chain. With this feature the blockchain now is a combination of what's logged into that file and what is in memory.

## Project Structure
- **Node**: Each node operates as an independent process that creates, orders, and verifies blocks.
- **Streamlet Protocol Implementation**: Nodes communicate through a propose-vote-finalize system, handling epochs to achieve consensus on the blockchain.

## Files in This Repository
- `src/`: Source code files for the project.
- `dist/`: Directory where the executable will be
- `docs/`: Directory containing any project related documents
- `compilation-script`: Script to compile all source files into a single jar file
- `config.txt`: Configuration file that contains the necessary parameters to run the program.
- `peers.txt`: Configuration file that contains the `ID`, `IP`, `Port` of each of the nodes in the network

## Installation and Setup
1. **Check the config files**: Check that the configuration files are correct.  
The `config.txt` should look something like this:
    ```
    start_time=16:56:00 	//Time for the program to start
    seed=123456789 			//Seed for lider election
    round_duration_sec=2 	//Duration of a round in the streamlet protocol
	recovery_epochs=2 		//Number of epochs used to allow a node to recover
	confusion_start=5		//Epoch where a period of confusion should start
	confusion_duration=5	//Number of epochs the period of confusion should last
    
    ```
    The `peers.txt` should look something like this:
    ```
    1 127.0.0.1:11111 //id ip:port
    2 127.0.0.1:22222
    3 127.0.0.1:33333
    4 127.0.0.1:44444
    5 127.0.0.1:55555
    ```
2. **Compilation script**: Run the compilation script.  
This will generate a jar file to the `dist` directory. It also increments the start time, in the config file, by 1 minute based on the current time.  
   ```bash
   ./compilation-script
   ```
3. **Run the program**: If your peer.txt file contains 5 lines (which correspond to 5 different nodes), you'll need to run 5 instances of the program with the following line (assuming you're in the root directory):
    ```bash
    java -jar dist/streamlet.jar <peerID> <peerFile> <configFile>
    ```
    Replace `peerFile`, `configFile` with the paths for those files and `peerID` with the appropriate ID contained in your `peerFile`.

> [!NOTE]
> The value of the configuration parameter `round_duration_sec` should be directly proportional to the value of `confusion_duration`. Otherwise message processing could be cut short for some messages.  

> [!WARNING]
> The values of the configuration parameters `confusion_start` and `confusion_duration` should always be bigger than `0` if confusion epochs are expected to appear. The combination of `confusion_start=1` and `confusion_duration=1` results in no epochs of confusion


## Shortcommings
- Phase 1: Nothing to report. All requirements met.
- Phase 2: Nothing to report. All requirements met.

## References
DAG implementation used for blockchain logic- https://github.com/ajs1998/Dag
