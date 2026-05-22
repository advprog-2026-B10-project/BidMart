"use client";
import { useEffect, useState } from 'react';

interface Transaction {
    id: number;
    userId: string;
    amount: number;
    type: string;
    status: string;
    idempotencyKey: string;
    createdAt: string;
}

export default function AdminTransactionList() {
    const [transactions, setTransactions] = useState<Transaction[]>([]);
    const [loading, setLoading] = useState(true);

    const fetchTransactions = async () => {
        try {
            const res = await fetch('http://localhost:8080/wallet/admin/transactions');
            if (res.ok) {
                const data = await res.json();
                setTransactions(data);
            }
        } catch (err) {
            console.error("Failed to fetch transactions", err);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchTransactions();
    }, []);

    return (
        <div className="bg-[#1e293b] p-6 rounded-2xl border border-gray-800 shadow-2xl mt-8">
            <div className="flex justify-between items-center mb-4">
                <h2 className="text-xl font-bold text-white">Admin View: Audit Trail</h2>
                <button 
                    onClick={fetchTransactions} 
                    className="text-sm bg-[#3b82f6] px-3 py-1 rounded text-white hover:bg-blue-500"
                >
                    Refresh
                </button>
            </div>
            
            {loading ? (
                <p className="text-gray-400">Loading transactions...</p>
            ) : (
                <div className="overflow-x-auto">
                    <table className="w-full text-left text-sm text-gray-400">
                        <thead className="bg-[#0f172a] text-gray-300 uppercase">
                            <tr>
                                <th className="p-3 rounded-tl-lg">ID</th>
                                <th className="p-3">User ID</th>
                                <th className="p-3">Type</th>
                                <th className="p-3">Amount</th>
                                <th className="p-3">Status</th>
                                <th className="p-3 rounded-tr-lg">Date</th>
                            </tr>
                        </thead>
                        <tbody>
                            {transactions.map((tx) => (
                                <tr key={tx.id} className="border-b border-gray-700 hover:bg-[#0f172a]">
                                    <td className="p-3">{tx.id}</td>
                                    <td className="p-3">{tx.userId}</td>
                                    <td className="p-3">
                                        <span className="bg-gray-700 px-2 py-1 rounded text-xs text-white">
                                            {tx.type}
                                        </span>
                                    </td>
                                    <td className="p-3 font-mono text-blue-400">{tx.amount}</td>
                                    <td className="p-3">
                                        <span className={`px-2 py-1 rounded text-xs text-white ${tx.status === 'SUCCESS' ? 'bg-green-600' : 'bg-red-600'}`}>
                                            {tx.status || 'UNKNOWN'}
                                        </span>
                                    </td>
                                    <td className="p-3">
                                        {new Date(tx.createdAt).toLocaleString()}
                                    </td>
                                </tr>
                            ))}
                            {transactions.length === 0 && (
                                <tr>
                                    <td colSpan={6} className="text-center p-4">No transactions found</td>
                                </tr>
                            )}
                        </tbody>
                    </table>
                </div>
            )}
        </div>
    );
}