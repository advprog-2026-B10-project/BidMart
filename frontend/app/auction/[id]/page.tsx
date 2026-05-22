'use client';
import { useEffect, useState } from 'react';
import { useParams, useRouter } from 'next/navigation';
import Link from 'next/link';

interface Bid {
    buyerId: string;
    amount: number;
    timestamp: string;
}

interface Auction {
    id: number;
    listingId: number;
    startingPrice: number;
    reservePrice: number;
    endTime: string;
    status: 'DRAFT' | 'ACTIVE' | 'EXTENDED' | 'CLOSED' | 'WON' | 'UNSOLD';
    bids: Bid[];
}

const statusColor: Record<string, string> = {
    DRAFT: 'bg-gray-500',
    ACTIVE: 'bg-green-500',
    EXTENDED: 'bg-yellow-500',
    CLOSED: 'bg-red-500',
    WON: 'bg-blue-500',
    UNSOLD: 'bg-gray-600',
};

export default function AuctionDetailPage() {
    const { id } = useParams();
    const router = useRouter();
    const [auction, setAuction] = useState<Auction | null>(null);
    const [loading, setLoading] = useState(true);
    const [bidAmount, setBidAmount] = useState('');
    const [bidStatus, setBidStatus] = useState({ msg: '', isError: false });
    const [bidLoading, setBidLoading] = useState(false);
    const [timeLeft, setTimeLeft] = useState('');

    const token = typeof window !== 'undefined' ? localStorage.getItem('token') : null;
    const email = typeof window !== 'undefined' ? localStorage.getItem('email') : null;

    useEffect(() => {
        if (!token) { router.push('/login'); return; }
        fetchAuction();
    }, []);

    useEffect(() => {
        if (!auction) return;
        const interval = setInterval(() => {
            const diff = new Date(auction.endTime).getTime() - Date.now();
            if (diff <= 0) { setTimeLeft('Auction ended'); clearInterval(interval); return; }
            const hours = Math.floor(diff / 3600000);
            const minutes = Math.floor((diff % 3600000) / 60000);
            const seconds = Math.floor((diff % 60000) / 1000);
            setTimeLeft(`${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`);
        }, 1000);
        return () => clearInterval(interval);
    }, [auction]);

    const fetchAuction = async () => {
        try {
            const res = await fetch(`http://localhost:8080/api/bidding/auctions/${id}`, {
                headers: { Authorization: `Bearer ${token}` },
            });
            if (!res.ok) throw new Error('Auction tidak ditemukan');
            const data = await res.json();
            setAuction(data);
        } catch (err: any) {
            setBidStatus({ msg: err.message, isError: true });
        } finally {
            setLoading(false);
        }
    };

    const handlePlaceBid = async () => {
        if (!bidAmount || !email || !auction) return;
        setBidLoading(true);
        setBidStatus({ msg: '', isError: false });
        try {
            const res = await fetch(
                `http://localhost:8080/api/bidding/bid?userId=${email}&auctionId=${auction.id}&amount=${bidAmount}`,
                {
                    method: 'POST',
                    headers: { Authorization: `Bearer ${token}` },
                }
            );
            const text = await res.text();
            setBidStatus({ msg: text, isError: !res.ok });
            if (res.ok) {
                setBidAmount('');
                fetchAuction(); // refresh data
            }
        } catch {
            setBidStatus({ msg: 'Gagal terhubung ke server', isError: true });
        } finally {
            setBidLoading(false);
        }
    };

    const getHighestBid = () =>
        auction && auction.bids.length > 0
            ? Math.max(...auction.bids.map((b) => b.amount))
            : auction?.startingPrice ?? 0;

    const isActive = auction?.status === 'ACTIVE' || auction?.status === 'EXTENDED';

    if (loading) return (
        <main className="min-h-screen bg-[#0f172a] flex items-center justify-center">
            <p className="text-gray-400">Loading auction...</p>
        </main>
    );

    if (!auction) return (
        <main className="min-h-screen bg-[#0f172a] flex items-center justify-center">
            <p className="text-red-400">Auction tidak ditemukan.</p>
        </main>
    );

    return (
        <main className="min-h-screen bg-[#0f172a] text-white p-8">
            <div className="max-w-4xl mx-auto">
                <div className="mb-8">
                    <Link href="/auction" className="text-gray-400 hover:text-white transition text-sm">
                        ← Back to Auctions
                    </Link>
                </div>

                <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                    {/* Info Panel */}
                    <div className="lg:col-span-2 space-y-6">
                        <div className="bg-[#1e293b] border border-gray-700 rounded-2xl p-6">
                            <div className="flex justify-between items-start mb-6">
                                <div>
                                    <p className="text-gray-400 text-sm">Listing #{auction.listingId}</p>
                                    <h1 className="text-2xl font-bold text-white mt-1">Auction #{auction.id}</h1>
                                </div>
                                <span className={`text-sm px-3 py-1 rounded-full text-white font-medium ${statusColor[auction.status]}`}>
                                    {auction.status}
                                </span>
                            </div>

                            <div className="grid grid-cols-2 gap-4 mb-6">
                                <div className="bg-[#0f172a] rounded-xl p-4">
                                    <p className="text-gray-400 text-xs mb-1">Current Highest Bid</p>
                                    <p className="text-2xl font-bold text-blue-400">
                                        Rp {getHighestBid().toLocaleString('id-ID')}
                                    </p>
                                </div>
                                <div className="bg-[#0f172a] rounded-xl p-4">
                                    <p className="text-gray-400 text-xs mb-1">Starting Price</p>
                                    <p className="text-2xl font-bold text-gray-300">
                                        Rp {auction.startingPrice.toLocaleString('id-ID')}
                                    </p>
                                </div>
                            </div>

                            {/* Timer */}
                            <div className={`rounded-xl p-4 text-center ${auction.status === 'EXTENDED' ? 'bg-yellow-500/10 border border-yellow-500' : 'bg-[#0f172a]'}`}>
                                <p className="text-gray-400 text-xs mb-1">
                                    {auction.status === 'EXTENDED' ? '⚡ Extended — Time Remaining' : '⏱ Time Remaining'}
                                </p>
                                <p className={`text-3xl font-mono font-bold ${auction.status === 'EXTENDED' ? 'text-yellow-400' : 'text-white'}`}>
                                    {timeLeft || 'Calculating...'}
                                </p>
                            </div>
                        </div>

                        {/* Bid History */}
                        <div className="bg-[#1e293b] border border-gray-700 rounded-2xl p-6">
                            <h2 className="text-lg font-semibold mb-4">Bid History ({auction.bids.length})</h2>
                            {auction.bids.length === 0 ? (
                                <p className="text-gray-500 text-center py-6">Belum ada bid. Jadilah yang pertama!</p>
                            ) : (
                                <div className="space-y-3 max-h-60 overflow-y-auto">
                                    {[...auction.bids]
                                        .sort((a, b) => b.amount - a.amount)
                                        .map((bid, i) => (
                                            <div key={i} className={`flex justify-between items-center p-3 rounded-lg ${i === 0 ? 'bg-blue-500/10 border border-blue-500/30' : 'bg-[#0f172a]'}`}>
                                                <div>
                                                    {i === 0 && <span className="text-xs text-blue-400 font-medium">👑 Highest</span>}
                                                    <p className="text-sm text-gray-300">{bid.buyerId}</p>
                                                    <p className="text-xs text-gray-500">
                                                        {new Date(bid.timestamp).toLocaleString('id-ID')}
                                                    </p>
                                                </div>
                                                <p className="font-bold text-white">
                                                    Rp {bid.amount.toLocaleString('id-ID')}
                                                </p>
                                            </div>
                                        ))}
                                </div>
                            )}
                        </div>
                    </div>

                    {/* Place Bid Panel */}
                    <div className="space-y-4">
                        <div className="bg-[#1e293b] border border-gray-700 rounded-2xl p-6">
                            <h2 className="text-lg font-semibold mb-4">Place Your Bid</h2>

                            {!isActive ? (
                                <div className="text-center py-6 text-gray-500">
                                    <p className="text-4xl mb-2">🔒</p>
                                    <p>Auction is not active</p>
                                </div>
                            ) : (
                                <>
                                    <div className="bg-[#0f172a] rounded-xl p-3 mb-4">
                                        <p className="text-gray-400 text-xs">Minimum bid</p>
                                        <p className="text-white font-semibold">
                                            Rp {(getHighestBid() + 1).toLocaleString('id-ID')}
                                        </p>
                                    </div>

                                    <input
                                        type="number"
                                        placeholder="Enter bid amount..."
                                        value={bidAmount}
                                        onChange={(e) => setBidAmount(e.target.value)}
                                        className="w-full bg-[#0f172a] border border-gray-600 rounded-lg p-3 text-white outline-none focus:ring-2 focus:ring-blue-500 mb-4"
                                    />

                                    <button
                                        onClick={handlePlaceBid}
                                        disabled={bidLoading || !bidAmount}
                                        className="w-full py-3 bg-blue-600 hover:bg-blue-700 disabled:opacity-50 text-white font-semibold rounded-lg transition"
                                    >
                                        {bidLoading ? 'Placing bid...' : '🔨 Place Bid'}
                                    </button>

                                    {bidStatus.msg && (
                                        <div className={`mt-4 p-3 rounded-lg text-sm ${bidStatus.isError ? 'bg-red-500/10 border border-red-500 text-red-400' : 'bg-green-500/10 border border-green-500 text-green-400'}`}>
                                            {bidStatus.msg}
                                        </div>
                                    )}
                                </>
                            )}
                        </div>

                        {/* Auction Info */}
                        <div className="bg-[#1e293b] border border-gray-700 rounded-2xl p-6 text-sm text-gray-400 space-y-2">
                            <p>📋 Total bids: <span className="text-white">{auction.bids.length}</span></p>
                            <p>🏁 Ends: <span className="text-white">{new Date(auction.endTime).toLocaleString('id-ID')}</span></p>
                        </div>
                    </div>
                </div>
            </div>
        </main>
    );
}