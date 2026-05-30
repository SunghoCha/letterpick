import http from 'k6/http';
import { check } from 'k6';

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

const baseUrl = textEnv('BASE_URL', 'https://dev-api.letterpicknews.com').replace(/\/+$/, '');
const path = textEnv('API_PATH', '/api/v1/newsletter-issues');
const keyword = textEnv('KEYWORD', '전환');
const category = (__ENV.CATEGORY || '').trim();
const page = positiveNumberEnv('PAGE', 0);
const size = positiveNumberEnv('SIZE', 20);
const rate = positiveNumberEnv('RATE', 5);
const preAllocatedVUs = positiveNumberEnv('PRE_ALLOCATED_VUS', Math.max(rate * 2, 10));
const maxVUs = positiveNumberEnv('MAX_VUS', Math.max(rate * 4, 20));
const duration = textEnv('DURATION', '60s');
const searchStrategy = textEnv('SEARCH_STRATEGY', 'unknown');

export const options = {
  scenarios: {
    public_feed_search: {
      executor: 'constant-arrival-rate',
      rate,
      timeUnit: '1s',
      duration,
      preAllocatedVUs,
      maxVUs,
    },
  },
  summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
};

function queryString() {
  const params = [
    ['page', page],
    ['size', size],
    ['keyword', keyword],
  ];

  if (category !== '') {
    params.push(['category', category]);
  }

  return params
    .map(([name, value]) => `${encodeURIComponent(name)}=${encodeURIComponent(String(value))}`)
    .join('&');
}

export default function () {
  const url = `${baseUrl}${path}?${queryString()}`;

  const response = http.get(url, {
    tags: {
      endpoint: 'public_feed_search',
      strategy: searchStrategy,
      keyword,
    },
  });

  check(response, {
    'status is 200': (res) => res.status === 200,
    'response body exists': (res) => Boolean(res.body && res.body.length > 0),
  });
}
