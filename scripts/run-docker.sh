BASEDIR=$(pwd)

docker run \
  --rm -it \
  -v $BASEDIR/config:/kobot/config \
  kobot:latest

