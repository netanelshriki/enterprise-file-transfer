FROM rustlang/rust:nightly as builder

WORKDIR /app
COPY Cargo.toml Cargo.lock ./
COPY src ./src

RUN cargo build --release

FROM debian:bookworm-slim

RUN apt-get update && apt-get install -y \
    ca-certificates \
    libssl3 \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY --from=builder /app/target/release/signaling-server /app/signaling-server

RUN mkdir -p /app/data && \
    chown -R 1000:1000 /app/data

EXPOSE 8080

ENV RUST_LOG=info
ENV DATABASE_URL=/app/data/signaling.db
ENV BIND_ADDRESS=0.0.0.0:8080

USER 1000

CMD ["./signaling-server", "--database", "/app/data/signaling.db", "--bind", "0.0.0.0:8080"]
