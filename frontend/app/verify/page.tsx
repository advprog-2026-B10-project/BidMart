'use client';
import { useEffect, useState, Suspense } from 'react';
import { useSearchParams } from 'next/navigation';
import axios from 'axios';

function VerifyContent() {
  const searchParams = useSearchParams();
  const urlToken = searchParams.get('token'); // Grab token from ?token=...
  const [status, setStatus] = useState('Checking token...');

  useEffect(() => {
    if (urlToken) {
      handleAutoVerify(urlToken);
    }
  }, [urlToken]);

  const handleAutoVerify = async (token: string) => {
    try {
      await axios.get(`http://localhost:8080/api/auth/verify?token=${token}`);
      setStatus('Account verified! Redirecting to login...');
      setTimeout(() => window.location.href = '/login', 3000);
    } catch (err) {
      setStatus('Verification failed. The token may be invalid.');
    }
  };

  return (
    <div className="text-white text-center">
      <h2 className="text-2xl font-bold mb-4">Verification Status</h2>
      <p className="bg-gray-800 p-4 rounded-lg border border-gray-700">{status}</p>
    </div>
  );
}

export default function VerifyPage() {
  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-900">
      <Suspense fallback={<p>Loading...</p>}>
        <VerifyContent />
      </Suspense>
    </div>
  );
}