'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';

const API_BASE = 'http://localhost:8080';

interface OrderItem {
  id: number;
  auctionId: number;
  buyerId: string;
  sellerId: string;
  totalAmount: number;
  status: string;
  shippingAddress: string | null;
  trackingNumber: string | null;
  createdAt: string;
  updatedAt: string | null;
}

interface UiStatus {
  type: 'success' | 'error' | '';
  message: string;
}

function authHeaders(): Record<string, string> {
  const token = typeof window !== 'undefined' ? localStorage.getItem('token') : null;
  const headers: Record<string, string> = { 'Content-Type': 'application/json' };
  if (token) headers.Authorization = `Bearer ${token}`;
  return headers;
}

function formatDate(iso: string | null): string {
  if (!iso) return '-';
  try {
    return new Date(iso).toLocaleString('id-ID', {
      dateStyle: 'medium',
      timeStyle: 'short',
    });
  } catch {
    return iso;
  }
}

function formatCurrency(amount: number): string {
  return new Intl.NumberFormat('id-ID', {
    style: 'currency',
    currency: 'IDR',
    minimumFractionDigits: 0,
  }).format(amount);
}

function statusColor(status: string): string {
  switch (status) {
    case 'PENDING':
      return 'bg-yellow-500/20 text-yellow-300 border-yellow-500/40';
    case 'CONFIRMED':
      return 'bg-blue-500/20 text-blue-300 border-blue-500/40';
    case 'SHIPPED':
      return 'bg-purple-500/20 text-purple-300 border-purple-500/40';
    case 'DELIVERED':
      return 'bg-green-500/20 text-green-300 border-green-500/40';
    case 'CANCELLED':
      return 'bg-red-500/20 text-red-300 border-red-500/40';
    default:
      return 'bg-gray-500/20 text-gray-300 border-gray-500/40';
  }
}

export default function OrdersPage() {
  const [orders, setOrders] = useState<OrderItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [status, setStatus] = useState<UiStatus>({ type: '', message: '' });
  const router = useRouter();

  useEffect(() => {
    const token = localStorage.getItem('token');
    if (!token) {
      router.push('/login');
      return;
    }
    loadOrders();
  }, [router]);

  const loadOrders = async () => {
    setLoading(true);
    setStatus({ type: '', message: '' });
    try {
      const res = await fetch(`${API_BASE}/orders/me`, { headers: authHeaders() });
      if (!res.ok) throw new Error('Gagal memuat pesanan');
      const data = (await res.json()) as OrderItem[];
      setOrders(data);
    } catch (err) {
      const msg = err instanceof Error ? err.message : 'Unexpected error';
      setStatus({ type: 'error', message: msg });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-gray-900 text-white p-6 md:p-8">
      <div className="max-w-4xl mx-auto">
        <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4 mb-8">
          <div>
            <h1 className="text-3xl font-bold text-blue-400">Pesanan Saya</h1>
            <p className="text-gray-400">Daftar pesanan dari lelang yang kamu menangkan.</p>
          </div>
          <div className="flex gap-3">
            <Link
              href="/notifications"
              className="px-4 py-2 rounded-lg bg-gray-700 hover:bg-gray-600 text-sm font-medium transition"
            >
              Notifikasi
            </Link>
            <Link
              href="/"
              className="px-4 py-2 rounded-lg bg-gray-700 hover:bg-gray-600 text-sm font-medium transition"
            >
              Kembali
            </Link>
          </div>
        </div>

        {status.message && (
          <div
            className={`mb-6 p-3 rounded-lg border text-sm ${
              status.type === 'error'
                ? 'bg-red-500/10 text-red-400 border-red-500/40'
                : 'bg-green-500/10 text-green-400 border-green-500/40'
            }`}
          >
            <p>{status.message}</p>
          </div>
        )}

        {loading ? (
          <div className="bg-gray-800 border border-gray-700 rounded-xl p-8 text-center text-gray-400">
            Memuat pesanan...
          </div>
        ) : orders.length === 0 ? (
          <div className="bg-gray-800 border border-gray-700 rounded-xl p-8 text-center text-gray-400">
            Belum ada pesanan.
          </div>
        ) : (
          <div className="overflow-hidden rounded-xl border border-gray-700">
            <table className="w-full text-sm">
              <thead className="bg-gray-800 text-gray-400 uppercase text-xs">
                <tr>
                  <th className="px-4 py-3 text-left">Order</th>
                  <th className="px-4 py-3 text-left">Lelang</th>
                  <th className="px-4 py-3 text-left">Total</th>
                  <th className="px-4 py-3 text-left">Status</th>
                  <th className="px-4 py-3 text-left">Dibuat</th>
                </tr>
              </thead>
              <tbody className="bg-gray-900">
                {orders.map((o) => (
                  <tr
                    key={o.id}
                    className="border-t border-gray-800 hover:bg-gray-800/60 transition"
                  >
                    <td className="px-4 py-3 font-mono text-gray-300">#{o.id}</td>
                    <td className="px-4 py-3 text-gray-300">#{o.auctionId}</td>
                    <td className="px-4 py-3 font-semibold text-white">
                      {formatCurrency(o.totalAmount)}
                    </td>
                    <td className="px-4 py-3">
                      <span
                        className={`text-xs border px-2 py-1 rounded-full ${statusColor(o.status)}`}
                      >
                        {o.status}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-gray-400">{formatDate(o.createdAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
