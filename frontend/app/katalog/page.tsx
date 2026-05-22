'use client';
import { useEffect, useState, useCallback } from 'react';
import axiosClient from '@/lib/axiosClient';
import Link from 'next/link';

interface Category {
  id: number;
  name: string;
  subCategories?: Category[];
}

interface Catalog {
  id: number;
  judul: string;
  deskripsi: string;
  hargaAwal: number;
  gambar?: string;
  endTime?: string;
  category?: {
    id: number;
    name: string;
  };
  sellerId?: number;
}

export default function CatalogPage() {
  const [catalogs, setCatalogs] = useState<Catalog[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  
  // Filter States
  const [q, setQ] = useState('');
  const [cat, setCat] = useState<number | ''>('');
  const [min, setMin] = useState<number | ''>('');
  const [max, setMax] = useState<number | ''>('');
  const [endTime, setEndTime] = useState('');
  
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');

  const fetchCategories = useCallback(async () => {
    try {
      const response = await axiosClient.get('/kategori/hierarki');
      setCategories(response.data);
    } catch (err) {
      console.error('Error fetching categories:', err);
    }
  }, []);

  const fetchCatalogs = useCallback(async () => {
    setIsLoading(true);
    setError('');
    try {
      const params = new URLSearchParams();
      if (q) params.append('q', q);
      if (cat) params.append('cat', cat.toString());
      if (min) params.append('min', min.toString());
      if (max) params.append('max', max.toString());
      if (endTime) {
        // convert to ISO string if it's just a date
        const dateObj = new Date(endTime);
        params.append('endTime', dateObj.toISOString());
      }

      const response = await axiosClient.get(`/katalog/search?${params.toString()}`);
      setCatalogs(response.data);
    } catch (err) {
      console.error('Error fetching catalogs:', err);
      setError('Gagal mengambil data katalog.');
    } finally {
      setIsLoading(false);
    }
  }, [q, cat, min, max, endTime]);

  useEffect(() => {
    void fetchCategories();
    void fetchCatalogs();
  }, [fetchCategories, fetchCatalogs]);

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    void fetchCatalogs();
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

  return (
    <div className="min-h-screen bg-gray-900 text-white p-8">
      <div className="max-w-7xl mx-auto">
        <div className="flex justify-between items-center mb-8">
          <div>
            <h1 className="text-4xl font-extrabold text-blue-500 tracking-tight">Browse Auctions</h1>
            <p className="text-gray-400 mt-2">Find the best items and place your bids.</p>
          </div>
          <Link href="/">
            <button className="px-4 py-2 bg-gray-800 hover:bg-gray-700 border border-gray-600 rounded-lg text-sm font-semibold transition">
              Back to Dashboard
            </button>
          </Link>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-4 gap-8">
          {/* Sidebar Filter */}
          <div className="bg-gray-800 p-6 rounded-xl border border-gray-700 h-fit">
            <h2 className="text-xl font-bold mb-4 text-white">Filters</h2>
            <form onSubmit={handleSearch} className="space-y-4">
              <div>
                <label className="block text-sm text-gray-400 mb-1">Search Keyword</label>
                <input
                  type="text"
                  placeholder="e.g., iPhone"
                  value={q}
                  onChange={(e) => setQ(e.target.value)}
                  className="w-full bg-gray-900 border border-gray-600 rounded-md px-3 py-2 text-sm focus:outline-none focus:border-blue-500"
                />
              </div>
              
              <div>
                <label className="block text-sm text-gray-400 mb-1">Category</label>
                <select
                  value={cat}
                  onChange={(e) => setCat(e.target.value === '' ? '' : Number(e.target.value))}
                  className="w-full bg-gray-900 border border-gray-600 rounded-md px-3 py-2 text-sm focus:outline-none focus:border-blue-500"
                >
                  <option value="">All Categories</option>
                  {renderCategoryOptions(categories)}
                </select>
              </div>

              <div className="flex gap-2">
                <div className="flex-1">
                  <label className="block text-sm text-gray-400 mb-1">Min Price</label>
                  <input
                    type="number"
                    value={min}
                    onChange={(e) => setMin(e.target.value === '' ? '' : Number(e.target.value))}
                    className="w-full bg-gray-900 border border-gray-600 rounded-md px-3 py-2 text-sm focus:outline-none focus:border-blue-500"
                  />
                </div>
                <div className="flex-1">
                  <label className="block text-sm text-gray-400 mb-1">Max Price</label>
                  <input
                    type="number"
                    value={max}
                    onChange={(e) => setMax(e.target.value === '' ? '' : Number(e.target.value))}
                    className="w-full bg-gray-900 border border-gray-600 rounded-md px-3 py-2 text-sm focus:outline-none focus:border-blue-500"
                  />
                </div>
              </div>

              <div>
                <label className="block text-sm text-gray-400 mb-1">Ending Before</label>
                <input
                  type="date"
                  value={endTime}
                  onChange={(e) => setEndTime(e.target.value)}
                  className="w-full bg-gray-900 border border-gray-600 rounded-md px-3 py-2 text-sm focus:outline-none focus:border-blue-500"
                />
              </div>

              <button
                type="submit"
                className="w-full mt-4 bg-blue-600 hover:bg-blue-700 py-2 rounded-md font-semibold transition shadow-lg shadow-blue-900/20"
              >
                Apply Filters
              </button>
            </form>
          </div>

          {/* Catalog Grid */}
          <div className="lg:col-span-3">
            {error && <div className="p-4 mb-4 bg-red-500/20 text-red-400 border border-red-500/50 rounded-lg">{error}</div>}
            
            {isLoading ? (
              <div className="flex justify-center items-center h-64">
                <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-500"></div>
              </div>
            ) : catalogs.length === 0 ? (
              <div className="flex flex-col justify-center items-center h-64 bg-gray-800 rounded-xl border border-gray-700">
                <svg className="w-16 h-16 text-gray-600 mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path>
                </svg>
                <p className="text-gray-400 text-lg">No items found matching your filters.</p>
              </div>
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-6">
                {catalogs.map((catalog) => (
                  <Link href={`/katalog/${catalog.id}`} key={catalog.id}>
                    <div className="bg-gray-800 rounded-xl border border-gray-700 overflow-hidden hover:border-blue-500/50 transition cursor-pointer group hover:shadow-xl hover:shadow-blue-900/10 h-full flex flex-col">
                      <div className="h-48 bg-gray-900 flex items-center justify-center overflow-hidden">
                        {catalog.gambar ? (
                          <img src={catalog.gambar} alt={catalog.judul} className="w-full h-full object-cover group-hover:scale-105 transition duration-500" />
                        ) : (
                          <span className="text-gray-600">No Image</span>
                        )}
                      </div>
                      <div className="p-5 flex-1 flex flex-col">
                        <div className="flex justify-between items-start mb-2">
                          <h3 className="font-bold text-lg text-white group-hover:text-blue-400 transition line-clamp-1">{catalog.judul}</h3>
                        </div>
                        <p className="text-emerald-400 font-semibold mb-3 text-lg">Rp {catalog.hargaAwal.toLocaleString('id-ID')}</p>
                        
                        <div className="mt-auto pt-4 border-t border-gray-700 flex justify-between items-center text-xs text-gray-400">
                          <span className="bg-gray-700/50 px-2 py-1 rounded truncate max-w-[120px]">
                            {catalog.category?.name || 'Uncategorized'}
                          </span>
                          {catalog.endTime && (
                            <span className="text-amber-400">
                              Ends {new Date(catalog.endTime).toLocaleDateString()}
                            </span>
                          )}
                        </div>
                      </div>
                    </div>
                  </Link>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
