'use client';

import { useEffect, useState } from 'react';
import { useParams, useRouter } from 'next/navigation';
import Link from 'next/link';

const API_BASE = 'http://localhost:8080';

interface OrderDetail {
  id: number;
  auctionId: number;
  buyerId: string;
  sellerId: string;
  totalAmount: number;
  status: string;
  shippingAddress: string | null;
  trackingNumber: string | null;
  disputeReason: string | null;
  createdAt: string;
  updatedAt: string | null;
  confirmedAt: string | null;
  shippedAt: string | null;
  deliveredAt: string | null;
  disputedAt: string | null;
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
  if (!iso) return '—';
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
    case 'PENDING': return 'bg-yellow-500/20 text-yellow-300 border-yellow-500/40';
    case 'CONFIRMED': return 'bg-blue-500/20 text-blue-300 border-blue-500/40';
    case 'SHIPPED': return 'bg-purple-500/20 text-purple-300 border-purple-500/40';
    case 'DELIVERED': return 'bg-green-500/20 text-green-300 border-green-500/40';
    case 'DISPUTED': return 'bg-orange-500/20 text-orange-300 border-orange-500/40';
    case 'CANCELLED': return 'bg-red-500/20 text-red-300 border-red-500/40';
    default: return 'bg-gray-500/20 text-gray-300 border-gray-500/40';
  }
}

