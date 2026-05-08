'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';

const API_BASE = 'http://localhost:8080';

interface NotificationItem {
  id: number;
  title: string;
  message: string;
  type: string;
  channel: string;
  isRead: boolean;
  referenceId: string | null;
  createdAt: string;
  readAt: string | null;
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

function formatDate(iso: string): string {
  try {
    return new Date(iso).toLocaleString('id-ID', {
      dateStyle: 'medium',
      timeStyle: 'short',
    });
  } catch {
    return iso;
  }
}

export default function NotificationsPage() {
  const [items, setItems] = useState<NotificationItem[]>([]);
  const [unreadCount, setUnreadCount] = useState<number>(0);
  const [loading, setLoading] = useState(true);
  const [status, setStatus] = useState<UiStatus>({ type: '', message: '' });
  const router = useRouter();

  useEffect(() => {
    const token = localStorage.getItem('token');
    if (!token) {
      router.push('/login');
      return;
    }
    loadAll();
  }, [router]);

  const loadAll = async () => {
    setLoading(true);
    setStatus({ type: '', message: '' });
    try {
      const [listRes, countRes] = await Promise.all([
        fetch(`${API_BASE}/notifications/me`, { headers: authHeaders() }),
        fetch(`${API_BASE}/notifications/me/unread-count`, { headers: authHeaders() }),
      ]);
      if (!listRes.ok) throw new Error('Failed to load notifications');
      if (!countRes.ok) throw new Error('Failed to load unread count');
      const list = (await listRes.json()) as NotificationItem[];
      const count = (await countRes.json()) as { count: number };
      setItems(list);
      setUnreadCount(count.count);
    } catch (err) {
      const msg = err instanceof Error ? err.message : 'Unexpected error';
      setStatus({ type: 'error', message: msg });
    } finally {
      setLoading(false);
    }
  };

  const handleMarkRead = async (id: number) => {
    try {
      const res = await fetch(`${API_BASE}/notifications/${id}/read`, {
        method: 'PATCH',
        headers: authHeaders(),
      });
      if (!res.ok) throw new Error('Failed to mark as read');
      const updated = (await res.json()) as NotificationItem;
      setItems((prev) => prev.map((n) => (n.id === updated.id ? updated : n)));
      setUnreadCount((c) => Math.max(0, c - 1));
    } catch (err) {
      const msg = err instanceof Error ? err.message : 'Unexpected error';
      setStatus({ type: 'error', message: msg });
    }
  };

  return (
    <div className="min-h-screen bg-gray-900 text-white p-6 md:p-8">
      <div className="max-w-3xl mx-auto">
        <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4 mb-8">
          <div>
            <h1 className="text-3xl font-bold text-blue-400">Notifikasi</h1>
            <p className="text-gray-400">
              {unreadCount > 0
                ? `${unreadCount} belum dibaca`
                : 'Semua notifikasi sudah dibaca'}
            </p>
          </div>
          <div className="flex gap-3">
            <Link
              href="/settings/notifications"
              className="px-4 py-2 rounded-lg bg-gray-700 hover:bg-gray-600 text-sm font-medium transition"
            >
              Preferensi
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
            Memuat notifikasi...
          </div>
        ) : items.length === 0 ? (
          <div className="bg-gray-800 border border-gray-700 rounded-xl p-8 text-center text-gray-400">
            Belum ada notifikasi.
          </div>
        ) : (
          <ul className="space-y-3">
            {items.map((n) => (
              <li
                key={n.id}
                className={`bg-gray-800 border rounded-xl p-4 transition ${
                  n.isRead ? 'border-gray-700' : 'border-blue-500/60'
                }`}
              >
                <div className="flex items-start justify-between gap-3">
                  <div className="flex-1">
                    <div className="flex items-center gap-2 mb-1">
                      <h3 className="font-semibold text-white">{n.title}</h3>
                      {!n.isRead && (
                        <span className="text-xs bg-blue-500/20 text-blue-300 border border-blue-500/40 px-2 py-0.5 rounded-full">
                          Baru
                        </span>
                      )}
                      <span className="text-xs text-gray-500 ml-auto">
                        {formatDate(n.createdAt)}
                      </span>
                    </div>
                    <p className="text-gray-300 text-sm">{n.message}</p>
                    <p className="text-xs text-gray-500 mt-1">{n.type}</p>
                  </div>
                  {!n.isRead && (
                    <button
                      onClick={() => handleMarkRead(n.id)}
                      className="px-3 py-1.5 text-xs bg-blue-600 hover:bg-blue-700 rounded-md font-semibold transition shrink-0"
                    >
                      Tandai dibaca
                    </button>
                  )}
                </div>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  );
}
