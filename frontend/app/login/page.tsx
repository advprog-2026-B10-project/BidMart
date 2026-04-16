'use client';
import { useState } from 'react';
import axios from 'axios';
import axiosClient from '@/lib/axiosClient';
import { useRouter } from 'next/navigation';
import Link from 'next/link';

export default function LoginPage() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [mfaCode, setMfaCode] = useState('');
  const [challengeToken, setChallengeToken] = useState('');
  const [mfaPending, setMfaPending] = useState(false);
  const [error, setError] = useState('');
  const [info, setInfo] = useState('');
  const [loading, setLoading] = useState(false);
  const router = useRouter();

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    setInfo('');
    
    try {
      const response = await axiosClient.post('/auth/login', { 
        email, 
        password 
      });

      if (response.data.mfaRequired) {
        setChallengeToken(response.data.mfaChallengeToken || '');
        setMfaPending(true);
        setInfo(response.data.message || 'Enter the 6-digit code from your authenticator app.');
        return;
      }
      
      localStorage.setItem('token', response.data.token);
      localStorage.setItem('refreshToken', response.data.refreshToken);
      localStorage.setItem('email', response.data.email);
      localStorage.setItem('role', response.data.role);
      router.push('/');
    } catch (err: unknown) {
      const message = axios.isAxiosError(err)
        ? (err.response?.data as { message?: string } | undefined)?.message || 'Invalid credentials or account not verified.'
        : 'Invalid credentials or account not verified.';
      setError(message);
    } finally {
      setLoading(false);
    }
  };

  const handleMfaVerify = async () => {
    setLoading(true);
    setError('');

    try {
      const response = await axiosClient.post('/auth/mfa/verify', {
        challengeToken,
        code: mfaCode,
      });

      localStorage.setItem('token', response.data.token);
      localStorage.setItem('refreshToken', response.data.refreshToken);
      localStorage.setItem('email', response.data.email);
      localStorage.setItem('role', response.data.role);
      router.push('/');
    } catch (err: unknown) {
      const message = axios.isAxiosError(err)
        ? (err.response?.data as { message?: string } | undefined)?.message || 'Invalid MFA code.'
        : 'Invalid MFA code.';
      setError(message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-900 px-4">
      <div className="max-w-md w-full bg-gray-800 rounded-xl shadow-2xl p-8 border border-gray-700">
        <div className="text-center mb-8">
          <h2 className="text-3xl font-bold text-white">BidMart</h2>
          <p className="text-gray-400 mt-2">Sign in to start bidding</p>
        </div>

        {error && (
          <div className="bg-red-500/10 border border-red-500 text-red-500 text-sm p-3 rounded-lg mb-6">
            {error}
          </div>
        )}

        {info && !error && (
          <div className="bg-blue-500/10 border border-blue-500 text-blue-300 text-sm p-3 rounded-lg mb-6">
            {info}
          </div>
        )}

        <form
          onSubmit={mfaPending ? (e) => {
            e.preventDefault();
            void handleMfaVerify();
          } : handleLogin}
          className="space-y-6"
        >
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

          <div>
            <label className="block text-sm font-medium text-gray-300 mb-2">Password</label>
            <input 
              type="password" 
              className="w-full px-4 py-3 bg-gray-700 border border-gray-600 rounded-lg text-white focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none transition"
              placeholder="••••••••"
              value={password} 
              onChange={(e) => setPassword(e.target.value)} 
              required 
            />
          </div>

          <button 
            type="submit" 
            disabled={loading || mfaPending}
            className="w-full py-3 bg-blue-600 hover:bg-blue-700 text-white font-semibold rounded-lg shadow-md transition duration-200 transform active:scale-[0.98] disabled:opacity-50"
          >
            {loading ? 'Signing in...' : mfaPending ? 'Continue' : 'Login'}
          </button>
        </form>

        {mfaPending && (
          <div className="mt-6 space-y-4 rounded-xl border border-gray-700 bg-gray-900/60 p-4">
            <div>
              <label className="block text-sm font-medium text-gray-300 mb-2">Authentication code</label>
              <input
                type="text"
                inputMode="numeric"
                autoComplete="one-time-code"
                maxLength={6}
                className="w-full px-4 py-3 bg-gray-700 border border-gray-600 rounded-lg text-white focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none transition tracking-[0.4em] text-center"
                placeholder="123456"
                value={mfaCode}
                onChange={(e) => setMfaCode(e.target.value.replace(/\D/g, '').slice(0, 6))}
              />
            </div>

            <button
              type="button"
              onClick={handleMfaVerify}
              disabled={loading || mfaCode.length !== 6}
              className="w-full py-3 bg-emerald-600 hover:bg-emerald-700 text-white font-semibold rounded-lg shadow-md transition duration-200 disabled:opacity-50"
            >
              {loading ? 'Verifying...' : 'Verify MFA Code'}
            </button>

            <p className="text-xs text-gray-500 leading-relaxed">
              Open your authenticator app and enter the current 6-digit code for your BidMart account.
            </p>
          </div>
        )}

        <div className="mt-8 text-center text-sm">
          <p className="text-gray-400">
            Don&apos;t have an account?{' '}
            <Link href="/register" className="text-blue-500 hover:text-blue-400 font-medium transition">
              Register here
            </Link>
          </p>
        </div>
      </div>
    </div>
  );
}