import WalletCard from './WalletCard';
import Link from 'next/link';

export default function WalletPage() {
    return (
        <main className="min-h-screen bg-[#0f172a] text-white p-8">
            <div className="max-w-4xl mx-auto">
                <div className="flex items-center gap-4 mb-10">
                    <Link href="/dashboard" className="text-gray-400 hover:text-white transition">
                        ← Back to Dashboard
                    </Link>
                    <h1 className="text-3xl font-bold text-[#3b82f6]">Manage Wallet</h1>
                </div>
                <WalletCard />
                <p className="text-center text-gray-600 mt-12 text-sm">
                    Secure transactions powered by BidMart Wallet System
                </p>
            </div>
        </main>
    );
}