FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /build
COPY mvnw pom.xml ./
COPY .mvn/ .mvn/
# go-offline skips the Swagger UI webjar, which is a plugin artifact rather than a dependency.
RUN ./mvnw -q -B dependency:go-offline dependency:unpack@swagger-ui-assets
COPY src/ src/
# Tests run in CI, not in the image build; offline, so a dependency missing from the layer above fails here.
RUN ./mvnw -q -B -o -DskipTests package

FROM eclipse-temurin:17-jre-jammy
RUN groupadd --system app && useradd --system --gid app --no-create-home app
USER app
WORKDIR /app
COPY --from=build /build/target/time-deposit.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
