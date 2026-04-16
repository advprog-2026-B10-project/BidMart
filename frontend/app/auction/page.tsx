'use client';
import { useEffect, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';

interface Auction {
    id: number;
    listingId: number;
    startingPrice: number;
    reservePrice: number;
    endTime: string;
    status: 'DRAFT' | 'ACTIVE' | 'EXTENDED' | 'CLOSED' | 'WON' | 'UNSOLD';
    bids: { amount: number; buyerId: string; timestamp: string }[];
}

const statusColor: Record<string, string> = {
    DRAFT: 'bg-gray-500',
    ACTIVE: 'bg-green-500',
    EXTENDED: 'bg-yellow-500',
    CLOSED: 'bg-red-500',
    WON: 'bg-blue-500',
    UNSOLD: 'bg-gray-600',
};

export default function AuctionListPage() {
    const [auctions, setAuctions] = useState<Auction[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const router = useRouter();

    const token = typeof window !== 'undefined' ? localStorage.getItem('token') : null;

    useEffect(() => {
        if (!token) {
            router.push('/login');
            return;
        }
        fetchAuctions();
    }, []);

    const fetchAuctions = async () => {
        try {
            const res = await fetch('http://localhost:8080/api/bidding/auctions', {
                headers: { Authorization: `Bearer ${token}` },
            });
            if (!res.ok) throw new Error('Gagal memuat auction');
            const data = await res.json();
            setAuctions(data);
        } catch (err: any) {
            setError(err.message);
        } finally {
            setLoading(false);
        }
    };

    const getHighestBid = (auction: Auction) =>
        auction.bids.length > 0
            ? Math.max(...auction.bids.map((b) => b.amount))
            : auction.startingPrice;

    const formatTime = (endTime: string) => {
        const diff = new Date(endTime).getTime() - Date.now();
        if (diff <= 0) return 'Berakhir';
        const minutes = Math.floor(diff / 60000);
        const hours = Math.floor(minutes / 60);
        if (hours > 0) return `${hours}j ${minutes % 60}m lagi`;
        return `${minutes}m lagi`;
    };

    return (
        <main className="min-h-screen bg-[#0f172a] text-white p-8">
            <div className="max-w-5xl mx-auto">
                <div className="flex items-center justify-between mb-10">
                    <div>
                        <Link href="/" className="text-gray-400 hover:text-white transition text-sm">
                            ← Back to Dashboard
                        </Link>
                        <h1 className="text-3xl font-bold text-[#3b82f6] mt-2">Live Auctions</h1>
                    </div>
                    <Link
                        href="/auction/create"
                        className="bg-blue-600 hover:bg-blue-700 text-white px-5 py-2 rounded-lg font-semibold transition"
                    >
                        + Create Auction
                    </Link>
                </div>

                {loading && (
                    <div className="text-center text-gray-400 py-20">Loading auctions...</div>
                )}

                {error && (
                    <div className="bg-red-500/10 border border-red-500 text-red-400 p-4 rounded-lg mb-6">
                        {error}
                    </div>
                )}

                {!loading && auctions.length === 0 && (
                    <div className="text-center text-gray-500 py-20">
                        Belum ada auction. Jadilah yang pertama!
                    </div>
                )}

                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                    {auctions.map((auction) => (
                        <Link key={auction.id} href={`/auction/${auction.id}`}>
                            <div className="bg-[#1e293b] border border-gray-700 rounded-2xl p-6 hover:border-blue-500 transition cursor-pointer shadow-lg">
                                <div className="flex justify-between items-start mb-4">
                                    <span className="text-gray-400 text-sm">Listing #{auction.listingId}</span>
                                    <span className={`text-xs px-2 py-1 rounded-full text-white font-medium ${statusColor[auction.status]}`}>
                                        {auction.status}
                                    </span>
                                </div>

                                <div className="mb-4">
                                    <p className="text-gray-400 text-xs mb-1">Current Highest Bid</p>
                                    <p className="text-2xl font-bold text-white">
                                        Rp {getHighestBid(auction).toLocaleString('id-ID')}
                                    </p>
                                    <p className="text-gray-500 text-xs mt-1">
                                        Starting: Rp {auction.startingPrice.toLocaleString('id-ID')}
                                    </p>
                                </div>

                                <div className="flex justify-between items-center">
                                    <span className="text-gray-400 text-sm">
                                        {auction.bids.length} bid{auction.bids.length !== 1 ? 's' : ''}
                                    </span>
                                    <span className={`text-sm font-medium ${auction.status === 'EXTENDED' ? 'text-yellow-400' : 'text-gray-300'}`}>
                                        ⏱ {formatTime(auction.endTime)}
                                    </span>
                                </div>
                            </div>
                        </Link>
                    ))}
                </div>
            </div>
        </main>
    );
}