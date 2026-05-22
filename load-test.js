import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    vus: 200,        // 200 virtual users
    duration: '30s', // selama 30 detik
};

const BASE_URL = 'http://localhost:8080';
const TOKEN = "eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiJqb2huYnV5ZXJAZ21haWwuY29tIiwicm9sZXMiOlsiUk9MRV9CVVlFUiJdLCJqdGkiOiJjMzg5Y2E4OC03MGFlLTQxNWQtODkxOS0xMTZhYmY1NWMxZWIiLCJpYXQiOjE3Nzk0NDAzMTIsImV4cCI6MTc3OTQ0MTIxMn0.USBbUP2N6eiyllUJg70eWtreCrqbmYquiYoQJ1-NcksGtBsvaoUiWl9WjwXb9y3n"

export default function () {
    const amount = Math.floor(Math.random() * 100000) + 60000; // random 60000-160000

    const res = http.post(
        `${BASE_URL}/api/bidding/bid?userId=johnbuyer@gmail.com&auctionId=1&amount=${amount}`,
        null,
        {
            headers: {
                Authorization: `Bearer ${TOKEN}`,
            },
        }
    );

    check(res, {
        'status is 200': (r) => r.status === 200,
        'bid response ok': (r) => r.body.includes('Bid berhasil') || r.body.includes('lebih tinggi') || r.body.includes('sibuk'),
    });

    sleep(0.1);
}