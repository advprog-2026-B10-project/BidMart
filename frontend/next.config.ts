import type { NextConfig } from "next";

const nextConfig: NextConfig = {
    async rewrites() {
        return [
            {
                source: '/api/:path*',
                destination: 'http://18.215.170.237/api/:path*'
            }
        ]
    }
};

export default nextConfig;