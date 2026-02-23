# docker build -t netmon2:1.0.0 .
docker build --no-cache -t netmon2:1.0.0 .
docker tag netmon2:1.0.0 matjazt/netmon2:1.0.0
docker push matjazt/netmon2:1.0.0