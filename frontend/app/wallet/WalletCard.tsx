"use client";
import { useState } from 'react';

export default function WalletCard() {
    const [userId, setUserId] = useState('johnseller@gmail.com');
    const [amount, setAmount] = useState('');
    const [status, setStatus] = useState({ msg: '', isError: false });

    const handleTransaction = async (type: 'topup' | 'withdraw') => {
        if (!amount) return;
        try {
            // Sesuai WalletController.java
            const res = await fetch(`http://localhost:8080/wallet/${type}?userId=${userId}&amount=${amount}`, {
                method: 'POST'
            });
            const text = await res.text();
            setStatus({ msg: text, isError: !res.ok });
        } catch (err) {
            setStatus({ msg: "Connection failed", isError: true });
        }
    };

    return (
        <div className="bg-[#1e293b] p-8 rounded-2xl border border-gray-800 shadow-2xl max-w-lg mx-auto">
            <div className="flex justify-between items-center mb-6">
                <h2 className="text-xl font-bold text-white">Digital Wallet</h2>
                <span className="text-xs bg-blue-500/10 text-blue-400 px-3 py-1 rounded-full border border-blue-500/20">Active</span>
            </div>

            <div className="space-y-4">
                <div>
                    <label className="text-xs text-gray-400 uppercase tracking-wider font-semibold">User Account</label>
                    <input
                        type="text"
                        value={userId}
                        disabled
                        className="w-full bg-[#0f172a] border border-gray-700 rounded-xl p-3 mt-1 text-gray-300 opacity-60"
                    />
                </div>

                <div>
                    <label className="text-xs text-gray-400 uppercase tracking-wider font-semibold">Amount (IDR)</label>
                    <input
                        type="number"
                        placeholder="Enter amount..."
                        value={amount}
                        onChange={(e) => setAmount(e.target.value)}
                        className="w-full bg-[#0f172a] border border-gray-700 rounded-xl p-4 mt-1 text-2xl font-bold text-[#60a5fa] focus:border-blue-500 outline-none transition"
                    />
                </div>

                <div className="grid grid-cols-2 gap-4 pt-4">
                    <button
                        onClick={() => handleTransaction('topup')}
                        className="bg-[#2563eb] hover:bg-blue-700 py-4 rounded-xl font-bold text-white transition transform active:scale-95"
                    >
                        TOP UP
                    </button>
                    <button
                        onClick={() => handleTransaction('withdraw')}
                        className="bg-[#1e293b] hover:bg-gray-800 border border-red-500/50 text-red-400 py-4 rounded-xl font-bold transition transform active:scale-95"
                    >
                        WITHDRAW
                    </button>
                </div>

                {status.msg && (
                    <div className={`mt-4 p-4 rounded-xl text-center text-sm font-medium ${status.isError ? 'bg-red-500/10 text-red-400 border border-red-500/20' : 'bg-green-500/10 text-green-400 border border-green-500/20'}`}>
                        {status.msg}
                    </div>
                )}
            </div>
        </div>
    );
}