#!/bin/bash
cd /mnt/ ;
sudo mkdir mongo ;
sudo chmod 777 mongo/ ;
cd mongo ;
wget http://fastdl.mongodb.org/linux/mongodb-linux-x86_64-2.0.2.tgz ;
tar -xzvf mongodb-linux-x86_64-2.0.2.tgz ;
nohup /mnt/mongo/mongodb-linux-x86_64-2.0.2/bin/mongod --port 27017 --dbpath /mnt/mongo &
