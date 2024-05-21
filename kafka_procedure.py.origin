# kafka_producer.py
import sys
from kafka import KafkaProducer
import json
import time

if len(sys.argv) != 3:
    print("Usage: python kafka_producer.py <topic> <message>")
    sys.exit(1)

topic = sys.argv[1]
message = sys.argv[2]

# Configure the producer
producer = KafkaProducer(
    bootstrap_servers='kafka-1.platform.stg.ajaib.int:9092',
    value_serializer=lambda v: json.dumps(v).encode('utf-8')
)

def publish_message(topic, message):
    try:
        producer.send(topic, {'message': message})
        producer.flush()
        print(f"Message published to {topic}: {message}")
    except Exception as e:
        print(f"Failed to publish message: {str(e)}")

if __name__ == "__main__":
    publish_message(topic, message)
    time.sleep(1)
