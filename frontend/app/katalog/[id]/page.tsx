'use client';
import { useEffect, useState, useSyncExternalStore } from 'react';
import { useParams, useRouter } from 'next/navigation';
import axiosClient from '@/lib/axiosClient';
import Link from 'next/link';

interface Catalog {
  id: number;
  judul: string;
  deskripsi: string;
  hargaAwal: number;
  hargaBeliSekarang?: number;
  gambar?: string;
  endTime?: string;
  category?: {
    id: number;
    name: string;
  };
  sellerId?: number;
}

export default function CatalogDetailPage() {
  const { id } = useParams();
  const router = useRouter();
  const [catalog, setCatalog] = useState<Catalog | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');
  
  const userRole = useSyncExternalStore(
    () => () => {},
    () => typeof window !== 'undefined' ? localStorage.getItem('role') : null,
    () => null
  );

  useEffect(() => {
    const fetchCatalog = async () => {
      try {
        const response = await axiosClient.get(`/katalog/${id}`);
        setCatalog(response.data);
      } catch (err) {
        console.error('Error fetching catalog:', err);
        setError('Barang tidak ditemukan.');
      } finally {
        setIsLoading(false);
      }
    };
    if (id) void fetchCatalog();
  }, [id]);

  const handleCancel = async () => {
    if (!confirm('Apakah kamu yakin ingin membatalkan listing ini? Tindakan ini tidak dapat diurungkan.')) return;
    try {
      await axiosClient.delete(`/katalog/cancel/${id}`);
      alert('Listing berhasil dibatalkan.');
      router.push('/katalog');
    } catch (err: any) {
      alert(err.response?.data?.message || 'Gagal membatalkan listing.');
    }
  };

  if (isLoading) {
    return (
      <div className="min-h-screen bg-gray-900 flex justify-center items-center">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-500"></div>
      </div>
    );
  }

  if (error || !catalog) {
    return (
      <div className="min-h-screen bg-gray-900 text-white p-8 flex flex-col items-center justify-center">
        <h1 className="text-3xl text-red-500 mb-4">{error}</h1>
        <button onClick={() => router.push('/katalog')} className="px-6 py-2 bg-gray-800 rounded-md">Kembali ke Katalog</button>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-900 text-white p-8">
      <div className="max-w-4xl mx-auto">
        <div className="mb-6 flex justify-between items-center">
          <Link href="/katalog">
            <button className="flex items-center text-gray-400 hover:text-white transition">
              <svg className="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M10 19l-7-7m0 0l7-7m-7 7h18"></path></svg>
              Kembali
            </button>
          </Link>

          {/* Akses Seller */}
          {userRole === 'SELLER' && (
            <div className="flex gap-3">
              <Link href={`/katalog/${catalog.id}/edit`}>
                <button className="px-4 py-2 bg-blue-600 hover:bg-blue-700 rounded-lg text-sm font-semibold shadow-lg shadow-blue-900/20 transition">
                  Edit Deskripsi & Foto
                </button>
              </Link>
              <button 
                onClick={handleCancel}
                className="px-4 py-2 bg-red-600 hover:bg-red-700 rounded-lg text-sm font-semibold shadow-lg shadow-red-900/20 transition"
              >
                Batalkan Listing
              </button>
            </div>
          )}
        </div>

        <div className="bg-gray-800 rounded-2xl border border-gray-700 overflow-hidden shadow-2xl flex flex-col md:flex-row">
          <div className="w-full md:w-1/2 bg-gray-900 flex items-center justify-center min-h-[300px]">
            {catalog.gambar ? (
              <img src={catalog.gambar} alt={catalog.judul} className="w-full h-full object-cover" />
            ) : (
              <span className="text-gray-600 flex flex-col items-center">
                <svg className="w-12 h-12 mb-2 opacity-50" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z"></path></svg>
                Tidak Ada Gambar
              </span>
            )}
          </div>
          <div className="p-8 w-full md:w-1/2 flex flex-col">
            <div className="mb-2 inline-block">
              <span className="px-3 py-1 bg-gray-700 text-gray-300 text-xs rounded-full uppercase tracking-wider font-bold">
                {catalog.category?.name || 'Uncategorized'}
              </span>
            </div>
            <h1 className="text-3xl font-extrabold text-white mb-2">{catalog.judul}</h1>
            <p className="text-gray-400 text-sm mb-6 whitespace-pre-wrap">{catalog.deskripsi}</p>
            
            <div className="mt-auto bg-gray-900/50 p-5 rounded-xl border border-gray-700">
              <p className="text-sm text-gray-400 mb-1">Harga Buka</p>
              <p className="text-3xl font-bold text-emerald-400 mb-4">Rp {catalog.hargaAwal.toLocaleString('id-ID')}</p>
              
              {catalog.hargaBeliSekarang && (
                <div className="flex justify-between items-center mb-2">
                  <span className="text-sm text-gray-400">Harga Beli Sekarang</span>
                  <span className="font-semibold text-white">Rp {catalog.hargaBeliSekarang.toLocaleString('id-ID')}</span>
                </div>
              )}
              
              {catalog.endTime && (
                <div className="flex justify-between items-center pt-2 border-t border-gray-700">
                  <span className="text-sm text-gray-400">Waktu Berakhir</span>
                  <span className="font-semibold text-amber-400">{new Date(catalog.endTime).toLocaleString('id-ID')}</span>
                </div>
              )}
            </div>

            {userRole === 'BUYER' && (
              <button className="mt-6 w-full py-3 bg-blue-600 hover:bg-blue-700 font-bold rounded-xl transition shadow-lg shadow-blue-900/20 text-lg">
                Ajukan Tawaran (Bid)
              </button>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
