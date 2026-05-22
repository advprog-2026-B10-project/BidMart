'use client';
import { useState, useEffect, useCallback, useSyncExternalStore } from 'react';
import { useRouter } from 'next/navigation';
import axiosClient from '@/lib/axiosClient';
import Link from 'next/link';

interface Category {
  id: number;
  name: string;
  subCategories?: Category[];
}

export default function CreateCatalogPage() {
  const router = useRouter();
  
  const userRole = useSyncExternalStore(
    () => () => {},
    () => typeof window !== 'undefined' ? localStorage.getItem('role') : null,
    () => null
  );

  const [categories, setCategories] = useState<Category[]>([]);
  const [judul, setJudul] = useState('');
  const [deskripsi, setDeskripsi] = useState('');
  const [hargaAwal, setHargaAwal] = useState('');
  const [hargaBeliSekarang, setHargaBeliSekarang] = useState('');
  const [gambar, setGambar] = useState('');
  const [categoryId, setCategoryId] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState('');

  const fetchCategories = useCallback(async () => {
    try {
      const response = await axiosClient.get('/kategori/hierarki');
      setCategories(response.data);
    } catch (err) {
      console.error('Error fetching categories:', err);
    }
  }, []);

  useEffect(() => {
    if (userRole !== 'SELLER' && userRole !== 'ADMIN' && userRole !== null) {
      alert('Hanya Seller yang dapat membuat listing.');
      router.push('/');
    }
    void fetchCategories();
  }, [fetchCategories, userRole, router]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setIsSubmitting(true);
    
    try {
      const payload = {
        judul,
        deskripsi,
        hargaAwal: Number(hargaAwal),
        hargaBeliSekarang: hargaBeliSekarang ? Number(hargaBeliSekarang) : null,
        gambar,
        category: categoryId ? { id: Number(categoryId) } : null
      };

      const response = await axiosClient.post('/katalog', payload);
      alert('Listing berhasil dibuat!');
      router.push(`/katalog/${response.data.id}`);
    } catch (err: any) {
      console.error(err);
      setError(err.response?.data?.message || 'Gagal membuat listing barang.');
    } finally {
      setIsSubmitting(false);
    }
  };

  const renderCategoryOptions = (cats: Category[], prefix = '') => {
    let options: React.ReactNode[] = [];
    cats.forEach(c => {
      options.push(
        <option key={c.id} value={c.id}>
          {prefix}{c.name}
        </option>
      );
      if (c.subCategories && c.subCategories.length > 0) {
        options = options.concat(renderCategoryOptions(c.subCategories, prefix + '-- '));
      }
    });
    return options;
  };

  if (userRole === null) return null; // loading auth state

  return (
    <div className="min-h-screen bg-gray-900 text-white p-8">
      <div className="max-w-2xl mx-auto">
        <div className="mb-8">
          <Link href="/">
            <button className="flex items-center text-gray-400 hover:text-white transition mb-6">
              <svg className="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M10 19l-7-7m0 0l7-7m-7 7h18"></path></svg>
              Kembali ke Dashboard
            </button>
          </Link>
          <h1 className="text-3xl font-bold text-white mb-2">Buat Listing Baru</h1>
          <p className="text-gray-400">Isi detail barang yang ingin kamu lelang.</p>
        </div>

        <div className="bg-gray-800 p-8 rounded-2xl border border-gray-700 shadow-xl">
          {error && <div className="p-4 mb-6 bg-red-500/20 text-red-400 border border-red-500/50 rounded-lg">{error}</div>}
          
          <form onSubmit={handleSubmit} className="space-y-6">
            <div>
              <label className="block text-sm font-medium text-gray-300 mb-2">Judul Barang <span className="text-red-500">*</span></label>
              <input
                type="text"
                required
                value={judul}
                onChange={(e) => setJudul(e.target.value)}
                className="w-full bg-gray-900 border border-gray-600 rounded-lg px-4 py-3 text-white focus:outline-none focus:border-blue-500 transition"
                placeholder="Misal: iPhone 15 Pro Max 256GB"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-300 mb-2">Deskripsi <span className="text-red-500">*</span></label>
              <textarea
                required
                rows={4}
                value={deskripsi}
                onChange={(e) => setDeskripsi(e.target.value)}
                className="w-full bg-gray-900 border border-gray-600 rounded-lg px-4 py-3 text-white focus:outline-none focus:border-blue-500 transition"
                placeholder="Jelaskan kondisi, spesifikasi, dan kelengkapan barang..."
              />
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div>
                <label className="block text-sm font-medium text-gray-300 mb-2">Harga Buka (Rp) <span className="text-red-500">*</span></label>
                <input
                  type="number"
                  required
                  min="0"
                  value={hargaAwal}
                  onChange={(e) => setHargaAwal(e.target.value)}
                  className="w-full bg-gray-900 border border-gray-600 rounded-lg px-4 py-3 text-white focus:outline-none focus:border-blue-500 transition"
                  placeholder="5000000"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-300 mb-2">Harga Beli Sekarang (Opsional)</label>
                <input
                  type="number"
                  min="0"
                  value={hargaBeliSekarang}
                  onChange={(e) => setHargaBeliSekarang(e.target.value)}
                  className="w-full bg-gray-900 border border-gray-600 rounded-lg px-4 py-3 text-white focus:outline-none focus:border-blue-500 transition"
                  placeholder="10000000"
                />
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-300 mb-2">URL Gambar Barang</label>
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

            <div>
              <label className="block text-sm font-medium text-gray-300 mb-2">Kategori <span className="text-red-500">*</span></label>
              <select
                required
                value={categoryId}
                onChange={(e) => setCategoryId(e.target.value)}
                className="w-full bg-gray-900 border border-gray-600 rounded-lg px-4 py-3 text-white focus:outline-none focus:border-blue-500 transition appearance-none"
              >
                <option value="" disabled>Pilih Kategori</option>
                {renderCategoryOptions(categories)}
              </select>
            </div>

            <button
              type="submit"
              disabled={isSubmitting}
              className={`w-full py-3 mt-4 rounded-xl font-bold text-lg transition shadow-lg ${isSubmitting ? 'bg-blue-800 cursor-not-allowed' : 'bg-blue-600 hover:bg-blue-700 shadow-blue-900/20'}`}
            >
              {isSubmitting ? 'Menyimpan...' : 'Post Listing Barang'}
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}
