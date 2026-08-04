import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

const baseUrl = __ENV.BASE_URL || 'http://127.0.0.1:8080';
const bearer = __ENV.ACCESS_TOKEN || '';
const errorRate = new Rate('meghaconnect_errors');
const normalApiLatency = new Trend('meghaconnect_normal_api_latency', true);

export const options = {
  scenarios: {
    normal_apis: {
      executor: 'ramping-vus',
      startVUs: 1,
      stages: [
        { duration: __ENV.RAMP_DURATION || '30s', target: Number(__ENV.VUS || 10) },
        { duration: __ENV.TEST_DURATION || '2m', target: Number(__ENV.VUS || 10) },
        { duration: '30s', target: 0 },
      ],
    },
  },
  thresholds: {
    meghaconnect_errors: ['rate<0.01'],
    meghaconnect_normal_api_latency: ['avg<500', 'p(95)<1500', 'p(99)<3000'],
  },
};

const publicPaths = [
  '/api/v1/reference/DISTRICT',
  '/api/v1/reference/DEPARTMENT',
  '/api/v1/schemes',
];
const authenticatedPaths = [
  '/api/v1/appointments?page=0&size=20',
  '/api/v1/users?page=0&size=20',
];

export default function () {
  const path = publicPaths[Math.floor(Math.random() * publicPaths.length)];
  record(http.get(`${baseUrl}${path}`));

  if (bearer) {
    const protectedPath = authenticatedPaths[Math.floor(Math.random() * authenticatedPaths.length)];
    record(http.get(`${baseUrl}${protectedPath}`, {
      headers: { Authorization: `Bearer ${bearer}` },
    }));
  }
  sleep(1);
}

function record(response) {
  normalApiLatency.add(response.timings.duration);
  const ok = check(response, { 'HTTP status below 500': (r) => r.status < 500 });
  errorRate.add(!ok);
}
