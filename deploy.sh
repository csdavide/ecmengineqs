#!/bin/sh
ENVIRONMENT=$1
cd target
mv ecmengineqs-1.0.0-SNAPSHOT-runner.jar ecmengineqs-$ENVIRONMENT.jar
tar cvf ecmengineqs-1.0.0.tar ecmengineqs-$ENVIRONMENT.jar
gzip ecmengineqs-1.0.0.tar