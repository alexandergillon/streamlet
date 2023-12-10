# Streamlet

An implementation of the Streamlet blockchain protocol. The Streamlet protocol, presented [here](https://eprint.iacr.org/2020/088) by Benjamin Chan and Elaine Shi, is a Byzantine fault-tolerant distributed consensus protocol for a permissioned blockchain. It emphasizes simplicity and is meant to be understandable for those without any experience with blockchains.

The purpose of this project is to learn more about blockchains and distributed systems. It isn't to make the most performant or efficient blockchain (the Streamlet protocol was not designed with this in mind). As a result, I've started with simple implementations, and plan to improve things from there. Google 'Gall's Law' 🙂.

## Description

This implementation of the Streamlet protocol uses a blockchain to implement a distributed chat room. Data on the blockchain is an ordered sequence of messages with timestamps, where each message (and metadata) is the payload of a block in the blockchain. An example of what the blockchain might represent at a certain point in time:

```text
2023/09/14 11:37 | funny_user: Why did the chicken cross the playground?
2023/09/14 01:20 | john_doe: I don't know, why?
2023/09/14 01:20 | funny_user: To get to the other slide.
```

## Architecture

First, a bit of context. The network consists of `n` nodes, for some fixed `n`. The protocol begins at some specific start time (which must be synchronized across the nodes), and proceeds in epochs of length `t` (a constant: currently 2 seconds in this implementation). Each epoch has a leader, who has the opportunity to propose a block to all other nodes. Other nodes have the opportunity to vote on a proposed block that they have seen at any time. For more information on how nodes propose and vote, see the [Streamlet paper](https://eprint.iacr.org/2020/088).

In the Streamlet paper, the way in which nodes communicate is via broadcasting - they send their message to all other nodes. I have implemented this by creating a broadcast server (which is a Java application). Nodes send their message to the broadcast server, who forwards that message to all other nodes. A broadcasting server was desired anyway (detailed in next paragraph), so I decided to collect all broadcasting functionality under one server, rather than have nodes do it themselves.

For users to propose a payload to be included in some block in the blockchain, they would ideally broadcast their proposed payload to all nodes (or at least a sizeable portion of them), and nodes would subsequently propose them as part of blocks. However, this would be cumbersome for actual users. As a result, the way in which users propose a message to the blockchain is via an HTTP POST request to the `/send` endpoint of the broadacst server. The broadcast server then forwards this proposal to all nodes.

Apache Kafka is used for all communication between nodes. Interactions between the user and nodes / the broadcast server is via REST HTTP calls. 

## Getting Started

### Dependencies

Everything is written in Java, so should work cross-platform. The intended method of deployment is via Docker containers, which work best in a Unix environment. For Windows users, I recommend Ubuntu running on the Windows Subsystem for Linux, with Docker Desktop installed on Windows and with WSL integration turned on. See [here](https://docs.docker.com/desktop/wsl/) for more information on Docker Desktop and the WSL. 

From this point onwards I will assume a Unix environment.

### Building

The two applications can be built with the following:

```shell
./mvnw clean package spring-boot:repackage -Dmaven.test.skip=true
```

This creates a 'uber Jar' which is executable and contains all dependencies needed to run the program. Tests need to be skipped as Kafka will cause tests to hang forever as the application cannot connect to the Kafka broker.

If you choose, you could run the applications as is. I choose to package the applications into Docker containers. There are a number of scripts in the `docker/` directory of the repository which will both compile the Jar and build a container image for the node and broadcast applications. There is also one that packages a Kafka server into a Docker container.

### Executing

In order to run the applications, there are a number of environment variables that need to be defined when they are launched. Also, you should probably pass through port mappings from the host machine to the Docker container via Docker. In general, reading the various shell scripts that I have written should hopefully demonstrate how the network setup is working.

By application, required environment variables are:

- Kafka Server:
  ```text
  KAFKA_ADVERTISED_LISTENERS - This is where Kafka will tell nodes that connect to the network
                               where they can find brokers. By default, Kafka will tell them to
                               connect on localhost:9092, but if Kafka is running in a Docker 
                               container or not running on the same machine as other applications, 
                               localhost:9092 will not resolve to the Kafka server. If running
                               on a Docker container on the same machine as other applications that
                               are also running in Docker containers, this variable should be
                               'PLAINTEXT://172.17.0.1:9092'. If Kafka is running in the cloud, it
                               would likely use the public IP of your cloud VM instead.
  ```
  On Docker containers, `172.17.0.1` resolves to the host machine that the Docker container is running on.

- Broadcast server:
  ```text
  
  STREAMLET_KAFKA_BOOTSTRAP_SERVERS - Address of the bootstrap server for Kafka. If running on a
                                      Docker container on a host where Kafka is also running on a
                                      Docker container, this should probably be 172.17.0.1. If Kafka
                                      is running in the cloud, this should be its publicly facing IP
                                      address.
  ```
  
- Node server:
  ```text
  STREAMLET_PARTICIPANTS - Number of nodes in the Streamlet network.
  STREAMLET_NODE_ID - Unique numeric ID that identifies the node. 0 <= STREAMLET_NODE_ID < STREAMLET_PARTICIPANTS.
  STREAMLET_KAFKA_BOOTSTRAP_SERVERS - Address of the bootstrap server for Kafka. See description for
                                      broadcast server above for more information.
  ```

Also, after the nodes are running, their start time needs to be set via a HTTP GET request to `<node address>/start?time=<start time>`, where `start time` is when epoch 0 begins, measured in milliseconds since the Unix epoch. `start time` must be in the future. This needs to be performed on every node, and they all must be supplied the same value in order to be correctly synchronized.

#### Running Locally

In order to run the entire network locally, I have written a number of scripts that make this process convenient. In order, run:

1. `docker/kafka/docker-run-local.sh` - runs the Kafka server.
2. `docker/broadcast/docker-run-local.sh` - runs the broadcast server. `STREAMLET_PARTICIPANTS` is hard-coded in this file - edit the file to modify this.
3. `docker/node/start-network-local.sh <STREAMLET_PARTICIPANTS>` - starts `STREAMLET_PARTICIPANTS` number of nodes.
4. `python docker/node/set_node_start_time.py` - sets the start time of all nodes to 5 seconds after this script is called, which causes the network to begin. 

### Interacting With the Blockchain

At present, the following endpoints allow you to interact with the blockchain, while it is running. On the broadcast server:

```text
POST /send                - sends a message to all nodes, to be included in the blockchain
```

On the node server:

```text
GET /chain/readable       - retrieves the finalized blockchain, as known to this node, in a human-readable format
GET /chain/json           - retrieves the finalized blockchain, in a JSON-friendly format
```

All applications are running Swagger UI. Go to `/swagger-ui.html` to see example HTTP requests that can be made to the servers.

## License

This project is licensed under the GNU GPLv3 License - see the LICENSE.md file for details.