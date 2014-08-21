#!/bin/sh

for i in `seq 1979 2005`
do
  curl -O 'http://data.gdeltproject.org/events/'$i'.zip'
  unzip $i'.zip'
  rm $i'.zip'
  gzip $i.csv
  sleep 3
done

for year in `seq 2006 2012`
do
  for month in `seq 1 12`
  do
    mmzero=0$month
    mm=${mmzero: -2}
    curl -O 'http://data.gdeltproject.org/events/'$year$mm'.zip'
    unzip $year$mm'.zip'
    rm $year$mm'.zip'
    gzip $year$mm.csv
    sleep 3
  done
done