#!/bin/bash
cd /mnt/ ;
sudo mkdir mongo ;
sudo chmod 777 mongo/ ;
cd mongo ;
wget http://fastdl.mongodb.org/linux/mongodb-linux-x86_64-2.0.2.tgz ;
tar -xzvf mongodb-linux-x86_64-2.0.2.tgz ;
#start the mongo shard
nohup /mnt/mongo/mongodb-linux-x86_64-2.0.2/bin/mongod --port 27018 --rest --replSet rs_c --dbpath /mnt/mongo & ;
#start the configuration server
cd /mnt/ ;
sudo mkdir mongoconfig ;
sudo chmod 777 mongoconfig/ ;
cd mongoconfig ;
nohup /mnt/mongo/mongodb-linux-x86_64-2.0.2/bin/mongod --configsvr --dbpath /mnt/mongoconfig/ & ;
