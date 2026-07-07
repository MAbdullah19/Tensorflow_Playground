FROM eclipse-temurin:17-jdk

WORKDIR /app

# Copy sources and compile with plain javac (zero build tools, matches build.sh)
COPY src ./src
RUN find src/main/java -name "*.java" > sources.txt \
    && javac -d out --release 11 @sources.txt \
    && rm sources.txt

# Render (and most PaaS) inject a PORT env var; Main.java already reads it.
EXPOSE 8080
CMD ["java", "-cp", "out", "com.playground.Main"]
