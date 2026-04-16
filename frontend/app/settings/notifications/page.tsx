'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';

const API_BASE = 'http://localhost:8080';

interface PreferenceState {
  inAppEnabled: boolean;
  auctionStatusEnabled: boolean;
  orderUpdateEnabled: boolean;
  transactionEnabled: boolean;
  systemEnabled: boolean;
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

const DEFAULT_PREFS: PreferenceState = {
  inAppEnabled: true,
  auctionStatusEnabled: true,
  orderUpdateEnabled: true,
  transactionEnabled: true,
  systemEnabled: true,
};

const TOGGLES: { key: keyof PreferenceState; label: string; description: string }[] = [
  {
    key: 'inAppEnabled',
    label: 'Notifikasi Dalam Aplikasi',
    description: 'Saklar utama untuk seluruh notifikasi di dalam aplikasi.',
  },
  {
    key: 'auctionStatusEnabled',
    label: 'Status Lelang',
    description: 'Menang, kalah, atau lelang tidak laku.',
  },
  {
    key: 'orderUpdateEnabled',
    label: 'Pembaruan Pesanan',
    description: 'Pesanan dibuat dan perubahan status pengiriman.',
  },
  {
    key: 'transactionEnabled',
    label: 'Transaksi Dompet',
    description: 'Top up, penarikan, pembayaran.',
  },
  {
    key: 'systemEnabled',
    label: 'Sistem',
    description: 'Pengumuman dan pemberitahuan umum.',
  },
];

export default function NotificationPreferencesPage() {
  const [prefs, setPrefs] = useState<PreferenceState>(DEFAULT_PREFS);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [status, setStatus] = useState<UiStatus>({ type: '', message: '' });
  const router = useRouter();

  useEffect(() => {
    const token = localStorage.getItem('token');
    if (!token) {
      router.push('/login');
      return;
    }
    loadPrefs();
  }, [router]);

  const loadPrefs = async () => {
    setLoading(true);
    setStatus({ type: '', message: '' });
    try {
      const res = await fetch(`${API_BASE}/notifications/preferences`, {
        headers: authHeaders(),
      });
      if (!res.ok) throw new Error('Gagal memuat preferensi');
      const data = (await res.json()) as PreferenceState;
      setPrefs(data);
    } catch (err) {
      const msg = err instanceof Error ? err.message : 'Unexpected error';
      setStatus({ type: 'error', message: msg });
    } finally {
      setLoading(false);
    }
  };

  const handleToggle = (key: keyof PreferenceState) => {
    setPrefs((prev) => ({ ...prev, [key]: !prev[key] }));
  };

  const handleSave = async () => {
    setSaving(true);
    setStatus({ type: '', message: '' });
    try {
      const res = await fetch(`${API_BASE}/notifications/preferences`, {
        method: 'PUT',
        headers: authHeaders(),
        body: JSON.stringify(prefs),
      });
      if (!res.ok) throw new Error('Gagal menyimpan preferensi');
      const data = (await res.json()) as PreferenceState;
      setPrefs(data);
      setStatus({ type: 'success', message: 'Preferensi berhasil disimpan.' });
    } catch (err) {
      const msg = err instanceof Error ? err.message : 'Unexpected error';
      setStatus({ type: 'error', message: msg });
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="min-h-screen bg-gray-900 text-white p-6 md:p-8">
      <div className="max-w-2xl mx-auto">
        <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4 mb-8">
          <div>
            <h1 className="text-3xl font-bold text-blue-400">Preferensi Notifikasi</h1>
            <p className="text-gray-400">Atur jenis notifikasi yang ingin kamu terima.</p>
          </div>
          <div className="flex gap-3">
            <Link
              href="/notifications"
              className="px-4 py-2 rounded-lg bg-gray-700 hover:bg-gray-600 text-sm font-medium transition"
            >
              Inbox
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
            Memuat preferensi...
          </div>
        ) : (
          <div className="bg-gray-800 border border-gray-700 rounded-xl p-6 md:p-8 space-y-5">
            {TOGGLES.map((t) => (
              <div
                key={t.key}
                className="flex items-start justify-between gap-4 pb-4 border-b border-gray-700 last:border-b-0 last:pb-0"
              >
                <div>
                  <p className="font-medium text-white">{t.label}</p>
                  <p className="text-sm text-gray-400">{t.description}</p>
                </div>
                <button
                  type="button"
                  onClick={() => handleToggle(t.key)}
                  aria-pressed={prefs[t.key]}
                  className={`shrink-0 w-12 h-6 rounded-full transition relative ${
                    prefs[t.key] ? 'bg-blue-600' : 'bg-gray-600'
                  }`}
                >
                  <span
                    className={`absolute top-0.5 w-5 h-5 bg-white rounded-full transition ${
                      prefs[t.key] ? 'left-6' : 'left-0.5'
                    }`}
                  />
                </button>
              </div>
            ))}

            <button
              type="button"
              onClick={handleSave}
              disabled={saving}
              className="w-full md:w-auto px-6 py-3 bg-blue-600 hover:bg-blue-700 disabled:opacity-60 rounded-lg font-semibold transition"
            >
              {saving ? 'Menyimpan...' : 'Simpan Preferensi'}
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
