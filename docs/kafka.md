# Kafka Usage

This implementation uses Kafka for messaging between nodes. Kafka was chosen for two reasons:

1. Kafka is fault-tolerant and has persistent message storage. The Streamlet paper assumes a model of communication where messages are eventually delivered to nodes. Kafka's persistent message storage ensures that messages are not lost while sitting in the queue, waiting to be read. Furthermore, as Kafka allows messages to be re-read, nodes can recover from failure without message loss. For example, if a node goes down while processing a message, it can re-read the message (ensuring the message is not lost).
2. I wanted to learn Kafka as it seems like an interesting technology.

## Kafka and Broadcasting

The Streamlet paper assumes a broadcasting method of communication. I.e. that every message sent by a node is broadcast to all nodes. This is achieved in this implementation with a dedicated broadcast server. Nodes send their message to the broadcast server, and the broadcast server ensures that the message is broadcast to all other nodes.

This is an unnecessary level of indirection - nodes could just broadcast their messages directly to all nodes. However, for users to interact with the blockchain, a broadcast server was desired (so that users could send a proposed block to one place and have it reach all nodes). As a result, I decided to also have nodes do the same thing, so that most communication-related code sits together on the broadcast server. The broadcast server is therefore essentially an abstraction on actual broadcasting by nodes.

Ultimately, this project is a learning exercise, so I am not too concerned with the overhead that this adds.

## Kafka Topics

Each node has three dedicated Kafka topics: one for proposed payloads, one for proposed blocks, and one for votes. For a node with ID `i`, these topics are:

- `payloadsForNodei`
- `proposalsForNodei`
- `votesForNodei`

Then, there is an overall `broadcast` topic. This is where nodes send their messages to the broadcast server over Kafka.

## JSON Schemas

### Broadcasts
For messages on the `broadcast` topic:

```json
{
  "sender" : int,
  "messageType": "propose" or "vote",
  "message" : JSON
}
```

Messages are broadcast to the appropriate topics based on the `messageType` field. The `sender` field is naively trusted by the broadcast server, and is only used so that the broadcast server does not send messages back to the server. Because the broadcast server is essentially an abstraction on actual broadcasting by each node, it is not a focus of this project to make it entirely secure. Note: there is no `payload` option for `messageType` as payloads are delivered to the broadcast server via REST API.

### Payloads
For messages on a `payloadsForNodei` topic:

```json
{
  "username": "the username of the user sending the message",
  "text": "The text of the message",
  "timestamp": milliseconds since the epoch of when the broadcast server 
               received this message (JSON int, Java long)
}
```

### Proposals
For messages on a `proposalsForNodei` topic:

```json
{
  "nodeId": int,
  "block": {
    "parentHash": "parent hash encoded as base-64 string",
    "epoch": the epoch of this block (int),
    "payload": "data encoded as a base-64 string"
  },
  "signature": "signature encoded as a base-64 string"
}
```

`nodeId` is supposed to be the ID of the node who sent this message. However, if Byzantine behavior occurs, it could be the ID of any node (not just the sender). The digital signature must be used to verify this node ID.

### Votes
For messages on a `votesForNodei` topic:

```json
{
  "nodeId": int,
  "block": {
    "parentHash": "parent hash encoded as base-64 string",
    "epoch": the epoch of this block (int),
    "payload": "data encoded as a base-64 string"
  },
  "signature": "signature of the voter encoded as a base-64 string",
  "proposerSignature" : "signature of the original proposer of the block, as a base-64 encoded string"
}
```

`nodeId` is supposed to be the ID of the node who sent this message. However, if Byzantine behavior occurs, it could be the ID of any node (not just the sender). The digital signature must be used to verify this node ID.

`proposerSignature` is sent so that nodes can easily discard Byzantine votes (votes on blocks that were never actually proposed). By sending this signature, a vote message confirms that the block it is voting on was actually proposed by the leader of that round.