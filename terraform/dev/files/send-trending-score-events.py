#!/usr/bin/env python3
import argparse
import datetime as dt
import json
import math
import os
import subprocess
import sys
import tempfile
import time
import uuid


DEFAULT_EVENT_TYPE = "ISSUE_VIEW_COUNT_UPDATED"
DEFAULT_SCHEMA_VERSION = 1
DEFAULT_SOURCE = "letterpick"
MAX_SQS_BATCH_SIZE = 10


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


def send_batch(queue_url, entries):
    with tempfile.NamedTemporaryFile("w", encoding="utf-8", suffix=".json", delete=False) as file:
        json.dump(entries, file, separators=(",", ":"))
        file_path = file.name

    try:
        result = subprocess.run(
            [
                "aws",
                "sqs",
                "send-message-batch",
                "--queue-url",
                queue_url,
                "--entries",
                f"file://{file_path}",
            ],
            check=True,
            capture_output=True,
            text=True,
        )
    finally:
        try:
            os.remove(file_path)
        except FileNotFoundError:
            pass

    payload = json.loads(result.stdout)
    failed = payload.get("Failed", [])
    if failed:
        raise RuntimeError(f"SQS batch send failed: {json.dumps(failed, ensure_ascii=False)}")


def parse_args():
    parser = argparse.ArgumentParser(
        description="Send ISSUE_VIEW_COUNT_UPDATED events to the dev trending score SQS queue."
    )
    parser.add_argument("--queue-url", default=env("TRENDING_SCORE_EVENTS_QUEUE_URL"))
    parser.add_argument("--rate", default=env("RATE", "10"))
    parser.add_argument("--duration", default=env("DURATION", "10s"))
    parser.add_argument("--messages", default=env("MESSAGE_COUNT"))
    parser.add_argument("--issue-count", default=env("ISSUE_COUNT", "1"))
    parser.add_argument("--start-issue-id", default=env("START_ISSUE_ID", "1"))
    parser.add_argument("--start-view-count", default=env("START_VIEW_COUNT", "0"))
    parser.add_argument("--trace-prefix", default=env("TRACE_PREFIX", "dev-score-trace"))
    parser.add_argument("--source", default=env("EVENT_SOURCE", DEFAULT_SOURCE))
    parser.add_argument("--dry-run", action="store_true", default=env("DRY_RUN", "false").lower() == "true")

    args = parser.parse_args()
    args.rate = positive_float(args.rate, "rate")
    args.duration_seconds = parse_duration(args.duration)
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
    print(f"  rate: {args.rate}/s")
    print(f"  duration: {args.duration_seconds}s")
    print(f"  messages: {args.messages}")
    print(f"  issue range: {args.start_issue_id}..{args.start_issue_id + args.issue_count - 1}")
    print(f"  dry_run: {args.dry_run}")

    sent = 0
    started_at = time.monotonic()

    while sent < args.messages:
        batch_size = min(MAX_SQS_BATCH_SIZE, args.messages - sent)
        entries = build_batch(args, sent, batch_size)

        if args.dry_run:
            if sent == 0:
                print(json.dumps([json.loads(entry["MessageBody"]) for entry in entries[:3]], indent=2))
        else:
            send_batch(args.queue_url, entries)

        sent += batch_size
        next_send_at = started_at + (sent / args.rate)
        sleep_seconds = next_send_at - time.monotonic()
        if sleep_seconds > 0:
            time.sleep(sleep_seconds)

    elapsed = time.monotonic() - started_at
    print(f"Done. sent={sent}, elapsed={elapsed:.3f}s, actual_rate={sent / elapsed:.2f}/s")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
