#!/usr/bin/env python3
import argparse
from concurrent.futures import FIRST_COMPLETED, ThreadPoolExecutor, wait
import datetime as dt
import json
import math
import os
import sys
import time
import uuid


DEFAULT_EVENT_TYPE = "ISSUE_VIEW_COUNT_UPDATED"
DEFAULT_SCHEMA_VERSION = 1
DEFAULT_SOURCE = "letterpick"
MAX_SQS_BATCH_SIZE = 10
DEFAULT_CONCURRENCY = 16
DEFAULT_RATE_THRESHOLD = 0.95


def env(name, default=None):
    return os.environ.get(name, default)


def parse_duration(value):
    text = str(value).strip().lower()
    if not text:
        raise ValueError("duration must not be blank")

    if text.endswith("ms"):
        return float(text[:-2]) / 1000
    if text.endswith("s"):
        return float(text[:-1])
    if text.endswith("m"):
        return float(text[:-1]) * 60

    return float(text)


def positive_int(value, name):
    number = int(value)
    if number < 1:
        raise ValueError(f"{name} must be a positive integer")
    return number


def positive_float(value, name):
    number = float(value)
    if number <= 0:
        raise ValueError(f"{name} must be positive")
    return number


def positive_ratio(value, name):
    number = positive_float(value, name)
    if number > 1:
        raise ValueError(f"{name} must be <= 1")
    return number


def utc_now():
    return dt.datetime.now(dt.timezone.utc).isoformat(timespec="microseconds").replace("+00:00", "Z")


