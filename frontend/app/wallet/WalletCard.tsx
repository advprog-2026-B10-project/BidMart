"use client";
import { useState, useEffect, useCallback } from 'react';
import { API_URL } from '@/lib/config';

interface BalanceInfo {
    balance: number;
    heldBalance: number;
}

interface WalletCardProps {
    role?: 'seller' | 'buyer';
    userId: string;
}

export default function WalletCard({ role = 'buyer', userId }: WalletCardProps) {

    const [amount, setAmount] = useState('');
    const [balanceInfo, setBalanceInfo] = useState<BalanceInfo | null>(null);
    const [status, setStatus] = useState({ msg: '', isError: false });
    const [loadingBalance, setLoadingBalance] = useState(true);

    const fetchBalance = useCallback(async () => {
        if (!userId) return;
        setLoadingBalance(true);
        try {
            const res = await fetch(`${API_URL}/wallet/balance?userId=${userId}`);
            if (res.ok) {
                const data = await res.json();
                setBalanceInfo(data);
            }
        } catch {
            console.error("Gagal fetch balance");
        } finally {
            setLoadingBalance(false);
        }
    }, [userId]);

    useEffect(() => {
        fetchBalance();
    }, [fetchBalance]);

    const handleTransaction = async (type: 'topup' | 'withdraw') => {
        if (!amount) return;
        const idempotencyKey = crypto.randomUUID();
        try {
            const res = await fetch(
                `${API_URL}/wallet/${type}?userId=${userId}&amount=${amount}`,
                {
                    method: 'POST',
                    headers: { 'X-Idempotency-Key': idempotencyKey }
                }
            );

            if (res.ok) {
                setStatus({ msg: `${type.toUpperCase()} Success!`, isError: false });
                setAmount('');
                fetchBalance();
            } else {
                setStatus({ msg: `${type.toUpperCase()} Failed!`, isError: true });
            }
        } catch (err) {
            setStatus({ msg: "Connection error", isError: true });
        }
    };

    const handleWinAuction = async () => {
        try {
            const res = await fetch(`${API_URL}/wallet/test-event?userId=${userId}`, {
                method: 'POST'
            });
            if (res.ok) {
                setStatus({ msg: "Win Auction Event Published!", isError: false });
                setTimeout(fetchBalance, 1000);
            }
        } catch (err) {
            setStatus({ msg: "Failed to publish event", isError: true });
        }
    };

    const handlePublishEvent = async () => {
        try {
            const res = await fetch(`${API_URL}/wallet/test-event?userId=${userId}`, {
                method: 'POST'
            });
            if (res.ok) {
                setStatus({ msg: "Test Event Published!", isError: false });
            }
        } catch (err) {
            setStatus({ msg: "Failed to publish event", isError: true });
        }
    };

    return (
        <div className="bg-[#1e293b] p-8 rounded-3xl border border-gray-800 shadow-2xl max-w-md mx-auto">
            <div className="flex justify-between items-center mb-6">
                <h2 className="text-2xl font-bold text-white">Digital Wallet</h2>
                <span className={`px-3 py-1 rounded-full text-xs font-bold uppercase ${role === 'seller' ? 'bg-yellow-600 text-white' : 'bg-blue-600 text-white'}`}>
                    {role}
                </span>
            </div>

            <div className="mb-6">
                <input
                    type="text"
                    value={userId}
                    disabled
                    className="w-full bg-[#0f172a] border border-gray-700 rounded-lg p-3 text-gray-400 cursor-not-allowed"
                />
            </div>

            <div className="grid grid-cols-2 gap-4 mb-6">
                <div className="bg-[#0f172a] p-4 rounded-xl border border-gray-800">
                    <p className="text-xs text-gray-400 mb-1">Balance</p>
                    <p className="text-xl font-bold text-green-400">
                        {loadingBalance ? '...' : `Rp ${balanceInfo?.balance?.toLocaleString() || 0}`}
                    </p>
                </div>
                <div className="bg-[#0f172a] p-4 rounded-xl border border-gray-800">
                    <p className="text-xs text-gray-400 mb-1">On Hold</p>
                    <p className="text-xl font-bold text-yellow-500">
                        {loadingBalance ? '...' : `Rp ${balanceInfo?.heldBalance?.toLocaleString() || 0}`}
                    </p>
                </div>
            </div>

            <div className="mb-6">
                <input
                    type="number"
                    placeholder="Amount..."
                    value={amount}
                    onChange={(e) => setAmount(e.target.value)}
                    className="w-full bg-[#0f172a] border border-gray-700 rounded-lg p-4 text-white focus:outline-none focus:border-blue-500 transition-all"
                />
            </div>

            <div className="grid grid-cols-2 gap-4 mb-3">
                <button
                    onClick={() => handleTransaction('topup')}
                    className="bg-blue-600 hover:bg-blue-500 p-3 rounded font-semibold transition"
                >
                    TOPUP
                </button>
                <button
                    onClick={() => handleTransaction('withdraw')}
                    className="bg-red-600 hover:bg-red-500 p-3 rounded font-semibold transition"
                >
                    WITHDRAW
                </button>
            </div>

            {role === 'seller' && (
                <div className="mb-3">
                    <button
                        onClick={handleWinAuction}
                        className="w-full bg-yellow-600 hover:bg-yellow-500 p-3 rounded font-semibold transition"
                    >
                        WIN AUCTION
                    </button>
                </div>
            )}

            <button
                onClick={handlePublishEvent}
                className="w-full bg-purple-600 hover:bg-purple-500 p-3 rounded font-semibold transition"
            >
                TEST EVENT
            </button>

            {status.msg && (
                <div className={`mt-4 p-3 rounded text-sm font-medium ${status.isError ? 'bg-red-500' : 'bg-green-500'} text-white text-center`}>
                    {status.msg}
                </div>
            )}
        </div>
    );
}