export default function OrderDetailPage() {
  const params = useParams();
  const router = useRouter();
  const id = params?.id as string;

  const [order, setOrder] = useState<OrderDetail | null>(null);
  const [me, setMe] = useState<string>('');
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [status, setStatus] = useState<UiStatus>({ type: '', message: '' });

  const [addressDraft, setAddressDraft] = useState('');
  const [trackingDraft, setTrackingDraft] = useState('');
  const [disputeOpen, setDisputeOpen] = useState(false);
  const [disputeDraft, setDisputeDraft] = useState('');

  useEffect(() => {
    const token = localStorage.getItem('token');
    const email = localStorage.getItem('email');
    if (!token) {
      router.push('/login');
      return;
    }
    setMe(email ?? '');
    load();
  }, [router, id]);

  const load = async () => {
    setLoading(true);
    setStatus({ type: '', message: '' });
    try {
      const res = await fetch(`${API_BASE}/orders/${id}`, { headers: authHeaders() });
      if (!res.ok) throw new Error('Gagal memuat pesanan');
      const data = (await res.json()) as OrderDetail;
      setOrder(data);
      setAddressDraft(data.shippingAddress ?? '');
    } catch (err) {
      setStatus({ type: 'error', message: err instanceof Error ? err.message : 'Unexpected error' });
    } finally {
      setLoading(false);
    }
  };

  const callPatch = async (path: string, body?: unknown) => {
    setBusy(true);
    setStatus({ type: '', message: '' });
    try {
      const res = await fetch(`${API_BASE}/orders/${id}${path}`, {
        method: 'PATCH',
        headers: authHeaders(),
        body: body ? JSON.stringify(body) : undefined,
      });
      if (!res.ok) {
        const err = await res.json().catch(() => ({ message: 'Gagal memproses' }));
        throw new Error(err.message ?? 'Gagal memproses');
      }
      const data = (await res.json()) as OrderDetail;
      setOrder(data);
      setStatus({ type: 'success', message: 'Berhasil diperbarui.' });
    } catch (err) {
      setStatus({ type: 'error', message: err instanceof Error ? err.message : 'Unexpected error' });
    } finally {
      setBusy(false);
    }
  };

  const handleSaveAddress = () => callPatch('/shipping-address', { address: addressDraft });
  const handleConfirm = () => callPatch('/confirm');
  const handleShip = () => {
    if (!trackingDraft.trim()) {
      setStatus({ type: 'error', message: 'Nomor resi wajib diisi.' });
      return;
    }
    callPatch('/ship', { trackingNumber: trackingDraft });
  };
  const handleReceive = () => callPatch('/receive');
  const handleDispute = () => {
    if (!disputeDraft.trim()) {
      setStatus({ type: 'error', message: 'Alasan dispute wajib diisi.' });
      return;
    }
    callPatch('/dispute', { reason: disputeDraft }).then(() => {
      setDisputeOpen(false);
      setDisputeDraft('');
    });
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-900 text-white p-6 md:p-8">
        <div className="max-w-3xl mx-auto bg-gray-800 border border-gray-700 rounded-xl p-8 text-center text-gray-400">
          Memuat detail pesanan...
        </div>
      </div>
    );
  }

  if (!order) {
    return (
      <div className="min-h-screen bg-gray-900 text-white p-6 md:p-8">
        <div className="max-w-3xl mx-auto bg-gray-800 border border-gray-700 rounded-xl p-8 text-center text-gray-400">
          Pesanan tidak ditemukan.
          <div className="mt-4">
            <Link href="/orders" className="text-blue-400 hover:underline">← Kembali ke daftar pesanan</Link>
          </div>
        </div>
      </div>
    );
  }

  const isBuyer = me === order.buyerId;
  const isSeller = me === order.sellerId;
  const canEditAddress = isBuyer && (order.status === 'PENDING' || order.status === 'CONFIRMED');
  const canConfirm = isSeller && order.status === 'PENDING';
  const canShip = isSeller && order.status === 'CONFIRMED';
  const canReceiveOrDispute = isBuyer && order.status === 'SHIPPED';

  return (
    <div className="min-h-screen bg-gray-900 text-white p-6 md:p-8">
      <div className="max-w-3xl mx-auto">
        <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4 mb-8">
          <div>
            <h1 className="text-3xl font-bold text-blue-400">Pesanan #{order.id}</h1>
            <p className="text-gray-400">Lelang #{order.auctionId} · {formatCurrency(order.totalAmount)}</p>
          </div>
          <Link href="/orders" className="px-4 py-2 rounded-lg bg-gray-700 hover:bg-gray-600 text-sm font-medium transition">← Daftar Pesanan</Link>
        </div>

        {status.message && (
          <div className={`mb-6 p-3 rounded-lg border text-sm ${status.type === 'error' ? 'bg-red-500/10 text-red-400 border-red-500/40' : 'bg-green-500/10 text-green-400 border-green-500/40'}`}>
            {status.message}
          </div>
        )}

        <div className="bg-gray-800 border border-gray-700 rounded-xl p-6 mb-6">
          <div className="flex items-center gap-3 mb-4">
            <span className={`text-xs border px-3 py-1 rounded-full ${statusColor(order.status)}`}>{order.status}</span>
            {order.disputeReason && (
              <span className="text-xs text-orange-300">Alasan: {order.disputeReason}</span>
            )}
          </div>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-3 text-sm">
            <div><span className="text-gray-500">Buyer:</span> <span className="text-gray-200">{order.buyerId}</span></div>
            <div><span className="text-gray-500">Seller:</span> <span className="text-gray-200">{order.sellerId}</span></div>
            <div><span className="text-gray-500">Dibuat:</span> <span className="text-gray-200">{formatDate(order.createdAt)}</span></div>
            <div><span className="text-gray-500">Dikonfirmasi:</span> <span className="text-gray-200">{formatDate(order.confirmedAt)}</span></div>
            <div><span className="text-gray-500">Dikirim:</span> <span className="text-gray-200">{formatDate(order.shippedAt)}</span></div>
            <div><span className="text-gray-500">Diterima:</span> <span className="text-gray-200">{formatDate(order.deliveredAt)}</span></div>
            {order.disputedAt && (
              <div><span className="text-gray-500">Disengketakan:</span> <span className="text-gray-200">{formatDate(order.disputedAt)}</span></div>
            )}
            {order.trackingNumber && (
              <div><span className="text-gray-500">Resi:</span> <span className="text-gray-200 font-mono">{order.trackingNumber}</span></div>
            )}
          </div>
        </div>

        <div className="bg-gray-800 border border-gray-700 rounded-xl p-6 mb-6">
          <h2 className="text-lg font-semibold mb-3">Alamat Pengiriman</h2>
          {canEditAddress ? (
            <>
              <textarea
                value={addressDraft}
                onChange={(e) => setAddressDraft(e.target.value)}
                rows={3}
                className="w-full px-4 py-3 bg-gray-700 border border-gray-600 rounded-lg text-white focus:ring-2 focus:ring-blue-500 outline-none"
                placeholder="Alamat lengkap pengiriman"
              />
              <button
                onClick={handleSaveAddress}
                disabled={busy || !addressDraft.trim()}
                className="mt-3 px-4 py-2 bg-blue-600 hover:bg-blue-700 disabled:opacity-60 rounded-lg font-semibold transition"
              >
                {busy ? 'Menyimpan...' : 'Simpan Alamat'}
              </button>
            </>
          ) : (
            <p className="text-gray-300 whitespace-pre-line">{order.shippingAddress ?? '(belum diisi)'}</p>
          )}
        </div>

        {(canConfirm || canShip || canReceiveOrDispute) && (
          <div className="bg-gray-800 border border-gray-700 rounded-xl p-6">
            <h2 className="text-lg font-semibold mb-3">Aksi</h2>
            <div className="flex flex-wrap gap-3">
              {canConfirm && (
                <button onClick={handleConfirm} disabled={busy} className="px-4 py-2 bg-blue-600 hover:bg-blue-700 disabled:opacity-60 rounded-lg font-semibold">
                  {busy ? 'Memproses...' : 'Konfirmasi Pesanan'}
                </button>
              )}
              {canShip && (
                <div className="flex flex-col gap-2 w-full md:w-auto">
                  <input
                    value={trackingDraft}
                    onChange={(e) => setTrackingDraft(e.target.value)}
                    placeholder="Nomor resi"
                    className="px-3 py-2 bg-gray-700 border border-gray-600 rounded-lg text-white outline-none focus:ring-2 focus:ring-blue-500"
                  />
                  <button onClick={handleShip} disabled={busy} className="px-4 py-2 bg-purple-600 hover:bg-purple-700 disabled:opacity-60 rounded-lg font-semibold">
                    {busy ? 'Memproses...' : 'Kirim'}
                  </button>
                </div>
              )}
              {canReceiveOrDispute && (
                <>
                  <button onClick={handleReceive} disabled={busy} className="px-4 py-2 bg-green-600 hover:bg-green-700 disabled:opacity-60 rounded-lg font-semibold">
                    {busy ? 'Memproses...' : 'Konfirmasi Terima'}
                  </button>
                  <button onClick={() => setDisputeOpen(true)} disabled={busy} className="px-4 py-2 bg-orange-600 hover:bg-orange-700 disabled:opacity-60 rounded-lg font-semibold">
                    Ajukan Sengketa
                  </button>
                </>
              )}
            </div>

            {disputeOpen && (
              <div className="mt-4 p-4 bg-gray-900 rounded-lg border border-gray-700">
                <label className="block text-sm text-gray-300 mb-2">Alasan sengketa</label>
                <textarea
                  value={disputeDraft}
                  onChange={(e) => setDisputeDraft(e.target.value)}
                  rows={3}
                  className="w-full px-3 py-2 bg-gray-700 border border-gray-600 rounded-lg text-white outline-none focus:ring-2 focus:ring-orange-500"
                  placeholder="Misal: Barang rusak saat tiba"
                />
                <div className="flex gap-2 mt-3">
                  <button onClick={handleDispute} disabled={busy} className="px-4 py-2 bg-orange-600 hover:bg-orange-700 disabled:opacity-60 rounded-lg font-semibold">
                    {busy ? 'Mengirim...' : 'Kirim Sengketa'}
                  </button>
                  <button onClick={() => { setDisputeOpen(false); setDisputeDraft(''); }} className="px-4 py-2 bg-gray-700 hover:bg-gray-600 rounded-lg">
                    Batal
                  </button>
                </div>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
