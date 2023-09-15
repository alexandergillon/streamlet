import sys
import time
import threading
import requests

if len(sys.argv) != 2:
    print("Incorrect number of arguments.")
    print("usage: python start_all_nodes_local.py <# PARTICIPANTS>")
    sys.exit()

numNodes = int(sys.argv[1])
startTime = 5000 + time.time_ns() // 1_000_000

def start_node(nodeId):
    port = 8080 + (nodeId + 1)
    r = requests.get(f"http://localhost:{port}/start?time={startTime}", timeout=10)
    if not r.ok: print(f"Error starting node {nodeId}: {str(r)}")
    else: print(f"Node {nodeId} started")

threads = []
for i in range(numNodes):
    thread = threading.Thread(target=start_node, args=(i,))
    thread.start()
    threads.append(thread)

for i in range(numNodes):
    threads[i].join()

print("Nodes started")