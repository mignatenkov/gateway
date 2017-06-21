#!/bin/bash

# Notice! Base dir for this script and for the system parts must be the same. They must be runned from the same place!

# data save period: 10 days
SAVE_PERIOD=10

#===============================================
# Mongo Purge
#===============================================
SAVE_PERIOD_MONGO=$((1000 * 60 * 60 * 24 * $SAVE_PERIOD))
DB_NAME=gateway_collections

CUR_DATE=$(date +%s%N | cut -b1-13)
CUR_DATE=$(expr $CUR_DATE - $SAVE_PERIOD_MONGO)
TARGET_DATE_HUMAN_FRIENDLY=$(date -d @$(  echo "($CUR_DATE + 500) / 1000" | bc))

echo "=============================================================="
echo "Removing DELIVERED data before "$TARGET_DATE_HUMAN_FRIENDLY
echo "=============================================================="

mongo $DB_NAME --eval 'db.data.remove({"create_time":{$lte:'$CUR_DATE'},"status":"DELIVERED"})'
#mongo $DB_NAME --eval 'printjson(db.data.findOne({"create_time":{$lte:'$CUR_DATE'},"status":"DELIVERED"}))' #test

#===============================================
# FTP .done Purge
#===============================================

echo "=============================================================="
echo "Removing done ftp data before "$TARGET_DATE_HUMAN_FRIENDLY
echo "=============================================================="
find .done/* -mtime +$SAVE_PERIOD -exec rm {} \;