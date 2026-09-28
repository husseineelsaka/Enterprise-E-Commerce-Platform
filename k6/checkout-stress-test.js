import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '20s', target: 50 },
    { duration: '20s', target: 150 }, // well beyond S5's Bulkhead max-concurrent-calls: 10
    { duration: '20s', target: 0 },
  ],
  thresholds: {
    http_req_failed: ['rate<0.5'], // deliberately loose — we EXPECT failures
  },
};

export default function () {
  const payload = JSON.stringify({
    productId: "PROD-001",
    quantity: 3,
    amount: 100.00,
    customerId: "CUST-1"
  });

  const params = {
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${__ENV.TEST_JWT}`
    }
  };

  const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';
  const res = http.post(`${baseUrl}/api/v1/orders`, payload, params);
  
  check(res, {
    'status 200/202': (r) => [200, 202].includes(r.status)
  });
  
  sleep(1);
}
