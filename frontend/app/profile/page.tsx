'use client';

import { useEffect, useState } from 'react';
import axios from 'axios';
import Link from 'next/link';
import { useRouter } from 'next/navigation';

interface ProfileResponse {
  id: number;
  email: string;
  displayName: string;
  phoneNumber: string | null;
  role: string;
  enabled: boolean;
  mfaEnabled: boolean;
}

interface ApiError {
  message?: string;
  details?: Record<string, string>;
}

export default function ProfilePage() {
  const [profile, setProfile] = useState<ProfileResponse | null>(null);
  const [displayName, setDisplayName] = useState('');
  const [phoneNumber, setPhoneNumber] = useState('');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [status, setStatus] = useState<{ type: 'success' | 'error' | ''; message: string }>({
    type: '',
    message: '',
  });
  const router = useRouter();

  useEffect(() => {
    const token = localStorage.getItem('token');
    if (!token) {
      router.push('/login');
      return;
    }

    fetchProfile(token);
  }, [router]);

  const fetchProfile = async (token: string) => {
    setLoading(true);
    setStatus({ type: '', message: '' });

    try {
      const response = await axios.get<ProfileResponse>('http://localhost:8080/api/auth/profile', {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      });

      setProfile(response.data);
      setDisplayName(response.data.displayName ?? '');
      setPhoneNumber(response.data.phoneNumber ?? '');
    } catch (err: unknown) {
      const fallbackMessage = 'Failed to load profile.';
      if (axios.isAxiosError(err)) {
        const apiError = err.response?.data as ApiError | undefined;
        setStatus({ type: 'error', message: apiError?.message ?? fallbackMessage });
      } else {
        setStatus({ type: 'error', message: fallbackMessage });
      }
    } finally {
      setLoading(false);
    }
  };

  const handleSave = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();

    const token = localStorage.getItem('token');
    if (!token) {
      router.push('/login');
      return;
    }

    setSaving(true);
    setStatus({ type: '', message: '' });

    try {
      const response = await axios.patch<ProfileResponse>(
        'http://localhost:8080/api/auth/profile',
        {
          displayName,
          phoneNumber,
        },
        {
          headers: {
            Authorization: `Bearer ${token}`,
          },
        }
      );

      setProfile(response.data);
      setStatus({ type: 'success', message: 'Profile updated successfully.' });
    } catch (err: unknown) {
      const fallbackMessage = 'Failed to update profile.';

      if (axios.isAxiosError(err)) {
        const apiError = err.response?.data as ApiError | undefined;
        const details = apiError?.details ? Object.values(apiError.details).join(', ') : '';
        setStatus({
          type: 'error',
          message: details || apiError?.message || fallbackMessage,
        });
      } else {
        setStatus({ type: 'error', message: fallbackMessage });
      }
    } finally {
      setSaving(false);
    }
  };

  const handleLogout = () => {
    localStorage.clear();
    router.push('/login');
  };

  return (
    <div className="min-h-screen bg-gray-900 text-white p-6 md:p-8">
      <div className="max-w-3xl mx-auto">
        <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4 mb-8">
          <div>
            <h1 className="text-3xl font-bold text-blue-400">My Profile</h1>
            <p className="text-gray-400">Manage your account information</p>
          </div>
          <div className="flex gap-3">
            <Link
              href="/"
              className="px-4 py-2 rounded-lg bg-gray-700 hover:bg-gray-600 text-sm font-medium transition"
            >
              Back to Dashboard
            </Link>
            <button
              onClick={handleLogout}
              className="px-4 py-2 rounded-lg bg-red-600 hover:bg-red-700 text-sm font-medium transition"
            >
              Logout
            </button>
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
            {status.message}
          </div>
        )}

        {loading ? (
          <div className="bg-gray-800 border border-gray-700 rounded-xl p-8 text-center text-gray-400">
            Loading profile...
          </div>
        ) : profile ? (
          <div className="bg-gray-800 border border-gray-700 rounded-xl shadow-xl p-6 md:p-8">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-8">
              <div className="bg-gray-900/60 rounded-lg p-4 border border-gray-700">
                <p className="text-xs uppercase text-gray-500">Email</p>
                <p className="font-medium text-gray-200 break-all">{profile.email}</p>
              </div>
              <div className="bg-gray-900/60 rounded-lg p-4 border border-gray-700">
                <p className="text-xs uppercase text-gray-500">Role</p>
                <p className="font-medium text-gray-200">{profile.role}</p>
              </div>
              <div className="bg-gray-900/60 rounded-lg p-4 border border-gray-700">
                <p className="text-xs uppercase text-gray-500">Email Verification</p>
                <p className="font-medium text-gray-200">{profile.enabled ? 'Verified' : 'Pending Verification'}</p>
              </div>
              <div className="bg-gray-900/60 rounded-lg p-4 border border-gray-700">
                <p className="text-xs uppercase text-gray-500">2FA Status</p>
                <p className="font-medium text-gray-200">{profile.mfaEnabled ? 'Enabled' : 'Disabled'}</p>
              </div>
            </div>

            <form onSubmit={handleSave} className="space-y-5">
              <div>
                <label className="block text-sm text-gray-300 mb-2">Display Name</label>
                <input
                  type="text"
                  value={displayName}
                  onChange={(e) => setDisplayName(e.target.value)}
                  className="w-full px-4 py-3 bg-gray-700 border border-gray-600 rounded-lg text-white focus:ring-2 focus:ring-blue-500 outline-none"
                  placeholder="Your display name"
                />
              </div>

              <div>
                <label className="block text-sm text-gray-300 mb-2">Phone Number</label>
                <input
                  type="text"
                  value={phoneNumber}
                  onChange={(e) => setPhoneNumber(e.target.value)}
                  className="w-full px-4 py-3 bg-gray-700 border border-gray-600 rounded-lg text-white focus:ring-2 focus:ring-blue-500 outline-none"
                  placeholder="Example: +628123456789"
                />
                <p className="text-xs text-gray-500 mt-2">Use 8-15 digits, optional + prefix.</p>
              </div>

              <button
                type="submit"
                disabled={saving}
                className="w-full md:w-auto px-6 py-3 bg-blue-600 hover:bg-blue-700 disabled:opacity-60 rounded-lg font-semibold transition"
              >
                {saving ? 'Saving...' : 'Save Changes'}
              </button>
            </form>
          </div>
        ) : (
          <div className="bg-gray-800 border border-gray-700 rounded-xl p-8 text-center text-gray-400">
            Unable to load profile.
          </div>
        )}
      </div>
    </div>
  );
}