def build_message(args, sequence):
    issue_offset = sequence % args.issue_count
    issue_id = args.start_issue_id + issue_offset
    view_count = args.start_view_count + (sequence // args.issue_count) + 1
    occurred_at = utc_now()

    return {
        "eventId": str(uuid.uuid4()),
        "eventType": DEFAULT_EVENT_TYPE,
        "schemaVersion": DEFAULT_SCHEMA_VERSION,
        "source": args.source,
        "occurredAt": occurred_at,
        "traceId": f"{args.trace_prefix}-{sequence + 1}",
        "payload": {
            "issueId": issue_id,
            "viewCount": view_count,
        },
    }


def build_batch(args, start_sequence, count):
    entries = []
    for offset in range(count):
        sequence = start_sequence + offset
        body = json.dumps(build_message(args, sequence), separators=(",", ":"))
        entries.append({
            "Id": f"msg{sequence + 1}",
            "MessageBody": body,
        })
    return entries


def create_sqs_client(args):
    try:
        import boto3
        from botocore.config import Config
    except ImportError as error:
        raise RuntimeError("boto3 is required. Install python3-boto3 on the k6 runner.") from error

    client_args = {
        "config": Config(max_pool_connections=max(args.concurrency, 10)),
    }
    if args.region:
        client_args["region_name"] = args.region

    return boto3.client("sqs", **client_args)


def send_batch(client, queue_url, entries):
    response = client.send_message_batch(
        QueueUrl=queue_url,
        Entries=entries,
    )

    failed = response.get("Failed", [])
    if failed:
        raise RuntimeError(f"SQS batch send failed: {json.dumps(failed, ensure_ascii=False)}")

    successful = response.get("Successful", [])
    return len(successful)


def collect_completed(futures, wait_for):
    if not futures:
        return futures, 0

    done, remaining = wait(futures, return_when=wait_for)
    completed = 0
    for future in done:
        completed += future.result()

    return remaining, completed


def parse_args():
    parser = argparse.ArgumentParser(
        description="Send ISSUE_VIEW_COUNT_UPDATED events to the dev trending score SQS queue."
    )
    parser.add_argument("--queue-url", default=env("TRENDING_SCORE_EVENTS_QUEUE_URL"))
    parser.add_argument("--region", default=env("AWS_REGION", env("AWS_DEFAULT_REGION")))
    parser.add_argument("--rate", default=env("RATE", "10"))
    parser.add_argument("--duration", default=env("DURATION", "10s"))
    parser.add_argument("--messages", default=env("MESSAGE_COUNT"))
    parser.add_argument("--concurrency", default=env("CONCURRENCY", str(DEFAULT_CONCURRENCY)))
    parser.add_argument("--rate-threshold", default=env("RATE_THRESHOLD", str(DEFAULT_RATE_THRESHOLD)))
    parser.add_argument("--issue-count", default=env("ISSUE_COUNT", "1"))
    parser.add_argument("--start-issue-id", default=env("START_ISSUE_ID", "1"))
    parser.add_argument("--start-view-count", default=env("START_VIEW_COUNT", "0"))
    parser.add_argument("--trace-prefix", default=env("TRACE_PREFIX", "dev-score-trace"))
    parser.add_argument("--source", default=env("EVENT_SOURCE", DEFAULT_SOURCE))
    parser.add_argument("--dry-run", action="store_true", default=env("DRY_RUN", "false").lower() == "true")

    args = parser.parse_args()
    args.rate = positive_float(args.rate, "rate")
    args.duration_seconds = parse_duration(args.duration)
    args.concurrency = positive_int(args.concurrency, "concurrency")
    args.rate_threshold = positive_ratio(args.rate_threshold, "rate-threshold")
    args.issue_count = positive_int(args.issue_count, "issue-count")
    args.start_issue_id = positive_int(args.start_issue_id, "start-issue-id")
    args.start_view_count = int(args.start_view_count)
    if args.messages is not None:
        args.messages = positive_int(args.messages, "messages")

    if args.messages is None:
        args.messages = max(1, math.floor(args.rate * args.duration_seconds))

    if args.start_view_count < 0:
        raise ValueError("start-view-count must not be negative")

    if not args.queue_url and not args.dry_run:
        raise ValueError("queue url is required. Set TRENDING_SCORE_EVENTS_QUEUE_URL or pass --queue-url.")

    return args


def main():
    try:
        args = parse_args()
    except Exception as error:
        print(f"Invalid arguments: {error}", file=sys.stderr)
        return 2

    print("Sending trending score events")
    print(f"  queue_url: {args.queue_url or '(dry-run)'}")
    print(f"  region: {args.region or '(default)'}")
    print(f"  rate: {args.rate}/s")
    print(f"  duration: {args.duration_seconds}s")
    print(f"  messages: {args.messages}")
    print(f"  concurrency: {args.concurrency}")
    print(f"  rate_threshold: {args.rate_threshold * 100:.1f}%")
    print(f"  issue range: {args.start_issue_id}..{args.start_issue_id + args.issue_count - 1}")
    print(f"  dry_run: {args.dry_run}")

    if args.dry_run:
        entries = build_batch(args, 0, min(3, args.messages))
        print(json.dumps([json.loads(entry["MessageBody"]) for entry in entries], indent=2))
        print(f"Done. sampled={len(entries)}, target_messages={args.messages}")
        return 0

    try:
        client = create_sqs_client(args)
    except Exception as error:
        print(f"Failed to create SQS client: {error}", file=sys.stderr)
        return 2

    scheduled = 0
    completed = 0
    futures = set()
    started_at = time.monotonic()

    try:
        with ThreadPoolExecutor(max_workers=args.concurrency) as executor:
            while scheduled < args.messages:
                while len(futures) >= args.concurrency * 2:
                    futures, completed_count = collect_completed(futures, FIRST_COMPLETED)
                    completed += completed_count

                next_send_at = started_at + (scheduled / args.rate)
                sleep_seconds = next_send_at - time.monotonic()
                if sleep_seconds > 0:
                    time.sleep(sleep_seconds)

                batch_size = min(MAX_SQS_BATCH_SIZE, args.messages - scheduled)
                entries = build_batch(args, scheduled, batch_size)
                futures.add(executor.submit(send_batch, client, args.queue_url, entries))
                scheduled += batch_size

            futures, completed_count = collect_completed(futures, FIRST_COMPLETED)
            completed += completed_count
            while futures:
                futures, completed_count = collect_completed(futures, FIRST_COMPLETED)
                completed += completed_count
    except Exception as error:
        elapsed = time.monotonic() - started_at
        actual_rate = completed / elapsed if elapsed > 0 else 0
        print(
            f"Failed. scheduled={scheduled}, completed={completed}, elapsed={elapsed:.3f}s, "
            f"actual_rate={actual_rate:.2f}/s",
            file=sys.stderr,
        )
        print(f"Error: {error}", file=sys.stderr)
        return 1

    elapsed = time.monotonic() - started_at
    actual_rate = completed / elapsed if elapsed > 0 else 0
    print(f"Done. scheduled={scheduled}, completed={completed}, elapsed={elapsed:.3f}s, actual_rate={actual_rate:.2f}/s")

    if completed != args.messages:
        print(f"ERROR: completed message count mismatch. expected={args.messages}, actual={completed}", file=sys.stderr)
        return 3

    minimum_rate = args.rate * args.rate_threshold
    if actual_rate < minimum_rate:
        print(
            f"ERROR: target rate was not reached. target={args.rate:.2f}/s, "
            f"minimum={minimum_rate:.2f}/s, actual={actual_rate:.2f}/s",
            file=sys.stderr,
        )
        return 4

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
