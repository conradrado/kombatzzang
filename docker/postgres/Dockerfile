FROM postgres:16-alpine

ENV POSTGRES_DB=matzzang
ENV POSTGRES_USER=matzzang
ENV POSTGRES_PASSWORD=matzzang

COPY init/ /docker-entrypoint-initdb.d/
