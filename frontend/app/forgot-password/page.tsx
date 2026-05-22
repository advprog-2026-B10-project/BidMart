'use client';
import { useState } from 'react';
import axios from 'axios';
import Link from 'next/link';

export default function ForgotPasswordPage() {
  const [email, setEmail] = useState('');
  const [status, setStatus] = useState({ type: '', message: '' });
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setStatus({ type: '', message: '' });

    try {
      await axios.post('http://localhost:8080/api/auth/forgot-password', { email });
      setStatus({ type: 'success', message: 'If the email is registered, a password reset link has been sent.' });
    } catch {
      setStatus({ type: 'success', message: 'If the email is registered, a password reset link has been sent.' });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-900 px-4">
      <div className="max-w-md w-full bg-gray-800 rounded-xl shadow-2xl p-8 border border-gray-700">
        <h2 className="text-3xl font-bold text-white text-center mb-2">Reset Password</h2>
        <p className="text-gray-400 text-center mb-6 text-sm">
          Enter your email and we&apos;ll send you a reset link.
        </p>

        {status.message && (
          <div className={`p-3 rounded-lg mb-6 text-sm ${status.type === 'error' ? 'bg-red-500/10 text-red-500 border border-red-500' : 'bg-blue-500/10 text-blue-300 border border-blue-500'}`}>
            {status.message}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-6">
          <div>
            <label className="block text-sm font-medium text-gray-300 mb-2">Email Address</label>
            <input
              type="email"
              className="w-full px-4 py-3 bg-gray-700 border border-gray-600 rounded-lg text-white focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none transition"
              placeholder="name@example.com"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
            />
          </div>

          <button
            type="submit"
            disabled={loading}
            className="w-full py-3 bg-blue-600 hover:bg-blue-700 text-white font-semibold rounded-lg shadow-md transition duration-200 disabled:opacity-50"
          >
            {loading ? 'Sending...' : 'Send Reset Link'}
          </button>
        </form>

        <p className="mt-6 text-center text-gray-400 text-sm">
          Remember your password?{' '}
          <Link href="/login" className="text-blue-500 hover:text-blue-400 font-medium transition">
            Sign in
          </Link>
        </p>
      </div>
    </div>
  );
}
