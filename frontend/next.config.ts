import type { NextConfig } from "next";

const nextConfig: NextConfig = {
    async rewrites() {
        return [
            {
                source: '/api/:path*',
                destination: 'http://13.221.75.220:8080/api/:path*'
            }
        ]
    }
};

export default nextConfig;