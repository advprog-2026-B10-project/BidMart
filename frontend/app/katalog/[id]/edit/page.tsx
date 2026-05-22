'use client';
import { useState, useEffect, useSyncExternalStore } from 'react';
import { useParams, useRouter } from 'next/navigation';
import axiosClient from '@/lib/axiosClient';
import Link from 'next/link';

export default function EditCatalogPage() {
  const { id } = useParams();
  const router = useRouter();
  
  const userRole = useSyncExternalStore(
    () => () => {},
    () => typeof window !== 'undefined' ? localStorage.getItem('role') : null,
    () => null
  );

  const [deskripsi, setDeskripsi] = useState('');
  const [gambar, setGambar] = useState('');
  const [judul, setJudul] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    if (userRole !== 'SELLER' && userRole !== null) {
      alert('Hanya Seller yang dapat mengedit listing.');
      router.push(`/katalog/${id}`);
    }
  }, [userRole, router, id]);

  useEffect(() => {
    const fetchCatalog = async () => {
      try {
        const response = await axiosClient.get(`/katalog/${id}`);
        setJudul(response.data.judul);
        setDeskripsi(response.data.deskripsi || '');
        setGambar(response.data.gambar || '');
      } catch (err) {
        console.error('Error fetching catalog:', err);
        setError('Barang tidak ditemukan.');
      } finally {
        setIsLoading(false);
      }
    };
    if (id) void fetchCatalog();
  }, [id]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setIsSubmitting(true);
    
    try {
      await axiosClient.put(`/katalog/update/${id}`, {
        deskripsi,
        gambar
      });
      alert('Listing berhasil diupdate!');
      router.push(`/katalog/${id}`);
    } catch (err: any) {
      console.error(err);
      setError(err.response?.data?.message || 'Gagal mengupdate listing. Pastikan belum ada tawaran yang masuk.');
    } finally {
      setIsSubmitting(false);
    }
  };

  if (isLoading) {
    return (
      <div className="min-h-screen bg-gray-900 flex justify-center items-center">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-500"></div>
      </div>
    );
  }

  if (error && !judul) {
    return (
      <div className="min-h-screen bg-gray-900 text-white p-8 flex flex-col items-center justify-center">
        <h1 className="text-3xl text-red-500 mb-4">{error}</h1>
        <button onClick={() => router.push('/katalog')} className="px-6 py-2 bg-gray-800 rounded-md">Kembali ke Katalog</button>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-900 text-white p-8">
      <div className="max-w-2xl mx-auto">
        <div className="mb-8">
          <Link href={`/katalog/${id}`}>
            <button className="flex items-center text-gray-400 hover:text-white transition mb-6">
              <svg className="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M10 19l-7-7m0 0l7-7m-7 7h18"></path></svg>
              Kembali ke Detail
            </button>
          </Link>
          <h1 className="text-3xl font-bold text-white mb-2">Edit Listing</h1>
          <p className="text-gray-400">Kamu sedang mengedit: <span className="text-blue-400 font-semibold">{judul}</span></p>
          <div className="mt-3 p-3 bg-amber-500/10 border border-amber-500/30 rounded-lg">
            <p className="text-sm text-amber-400 font-medium flex items-start">
              <svg className="w-5 h-5 mr-2 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
              Informasi: Judul dan Harga tidak dapat diubah setelah listing dibuat. Edit hanya diizinkan jika belum ada tawaran yang masuk.
            </p>
          </div>
        </div>

        <div className="bg-gray-800 p-8 rounded-2xl border border-gray-700 shadow-xl">
          {error && <div className="p-4 mb-6 bg-red-500/20 text-red-400 border border-red-500/50 rounded-lg">{error}</div>}
          
          <form onSubmit={handleSubmit} className="space-y-6">
            <div>
              <label className="block text-sm font-medium text-gray-300 mb-2">Deskripsi Baru <span className="text-red-500">*</span></label>
              <textarea
                required
                rows={6}
                value={deskripsi}
                onChange={(e) => setDeskripsi(e.target.value)}
                className="w-full bg-gray-900 border border-gray-600 rounded-lg px-4 py-3 text-white focus:outline-none focus:border-blue-500 transition"
                placeholder="Jelaskan perubahan kondisi atau tambahkan info detail..."
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-300 mb-2">URL Gambar Baru</label>
              <input
                type="url"
                value={gambar}
                onChange={(e) => setGambar(e.target.value)}
                className="w-full bg-gray-900 border border-gray-600 rounded-lg px-4 py-3 text-white focus:outline-none focus:border-blue-500 transition"
                placeholder="https://example.com/image.jpg"
              />
              {gambar && (
                <div className="mt-3">
                  <p className="text-xs text-gray-400 mb-1">Preview:</p>
                  <img src={gambar} alt="Preview" className="h-32 rounded-lg object-cover border border-gray-700" onError={(e) => (e.currentTarget.style.display = 'none')} />
                </div>
              )}
            </div>

            <button
              type="submit"
              disabled={isSubmitting}
              className={`w-full py-3 mt-4 rounded-xl font-bold text-lg transition shadow-lg ${isSubmitting ? 'bg-blue-800 cursor-not-allowed' : 'bg-blue-600 hover:bg-blue-700 shadow-blue-900/20'}`}
            >
              {isSubmitting ? 'Menyimpan Perubahan...' : 'Simpan Perubahan'}
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}
