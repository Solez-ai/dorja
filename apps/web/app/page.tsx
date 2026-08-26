import { ListingTicket } from '../components/ListingTicket';
import { Search, Building, Home, DoorOpen, BedDouble, Briefcase, Store, TreePine, Coffee } from 'lucide-react';

async function getListings() {
  try {
    const res = await fetch('http://localhost:4000/v1/listings?page=1&limit=20', { cache: 'no-store' });
    if (!res.ok) return { data: [], meta: { total: 0 } };
    return res.json();
  } catch { return { data: [], meta: { total: 0 } }; }
}

const ROOM_ICONS: Record<string, string> = {
  APARTMENT: 'apartment', HOUSE: 'house', ROOM: 'room', SUBLET: 'sublet',
  OFFICE: 'office', SHOP: 'shop', LAND: 'land', HOSTEL_SEAT: 'hostel',
};

export default async function ExplorePage() {
  const { data: listings, meta } = await getListings();

  return (
    <>
      <div className="page-header">
        <h1>Explore Properties</h1>
        <p>Live availability · Reality Passports · Accountability</p>
        <div style={{ display: 'flex', gap: 8, marginTop: 12, alignItems: 'center' }}>
          <span style={{ fontFamily: 'var(--font-mono)', fontSize: 28, fontWeight: 700, color: 'var(--ink-950)' }}>{meta.total}</span>
          <span style={{ fontSize: 14, color: 'var(--sand-400)' }}>properties listed</span>
        </div>
      </div>

      <div className="search-section">
        <div className="search-box">
          <Search size={18} style={{ color: 'var(--sand-400)', flexShrink: 0 }} strokeWidth={1.8} />
          <input className="search-input" placeholder="Search area, road, or property ID" />
        </div>
      </div>

      <div className="filter-bar">
        {['For rent', 'For sale', 'Family', 'Bachelor', 'Available now', 'Captured tour', 'Verified authority'].map((f) => (
          <button key={f} className="filter-chip">{f}</button>
        ))}
      </div>

      <div className="listings-grid">
        {listings.length === 0 ? (
          <div className="empty-state" style={{ gridColumn: '1 / -1' }}>
            <Building size={48} style={{ color: 'var(--sand-300)', marginBottom: 12 }} strokeWidth={1} />
            <h3>No properties found</h3>
            <p style={{ marginTop: 8 }}>Try adjusting your filters or check back later.</p>
          </div>
        ) : (
          listings.map((listing: any) => (
            <ListingTicket key={listing.id} listing={listing} icon={ROOM_ICONS[listing.propertyType] || 'apartment'} />
          ))
        )}
      </div>
    </>
  );
}
