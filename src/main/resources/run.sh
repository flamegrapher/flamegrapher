#!/usr/bin/env bash

# Default values
HTTP_PORT=8080
while getopts 'p:' option; do
  case "$option" in
	p) HTTP_PORT=$OPTARG
	   printf "Http port : $OPTARG\n"
	   ;;
    :) printf "missing argument for -%s\n" "$OPTARG" >&2
       exit 1
       ;;
   \?) printf "illegal option: -%s\n" "$OPTARG" >&2
       exit 1
       ;;
  esac
done

# This script will be filtered, copied to this project basedir, and made executable
java -jar -DFLAMEGRAPHER_HTTP_PORT=$HTTP_PORT target/flamegrapher-${version}-fat.jar