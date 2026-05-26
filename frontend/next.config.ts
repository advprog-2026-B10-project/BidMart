import type { NextConfig } from "next";

const nextConfig: NextConfig = {
    async rewrites() {
        return [
            {
                source: '/api/:path*',
                destination: 'http://18.215.170.237:8080/api/:path*'
            }
        ]
    }
};

export default nextConfig;