BASEDIR=$(pwd)

docker run \
  --rm -it \
  -v $BASEDIR/data:/data \
  kobot:latest $*