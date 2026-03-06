'use client';
import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import axios from 'axios';
import Link from 'next/link';

interface User {
  id: number;
  email: string;
  displayName: string;
  enabled: boolean;
  role: string; 
}

export default function HomePage() {
  const [users, setUsers] = useState<User[]>([]);
  const [currentUser, setCurrentUser] = useState<string | null>(null);
  const [userRole, setUserRole] = useState<string | null>(null); // Track role
  const router = useRouter();

  useEffect(() => {
    const token = localStorage.getItem('token');
    const role = localStorage.getItem('role');
    const email = localStorage.getItem('email');

    if (!token) {
      router.push('/login');
      return;
    }

    setCurrentUser(email);
    setUserRole(role);

    // Only attempt to fetch the user list if the user is an ADMIN
    if (role === 'ADMIN') {
      fetchUsers(token);
    }
  }, [router]);

  const fetchUsers = async (token: string) => {
    try {
      const response = await axios.get('http://localhost:8080/api/auth/users', {
        headers: { 
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      });
      setUsers(response.data);
    } catch (err) {
      console.error("Axios Error Details:", err);
    }
  };

  const getRoleBadgeStyle = (role: string) => {
    switch (role?.toUpperCase()) {
      case 'ADMIN':
        return 'bg-red-500/20 text-red-400 border border-red-500/50';
      case 'SELLER':
        return 'bg-purple-500/20 text-purple-400 border border-purple-500/50';
      case 'BUYER':
        return 'bg-blue-500/20 text-blue-400 border border-blue-500/50';
      default:
        return 'bg-gray-500/20 text-gray-400 border border-gray-500/50';
    }
  };

  const handleLogout = () => {
    localStorage.clear();
    router.push('/login');
  };

  return (
    <div className="min-h-screen bg-gray-900 text-white p-8">
      <div className="max-w-5xl mx-auto">
        {/* Header Section */}
        <div className="flex justify-between items-center mb-10">
          <div>
            <h1 className="text-4xl font-extrabold text-blue-500 tracking-tight">BidMart Dashboard</h1>
            <p className="text-gray-400 mt-1">
              Logged in as: <span className="text-blue-300 font-medium">{currentUser}</span> 
              <span className="mx-2">|</span> 
              Role: <span className="uppercase text-xs font-bold px-2 py-0.5 rounded bg-gray-800 border border-gray-700">{userRole}</span>
            </p>
          </div>
          <button onClick={handleLogout} className="px-6 py-2 bg-red-600 hover:bg-red-700 rounded-lg font-semibold transition-all shadow-lg hover:shadow-red-900/20">
            Logout
          </button>
        </div>

        {/* Conditional Rendering based on Role */}
        {userRole === 'ADMIN' ? (
          <div className="bg-gray-800 rounded-xl border border-gray-700 overflow-hidden shadow-2xl">
            <div className="p-6 border-b border-gray-700 flex justify-between items-center bg-gray-800/50">
              <h3 className="text-xl font-semibold">System Management: Registered Users</h3>
              <span className="text-xs text-gray-500 italic">Admin Only Access</span>
            </div>
            <div className="overflow-x-auto">
              <table className="w-full text-left border-collapse">
                <thead className="bg-gray-700/50 text-gray-300 text-sm uppercase">
                  <tr>
                    <th className="px-6 py-4 border-b border-gray-700">ID</th>
                    <th className="px-6 py-4 border-b border-gray-700">Display Name</th>
                    <th className="px-6 py-4 border-b border-gray-700">Email</th>
                    <th className="px-6 py-4 border-b border-gray-700">Role</th>
                    <th className="px-6 py-4 text-center border-b border-gray-700">Status</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-700">
                  {users.map((user) => (
                    <tr key={user.id} className="hover:bg-gray-700/30 transition-colors group">
                      <td className="px-6 py-4 text-gray-500 text-sm">{user.id}</td>
                      <td className="px-6 py-4 font-medium group-hover:text-blue-400 transition-colors">{user.displayName}</td>
                      <td className="px-6 py-4 text-gray-300">{user.email}</td>
                      <td className="px-6 py-4">
                        <span className={`px-3 py-1 rounded-md text-[10px] font-bold uppercase tracking-wider ${getRoleBadgeStyle(user.role)}`}>
                          {user.role || 'USER'}
                        </span>
                      </td>
                      <td className="px-6 py-4 text-center">
                        <span className={`px-2 py-1 rounded-full text-[10px] font-bold uppercase ${user.enabled ? 'bg-green-500/20 text-green-400' : 'bg-yellow-500/20 text-yellow-400'}`}>
                          {user.enabled ? 'Verified' : 'Pending'}
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        ) : (
          /* Buyer/Seller View */
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div className="p-8 bg-gray-800 rounded-2xl border border-gray-700 shadow-xl">
              <h2 className="text-2xl font-bold text-blue-400 mb-4">Welcome back, {currentUser?.split('@')[0]}!</h2>
              <p className="text-gray-400 leading-relaxed">
                As a <span className="text-white font-semibold underline decoration-blue-500 underline-offset-4">{userRole}</span>, 
                you have full access to our auction features. Start bidding or list your items today.
              </p>
              <div className="mt-8 flex gap-4">
                <button className="px-6 py-2 bg-blue-600 hover:bg-blue-700 rounded-lg font-medium transition">Browse Auctions</button>
                <Link href="/wallet">
                  <button className="px-6 py-2 bg-emerald-600 hover:bg-emerald-700 rounded-lg font-medium transition shadow-lg hover:shadow-emerald-900/20">
                    My Wallet
                  </button>
                </Link>
                {userRole === 'SELLER' && (
                  <button className="px-6 py-2 bg-purple-600 hover:bg-purple-700 rounded-lg font-medium transition">Post New Item</button>
                )}
              </div>
            </div>
            
            <div className="p-8 bg-gradient-to-br from-gray-800 to-gray-900 rounded-2xl border border-gray-700 shadow-xl flex flex-col justify-center items-center text-center">
               <div className="w-16 h-16 bg-blue-500/10 rounded-full flex items-center justify-center mb-4">
                  <svg className="w-8 h-8 text-blue-500" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
               </div>
               <h3 className="text-lg font-semibold">User Management Hidden</h3>
               <p className="text-sm text-gray-500 mt-2">Only administrators can view the global user directory.</p>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}