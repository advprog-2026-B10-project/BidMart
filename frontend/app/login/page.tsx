'use client';
import { useState } from 'react';
import axios from 'axios';
import { useRouter } from 'next/navigation';
import Link from 'next/link'; // For the redirect link

export default function LoginPage() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const router = useRouter();

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    
    try {
      const response = await axios.post('http://localhost:8080/api/auth/login', { 
        email, 
        password 
      });
      
      localStorage.setItem('token', response.data.token);
      localStorage.setItem('email', response.data.email);
      localStorage.setItem('role', response.data.role);
      router.push('/');
    } catch (err: any) {
      setError(err.response?.data?.message || 'Invalid credentials or account not verified.');
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

        <form onSubmit={handleLogin} className="space-y-6">
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
            disabled={loading}
            className="w-full py-3 bg-blue-600 hover:bg-blue-700 text-white font-semibold rounded-lg shadow-md transition duration-200 transform active:scale-[0.98] disabled:opacity-50"
          >
            {loading ? 'Signing in...' : 'Login'}
          </button>
        </form>

        <div className="mt-8 text-center text-sm">
          <p className="text-gray-400">
            Don't have an account?{' '}
            <Link href="/register" className="text-blue-500 hover:text-blue-400 font-medium transition">
              Register here
            </Link>
          </p>
        </div>
      </div>
    </div>
  );
}