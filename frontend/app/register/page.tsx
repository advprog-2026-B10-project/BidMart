'use client';
import { useState } from 'react';
import axios from 'axios';
import Link from 'next/link';

export default function RegisterPage() {
  const [formData, setFormData] = useState({ email: '', password: '', displayName: '', role: '' });
  const [status, setStatus] = useState({ type: '', message: '' });

  const handleRegister = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await axios.post('http://localhost:8080/api/auth/register', formData);
      setStatus({ type: 'success', message: 'Registration successful! Check your DB for the token.' });
    } catch (err: any) {
        if (err.response?.status === 400 && typeof err.response.data === 'object') {
            // Collect all validation messages
            const messages = Object.values(err.response.data).join(', ');
            setStatus({ type: 'error', message: messages });
        } else {
            setStatus({ type: 'error', message: 'Registration failed.' });
        }
        }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-900 px-4">
      <div className="max-w-md w-full bg-gray-800 rounded-xl shadow-2xl p-8 border border-gray-700">
        <h2 className="text-3xl font-bold text-white text-center mb-6">Create Account</h2>
        
        {status.message && (
          <div className={`p-3 rounded-lg mb-6 text-sm ${status.type === 'error' ? 'bg-red-500/10 text-red-500 border border-red-500' : 'bg-green-500/10 text-green-500 border border-green-500'}`}>
            {status.message}
          </div>
        )}

        <form onSubmit={handleRegister} className="space-y-4">
          <input 
            type="text" placeholder="Full Name" 
            className="w-full px-4 py-3 bg-gray-700 border border-gray-600 rounded-lg text-white outline-none focus:ring-2 focus:ring-blue-500"
            onChange={(e) => setFormData({...formData, displayName: e.target.value})} required 
          />
          <input 
            type="email" placeholder="Email" 
            className="w-full px-4 py-3 bg-gray-700 border border-gray-600 rounded-lg text-white outline-none focus:ring-2 focus:ring-blue-500"
            onChange={(e) => setFormData({...formData, email: e.target.value})} required 
          />
          <input 
            type="password" placeholder="Password" 
            className="w-full px-4 py-3 bg-gray-700 border border-gray-600 rounded-lg text-white outline-none focus:ring-2 focus:ring-blue-500"
            onChange={(e) => setFormData({...formData, password: e.target.value})} required 
          />
          <div>
            <label className="block text-sm font-medium text-gray-300 mb-2">I want to be a:</label>
            <select 
              className="w-full px-4 py-3 bg-gray-700 border border-gray-600 rounded-lg text-white outline-none focus:ring-2 focus:ring-blue-500"
              onChange={(e) => setFormData({...formData, role: e.target.value})}
              required
            >
              <option value="">Select Role</option>
              <option value="BUYER">Buyer</option>
              <option value="SELLER">Seller</option>
            </select>
          </div>
          <button type="submit" className="w-full py-3 bg-blue-600 hover:bg-blue-700 text-white font-semibold rounded-lg transition">
            Register
          </button>
        </form>
        <p className="mt-6 text-center text-gray-400 text-sm">
          Already have an account? <Link href="/login" className="text-blue-500 hover:underline">Login</Link>
        </p>
      </div>
    </div>
  );
}