# Kafka Order Processing

Educational application for learning Apache Kafka through a real-world use case — **Order Processing Pipeline**.

User creates an order in React UI, the event flows through a Kafka pipeline (validation → payment → shipping), and the frontend tracks every step in real-time via SSE.

---

## Table of Contents

- [Architecture](#architecture)
- [How to Run](#how-to-run)
- [What to Observe](#what-to-observe)
- [Project Structure](#project-structure)
- [Apache Kafka — Deep Dive](#apache-kafka--deep-dive)
  - [What is Kafka?](#what-is-kafka)
  - [Broker](#broker)
  - [Topic](#topic)
  - [Partition](#partition)
  - [Producer](#producer)
  - [Consumer](#consumer)
  - [Consumer Group](#consumer-group)
  - [Offset](#offset)
  - [Message Key](#message-key)
  - [KRaft Mode](#kraft-mode)
  - [Serialization](#serialization)
- [How the Pipeline Works](#how-the-pipeline-works)
- [SSE vs WebSocket](#sse-vs-websocket)
- [Scaling — What Happens with Multiple Instances](#scaling--what-happens-with-multiple-instances)
- [Where Would Redis Fit In?](#where-would-redis-fit-in)

---

## Architecture

```
[React :5173]
    |  POST /api/orders              (create order)
    |  GET /api/events/stream/{id}   (SSE stream)
    v
[Spring Boot :8080]
    |  KafkaTemplate.send("order-created", event)
    v
[Kafka :9094]
    |
    |  order-created   --> OrderConsumer    --> payment-pending
    |  payment-pending --> PaymentConsumer  --> payment-success / payment-failed
    |  payment-success --> ShippingConsumer --> order-shipped
    |
    |  NotificationConsumer listens to ALL topics --> SSE push to frontend
    v
[React Timeline - real-time update]
```

## How to Run

### Prerequisites
- Docker & Docker Compose
- Java 17+
- Maven
- Node.js 18+

### 1. Start Kafka infrastructure

```bash
docker compose up -d
```

Wait ~15 seconds for topics to be created. Verify at **http://localhost:8085** (Kafka UI).

### 2. Start the backend

```bash
cd backend
mvn spring-boot:run
```

You should see: `Started KafkaDemoApplication`

### 3. Start the frontend

```bash
cd frontend
npm install
npm run dev
```

Open **http://localhost:5173**

### 4. Test the pipeline

1. Click **"Place Order"** in the UI
2. Watch the timeline fill up in real-time:
   - Order Created (instant)
   - Payment Pending (+1.5s)
   - Payment Success/Failed (+2s)
   - Order Shipped (+2.5s, only if payment succeeded)
3. Open **http://localhost:8085** to see messages in each topic
4. Check backend logs to see the consumer chain in action

## What to Observe

- **Kafka UI** — see messages appearing in each topic as the order progresses
- **Backend logs** — each consumer logs when it receives and forwards an event
- **Frontend timeline** — real-time SSE updates as each pipeline stage completes
- **Payment failures** — 20% of orders will fail at payment, stopping the pipeline
- **Multiple orders** — create several orders and watch them process independently

## Project Structure

```
kafkaSmall/
├── docker-compose.yml              # Kafka KRaft + Kafka UI
├── backend/
│   ├── pom.xml                     # Spring Boot 3.3, spring-kafka, lombok
│   └── src/main/java/com/kafka/demo/
│       ├── KafkaDemoApplication.java
│       ├── config/
│       │   ├── KafkaConfig.java         # 5x NewTopic beans
│       │   └── CorsConfig.java          # CORS for localhost:5173
│       ├── model/
│       │   └── OrderEvent.java          # orderId, productName, quantity, price, status, timestamp
│       ├── controller/
│       │   ├── OrderController.java     # POST /api/orders
│       │   └── SseController.java       # GET /api/events/stream/{orderId}
│       ├── producer/
│       │   └── OrderProducer.java       # KafkaTemplate wrapper
│       ├── consumer/
│       │   ├── OrderConsumer.java       # order-created → payment-pending
│       │   ├── PaymentConsumer.java     # payment-pending → success/failed
│       │   ├── ShippingConsumer.java    # payment-success → order-shipped
│       │   └── NotificationConsumer.java # all topics → SSE push
│       └── service/
│           ├── OrderService.java        # creates OrderEvent with UUID
│           └── SseService.java          # ConcurrentHashMap<orderId, List<SseEmitter>>
└── frontend/
    └── src/
        ├── App.tsx                      # layout: form + list | timeline
        ├── types/order.ts               # TypeScript types
        ├── hooks/useOrderEvents.ts      # EventSource SSE hook
        └── components/
            ├── OrderForm.tsx            # order creation form
            ├── OrderTimeline.tsx        # visual event timeline
            └── OrderList.tsx            # list of all orders with status
```

## Stopping

```bash
docker compose down        # stop Kafka
docker compose down -v     # stop + remove data
```

---

# Apache Kafka — Deep Dive

## What is Kafka?

Apache Kafka is a **distributed event streaming platform**. Think of it as a highly durable, high-throughput message bus. Producers write events (messages) to Kafka, and consumers read them. Kafka stores events on disk, so they persist and can be replayed.

Kafka was originally developed at LinkedIn to handle trillions of events per day. It is now used by most large tech companies for real-time data pipelines, event-driven architectures, and stream processing.

Key properties:
- **Durable** — messages are written to disk and replicated across brokers
- **Ordered** — messages within a partition are strictly ordered (FIFO)
- **Scalable** — add more brokers and partitions to increase throughput
- **Decoupled** — producers and consumers don't know about each other

---

## Broker

A broker is a **Kafka server process** that receives, stores, and serves messages. It is the heart of Kafka.

```
Producer → [Broker 1] → Consumer
           [Broker 2]
           [Broker 3]
```

- In this application we have **1 broker** (the Docker container `kafka`)
- In production you typically have **3, 5, or more** brokers — there is no upper limit
- More brokers enable:
  - **Replication** — if one broker dies, another has a copy of the data
  - **Scalability** — more brokers = more messages per second
  - **Partition distribution** — partitions are spread across brokers

Analogy: a broker is like a **post office**. You can have one in a small town or hundreds across a country.

---

## Topic

A topic is a **named category/channel** for messages. Producers send messages to a topic, consumers read from a topic.

In this application we have 5 topics:

| Topic | Description | Who Produces | Who Consumes |
|---|---|---|---|
| `order-created` | New order placed | OrderService | OrderConsumer, NotificationConsumer |
| `payment-pending` | Awaiting payment | OrderConsumer | PaymentConsumer, NotificationConsumer |
| `payment-success` | Payment succeeded | PaymentConsumer | ShippingConsumer, NotificationConsumer |
| `payment-failed` | Payment failed (20%) | PaymentConsumer | NotificationConsumer |
| `order-shipped` | Order shipped | ShippingConsumer | NotificationConsumer |

Topics are created in two places in this project:
1. **Docker** — `init-kafka` container runs `kafka-topics --create` for each topic
2. **Spring Boot** — `KafkaConfig.java` defines `NewTopic` beans that auto-create topics on startup

Both use "create if not exists" logic, so they never conflict.

Analogy: a topic is like a **TV channel**. Producers broadcast to it, consumers tune in.

---

## Partition

A partition is a **physical subdivision** of a topic. A topic is a logical name, but partitions are where messages are actually stored.

Our topic `payment-pending` has 3 partitions:

```
payment-pending-0:  [msg A] [msg D] [msg G]
payment-pending-1:  [msg B] [msg E]
payment-pending-2:  [msg C] [msg F] [msg H]
```

Why partitions matter:

1. **Parallelism** — with 1 partition, only 1 consumer can read. With 3 partitions, up to 3 consumers can read in parallel
2. **Ordering** — messages within a single partition are strictly ordered (FIFO). Messages across partitions have no ordering guarantee
3. **Scaling** — the number of partitions is the maximum number of parallel consumers in a group

How a message ends up in a specific partition:
```java
// OrderProducer.java
kafkaTemplate.send(topic, event.getOrderId(), event);
//                          ↑ this is the key
```
Kafka computes `hash(orderId) % numPartitions` to determine the partition. Same key always goes to the same partition.

In our application: 1 backend instance, 3 partitions per topic. One consumer reads all 3 partitions alone. No parallelism, but the partitions are ready for scaling.

---

## Producer

A producer is a process that **sends messages** to Kafka topics.

In this application, `OrderProducer.java` wraps Spring's `KafkaTemplate`:

```java
kafkaTemplate.send(topic, event.getOrderId(), event);
//                  ↑          ↑               ↑
//             topic name   message key     payload (serialized to JSON)
```

What happens when you call `send()`:
1. `JsonSerializer` converts the `OrderEvent` object to JSON bytes
2. The key (`orderId`) is hashed to determine the target partition
3. The message is sent over TCP to the Kafka broker
4. The broker appends it to the partition's log on disk
5. The broker acknowledges the write back to the producer

---

## Consumer

A consumer is a process that **reads messages** from topics and does something with them.

In Spring Kafka, you create a consumer with the `@KafkaListener` annotation:

```java
@KafkaListener(topics = "order-created", groupId = "order-group")
public void consume(OrderEvent event) {
    // process the event
}
```

What Spring does at startup:
1. Sees the `@KafkaListener` annotation
2. Creates a Kafka consumer that connects to the broker (localhost:9094)
3. Subscribes it to the topic `order-created` in group `order-group`
4. Starts a **background thread** that continuously polls the broker for new messages
5. When a message arrives — deserializes JSON into `OrderEvent` and calls the method

This application has 4 consumers:

| Consumer | Listens to | Does what | Produces to |
|---|---|---|---|
| `OrderConsumer` | `order-created` | Validates order (1.5s) | `payment-pending` |
| `PaymentConsumer` | `payment-pending` | Simulates payment (2s, 80% success) | `payment-success` or `payment-failed` |
| `ShippingConsumer` | `payment-success` | Prepares shipping (2.5s) | `order-shipped` |
| `NotificationConsumer` | All 5 topics | Pushes SSE to frontend | — |

---

## Consumer Group

A consumer group is a **team of consumers** that share the work of reading from a topic.

Key rules:
- Within the same group, **each partition is read by exactly one consumer** — work is divided
- Different groups read **independently** — each group gets all messages

```
Same group (work is split):
  Consumer A (order-group) → partition 0
  Consumer B (order-group) → partition 1
  Consumer C (order-group) → partition 2

Different groups (each gets everything):
  OrderConsumer    (order-group)        → reads order-created
  NotificationConsumer (notification-group) → also reads order-created
```

This is why `NotificationConsumer` and `OrderConsumer` can both listen to `order-created` without interfering — they have different group IDs.

If a consumer in a group dies, Kafka performs a **rebalance** — redistributes partitions among the remaining consumers:

```
BEFORE:
  Consumer 1 → partition 0
  Consumer 2 → partition 1    ← dies
  Consumer 3 → partition 2

AFTER (automatic rebalance):
  Consumer 1 → partition 0, partition 1   ← picks up the slack
  Consumer 3 → partition 2
```

---

## Offset

An offset is a **sequential number** that identifies a message's position within a partition. It's like an index in an array.

```
partition-0:  [offset 0] [offset 1] [offset 2] [offset 3]
                                       ↑
                              consumer has read up to here
```

Kafka tracks **which offset each consumer group has read up to** (committed offset). This is why:
- If a consumer restarts, it continues from where it left off
- If you stop the backend and start it again, it doesn't reprocess old messages
- Consumer group offsets are stored on the Kafka broker and persist even when all consumers disconnect

You can see offsets in Kafka UI — each consumer group shows current offset vs. end offset (lag).

---

## Message Key

The message key determines **which partition** a message goes to.

```java
kafkaTemplate.send("payment-pending", event.getOrderId(), event);
//                                     ↑ key
```

`hash(key) % numPartitions = partition number`

Why this matters:
- All events for the **same orderId** always go to the **same partition**
- Within a partition, messages are **strictly ordered**
- This guarantees: ORDER_CREATED → PAYMENT_PENDING → PAYMENT_SUCCESS happens in order for each order
- Without a key, messages would be distributed round-robin and ordering is lost

---

## KRaft Mode

Kafka traditionally required **Apache Zookeeper** — a separate service that managed cluster metadata (which broker is alive, where are partitions, who is the leader).

KRaft (Kafka Raft) replaces Zookeeper with Kafka's own built-in consensus protocol. This means:
- One less service to run and manage
- Faster startup and recovery
- Simpler deployment

In our `docker-compose.yml`:
```yaml
KAFKA_PROCESS_ROLES: broker,controller    # this node is both broker and controller
KAFKA_CONTROLLER_QUORUM_VOTERS: 1@kafka:9093   # KRaft voter config
```

In production, you would separate broker and controller roles across different nodes.

---

## Serialization

Kafka stores messages as **bytes**. To send a Java object or read it back, you need serializers and deserializers.

Configuration in `application.yml`:
```yaml
spring.kafka:
  producer:
    key-serializer: org.apache.kafka.common.serialization.StringSerializer
    value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
  consumer:
    key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
    value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
```

Flow:
```
OrderEvent (Java object)
    ↓ JsonSerializer
{"orderId":"abc","status":"CREATED",...}  (JSON bytes)
    ↓ Kafka stores on disk
    ↓ JsonDeserializer
OrderEvent (Java object again)
```

---

## How the Pipeline Works

There is **no central orchestrator**. The chain is driven by each consumer producing to the next topic:

```
POST /api/orders
    ↓
OrderService.createOrder()
    ↓ sends to "order-created"
OrderConsumer picks it up (listening via @KafkaListener)
    ↓ waits 1.5s (simulates validation)
    ↓ sends to "payment-pending"
PaymentConsumer picks it up
    ↓ waits 2s (simulates payment)
    ↓ sends to "payment-success" (80%) or "payment-failed" (20%)
ShippingConsumer picks it up (only payment-success)
    ↓ waits 2.5s (simulates shipping)
    ↓ sends to "order-shipped"
```

Meanwhile, `NotificationConsumer` listens to ALL 5 topics and pushes every event to the frontend via SSE.

The chain is **implicit** — defined by the fact that each consumer sends to the topic that the next consumer listens to. No consumer knows about any other consumer. This is the core benefit of event-driven architecture — components are fully **decoupled**.

---

## SSE vs WebSocket

This application uses **SSE (Server-Sent Events)** instead of WebSocket because the communication is **one-directional** — server pushes updates to the client.

| | SSE | WebSocket |
|---|---|---|
| Direction | Server → Client only | Bidirectional |
| Protocol | Standard HTTP | Separate WS protocol |
| Auto reconnect | Built into browser | Must implement manually |
| Implementation | `SseEmitter` — 5 lines of code | Requires starter, config, handler |
| Proxy/firewall | Always works (HTTP) | Sometimes blocked |

**WebSocket makes sense** when you need bidirectional communication — chat, multiplayer games, collaborative editing. For one-way push (notifications, live feeds, pipeline tracking), SSE is simpler and sufficient.

---

## Scaling — What Happens with Multiple Instances

### Multiple backend instances

If you run 3 instances of the backend, each `PaymentConsumer` (same `groupId = "payment-group"`) gets assigned different partitions:

```
Backend 1 — PaymentConsumer → payment-pending-0
Backend 2 — PaymentConsumer → payment-pending-1
Backend 3 — PaymentConsumer → payment-pending-2
```

Three orders process **simultaneously** instead of sequentially.

Important limits:
- 3 partitions, 3 consumers → each gets 1 partition (ideal)
- 3 partitions, 2 consumers → one gets 2, other gets 1
- 3 partitions, 5 consumers → 3 work, **2 sit idle** (no partitions left)

### The SSE problem with multiple instances

User connects SSE to Backend 1. `NotificationConsumer` on Backend 2 receives the message. Backend 2 doesn't have the user's SSE connection — the update is lost.

Solutions: Redis Pub/Sub, sticky sessions, or a shared message bus between instances.

---

## Where Would Redis Fit In?

Redis complements Kafka for three use cases:

| Use Case | Why Redis | Why not Kafka |
|---|---|---|
| **Cache** — store latest order status for instant reads on page refresh | Sub-millisecond reads, TTL expiry | Kafka is append-only log, not a key-value store |
| **Pub/Sub across instances** — broadcast SSE events to all backends | All instances subscribe and receive | Kafka consumer groups split work, not broadcast to all instances of same group |
| **Session/presence** — who is online | Shared set, real-time updates | Kafka has no concept of "current state" |

In summary:
- **Kafka** = "what happened" (durable event log, inter-service communication)
- **Redis** = "what's happening now" (cache, sessions, real-time broadcast)
- **Database** = "what we know forever" (permanent data storage)
