import http from 'k6/http';
import { check, fail } from 'k6';
import exec from 'k6/execution';

function positiveNumberEnv(name, fallback) {
  const value = Number(__ENV[name]);

  if (Number.isFinite(value) && value > 0) {
    return value;
  }

  return fallback;
}

function textEnv(name, fallback) {
  const value = __ENV[name];

  if (value === undefined || value === null || value.trim() === '') {
    return fallback;
  }

  return value.trim();
}

function optionalPositiveNumberEnv(name) {
  const value = Number(__ENV[name]);

  if (Number.isInteger(value) && value > 0) {
    return value;
  }

  return 0;
}

function issueIdsEnv() {
  const issueIds = textEnv('ISSUE_IDS', '');

  if (issueIds !== '') {
    const parsedIssueIds = issueIds.split(',')
      .map((value) => Number(value.trim()))
      .filter((value) => Number.isInteger(value) && value > 0);
    if (parsedIssueIds.length === 0) {
      throw new Error('ISSUE_IDS must contain at least one positive integer');
    }
    return parsedIssueIds;
  }

  const issueId = optionalPositiveNumberEnv('ISSUE_ID');
  if (issueId > 0) {
    return [issueId];
  }

  const startIssueId = optionalPositiveNumberEnv('START_ISSUE_ID');
  const issueCount = optionalPositiveNumberEnv('ISSUE_COUNT');
  if (startIssueId > 0 && issueCount > 0) {
    return Array.from({ length: issueCount }, (_, index) => startIssueId + index);
  }

  throw new Error('ISSUE_ID, ISSUE_IDS, or START_ISSUE_ID with ISSUE_COUNT must be provided');
}

const baseUrl = textEnv('BASE_URL', 'https://dev-api.letterpicknews.com').replace(/\/+$/, '');
const issueIds = issueIdsEnv();
const csrfPath = textEnv('CSRF_PATH', '/api/v1/csrf');
const anonymousCookieName = textEnv('ANONYMOUS_COOKIE_NAME', 'letterpick_anonymous_id');
const rate = positiveNumberEnv('RATE', 10);
const preAllocatedVUs = positiveNumberEnv('PRE_ALLOCATED_VUS', Math.max(rate * 2, 20));
const maxVUs = positiveNumberEnv('MAX_VUS', Math.max(rate * 4, 40));
const duration = textEnv('DURATION', '60s');
const iterations = optionalPositiveNumberEnv('ITERATIONS');
const vus = positiveNumberEnv('VUS', Math.min(Math.max(issueIds.length, 1), 100));

let csrfToken = '';
let sessionId = '';

export const options = {
  scenarios: {
    public_issue_view_count: iterations > 0
      ? {
          executor: 'shared-iterations',
          iterations,
          vus,
        }
      : {
          executor: 'constant-arrival-rate',
          rate,
          timeUnit: '1s',
          duration,
          preAllocatedVUs,
          maxVUs,
        },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<1000'],
  },
  summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
};

function cookieValue(response, name) {
  const values = response.cookies[name];

  if (!values || values.length === 0) {
    return '';
  }

  return values[0].value;
}

function ensureCsrfToken() {
  if (csrfToken !== '') {
    return;
  }

  const response = http.get(`${baseUrl}${csrfPath}`, {
    tags: {
      endpoint: 'csrf',
    },
  });

  check(response, {
    'csrf status is 204': (res) => res.status === 204,
    'csrf cookie exists': (res) => Boolean(cookieValue(res, 'XSRF-TOKEN')),
  });

  csrfToken = cookieValue(response, 'XSRF-TOKEN');
  sessionId = cookieValue(response, 'JSESSIONID');

  if (csrfToken === '') {
    fail('XSRF-TOKEN cookie was not returned');
  }
}

function anonymousId() {
  return `k6-${__VU}-${__ITER}-${Date.now()}-${Math.random().toString(36).slice(2)}`;
}

function cookieHeader(actorId) {
  const values = [
    `XSRF-TOKEN=${csrfToken}`,
    `${anonymousCookieName}=${actorId}`,
  ];

  if (sessionId !== '') {
    values.push(`JSESSIONID=${sessionId}`);
  }

  return values.join('; ');
}

export default function () {
  ensureCsrfToken();

  const issueId = issueIds[exec.scenario.iterationInTest % issueIds.length];
  const actorId = anonymousId();
  const url = `${baseUrl}/api/v1/newsletter-issues/${issueId}/views`;
  const response = http.post(url, null, {
    headers: {
      'X-XSRF-TOKEN': csrfToken,
      Cookie: cookieHeader(actorId),
    },
    tags: {
      endpoint: 'public_issue_view_count',
      issue_id: String(issueId),
    },
  });

  check(response, {
    'view status is 204': (res) => res.status === 204,
  });
}
