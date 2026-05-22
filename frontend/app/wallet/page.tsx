"use client";
import { useEffect, useState } from 'react';
import WalletCard from './WalletCard';
import AdminTransactionList from './AdminTransactionList';
import Link from 'next/link';

export default function WalletPage() {
    const [role, setRole] = useState<'seller' | 'buyer' | null>(null);
    const [userEmail, setUserEmail] = useState<string>("");

    useEffect(() => {
        const fetchProfile = async () => {
            try {
                const token = localStorage.getItem('token');
                if (!token) return;

                const res = await fetch('http://localhost:8080/api/auth/profile', {
                    headers: {
                        'Authorization': `Bearer ${token}`
                    }
                });

                if (res.ok) {
                    const data = await res.json();
                    const userRole = data.role?.toLowerCase();
                    setRole(userRole === 'seller' ? 'seller' : 'buyer');

                    if (data.email) {
                        setUserEmail(data.email);
                    } else if (data.userId) {
                        setUserEmail(data.userId);
                    }
                }
            } catch (err) {
                console.error("Gagal fetch profile", err);
            }
        };

        fetchProfile();
    }, []);

    return (
        <main className="min-h-screen bg-[#0f172a] text-white p-8">
            <div className="max-w-5xl mx-auto">
                <div className="flex items-center gap-4 mb-10">
                    <Link href="/dashboard" className="text-gray-400 hover:text-white transition">
                        ← Back to Dashboard
                    </Link>
                    <h1 className="text-3xl font-bold text-[#3b82f6]">Manage Wallet</h1>
                </div>

                {role === null || userEmail === "" ? (
                    <div className="flex flex-col items-center justify-center p-20">
                        <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-blue-500 mb-4"></div>
                        <p className="text-gray-400 text-center">Loading wallet profile...</p>
                    </div>
                ) : (
                    <>
                        <WalletCard role={role} userId={userEmail} />
                        <AdminTransactionList />
                    </>
                )}

                <p className="text-center text-gray-600 mt-12 text-sm">
                    Secure transactions powered by BidMart Wallet System
                </p>
            </div>
        </main>
    );
}