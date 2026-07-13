import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '5s', target: 5 },
    { duration: '10s', target: 20 },
    { duration: '5s', target: 0 },
  ],
  thresholds: {
    http_req_duration: ['p(95)<3000'],
  },
};

const baseUrl = __ENV.BASE_URL || 'http://localhost:8200';
const headers = { 'X-Token': __ENV.TOKEN || 'API_KEY_PLACEHOLDER' };

export default function () {
  const res = http.get(`${baseUrl}/api/products/4001`, { headers });
  check(res, {
    'status is 200 or 429': (r) => r.status === 200 || r.status === 429,
    'gateway response': (r) => r.body.length > 0,
  });
  sleep(1);
}
