#!/bin/sh

for i in `seq 1979 2005`
do
  curl -O 'http://data.gdeltproject.org/events/'$i'.zip'
  unzip $i'.zip'
  rm $i'.zip'
  sleep 3
done