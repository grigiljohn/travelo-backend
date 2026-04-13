import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

// Custom metrics
const errorRate = new Rate('errors');
const postCreationTime = new Trend('post_creation_time');

// Test configuration
export const options = {
    stages: [
        { duration: '30s', target: 10 },   // Ramp up to 10 users
        { duration: '1m', target: 10 },    // Stay at 10 users
        { duration: '30s', target: 50 },   // Ramp up to 50 users
        { duration: '2m', target: 50 },    // Stay at 50 users
        { duration: '30s', target: 0 },    // Ramp down
    ],
    thresholds: {
        http_req_duration: ['p(95)<500', 'p(99)<1000'], // 95% of requests < 500ms, 99% < 1s
        http_req_failed: ['rate<0.01'],                  // Error rate < 1%
        errors: ['rate<0.01'],
    },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8083';
const API_TOKEN = __ENV.API_TOKEN || '';

export default function () {
    // Test post creation
    const payload = JSON.stringify({
        caption: 'Load test post',
        authorId: '00000000-0000-0000-0000-000000000001',
        postType: 'POST',
        mood: 'HAPPY'
    });

    const params = {
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${API_TOKEN}`,
            'Idempotency-Key': `test-${__VU}-${__ITER}`,
        },
    };

    const startTime = Date.now();
    const response = http.post(`${BASE_URL}/api/v1/posts`, payload, params);
    const duration = Date.now() - startTime;

    const success = check(response, {
        'status is 201': (r) => r.status === 201,
        'response has id': (r) => JSON.parse(r.body).data?.id !== undefined,
    });

    errorRate.add(!success);
    postCreationTime.add(duration);

    sleep(1);
}

export function handleSummary(data) {
    return {
        'stdout': textSummary(data, { indent: ' ', enableColors: true }),
        'results.json': JSON.stringify(data),
    };
}

