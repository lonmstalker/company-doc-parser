FROM alpine

RUN curl -s "https://get.sdkman.io" | bash

RUN sdk install java 19.0.0-grl \
    && gu install native-image

RUN sdk install maven

WORKDIR /workspace

ADD algorithm-db algorithm-db
ADD algorithm-impl algorithm-impl
ADD pom.xml pom.xml

RUN mvn -Pnative -DskipTests package