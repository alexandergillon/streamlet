STREAMLET_PARTICIPANTS=5

docker run --init \
-e STREAMLET_PARTICIPANTS=$STREAMLET_PARTICIPANTS \
-p 8080:8080 \
-p 9092:9092 \
alexandergillon/projects:broadcast 