"use client";
import { useState } from 'react';

export default function WalletCard() {
    const [userId, setUserId] = useState('johnseller@gmail.com');
    const [amount, setAmount] = useState('');
    const [status, setStatus] = useState({ msg: '', isError: false });

    const handleTransaction = async (type: 'topup' | 'withdraw') => {
        if (!amount) return;
        try {
            const res = await fetch(`http://localhost:8080/wallet/${type}?userId=${userId}&amount=${amount}`, {
                method: 'POST'
            });
            const text = await res.text();
            setStatus({ msg: text, isError: !res.ok });
        } catch (err) {
            setStatus({ msg: "Connection failed", isError: true });
        }
    };

    // Simulasi MENANG AUCTION
    const handleWinAuction = async () => {
        if (!amount) return;
        try {
            const res = await fetch(
                `http://localhost:8080/wallet/win?userId=${userId}&amount=${amount}`,
                { method: 'POST' }
            );
            const text = await res.text();
            setStatus({ msg: "WIN EVENT: " + text, isError: !res.ok });
        } catch (err) {
            setStatus({ msg: "Win simulation failed", isError: true });
        }
    };

    //      Test event publish manual
    const handlePublishEvent = async () => {
        try {
            const res = await fetch(
                `http://localhost:8080/wallet/test-event?userId=${userId}`,
                { method: 'POST' }
            );
            const text = await res.text();
            setStatus({ msg: "EVENT: " + text, isError: !res.ok });
        } catch (err) {
            setStatus({ msg: "Event publish failed", isError: true });
        }
    };

    return (
        <div className="bg-[#1e293b] p-8 rounded-2xl border border-gray-800 shadow-2xl max-w-lg mx-auto">
            <h2 className="text-xl font-bold text-white mb-6">Digital Wallet</h2>

            <input
                type="text"
                value={userId}
                disabled
                className="w-full bg-[#0f172a] p-3 rounded mb-3 text-gray-400"
            />

            <input
                type="number"
                placeholder="Amount..."
                value={amount}
                onChange={(e) => setAmount(e.target.value)}
                className="w-full bg-[#0f172a] p-3 rounded mb-4 text-blue-400"
            />

            {/* Basic Wallet */}
            <div className="grid grid-cols-2 gap-3 mb-4">
                <button onClick={() => handleTransaction('topup')} className="bg-blue-600 p-3 rounded">
                    TOPUP
                </button>
                <button onClick={() => handleTransaction('withdraw')} className="bg-red-600 p-3 rounded">
                    WITHDRAW
                </button>
            </div>

            <div className="grid grid-cols-2 gap-3">
                <button onClick={handleWinAuction} className="bg-yellow-600 p-3 rounded">
                    WIN AUCTION
                </button>
                <button onClick={handlePublishEvent} className="bg-purple-600 p-3 rounded">
                    TEST EVENT
                </button>
            </div>

            {status.msg && (
                <div className={`mt-4 p-3 rounded text-sm ${status.isError ? 'bg-red-500' : 'bg-green-500'}`}>
                    {status.msg}
                </div>
            )}
        </div>
    );
}