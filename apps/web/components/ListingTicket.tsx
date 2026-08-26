'use client';

import { Building2, Home, DoorOpen, BedDouble, Briefcase, Store, TreePine, Armchair, MapPin, Clock, ArrowRight, Camera, CheckCircle, Star } from 'lucide-react';

type Props = { listing: any; icon: string; };

const GRADIENTS = [
  'linear-gradient(135deg, #e8e0d4 0%, #c4b5a0 100%)',
  'linear-gradient(135deg, #d7f1ee 0%, #a8d4cf 100%)',
  'linear-gradient(135deg, #e0f2e8 0%, #a8d4b8 100%)',
  'linear-gradient(135deg, #FCE8BE 0%, #E79C2E 100%)',
  'linear-gradient(135deg, #d4dce8 0%, #a0b4c4 100%)',
];

function getGradient(slug: string) {
  let h = 0;
  for (let i = 0; i < slug.length; i++) h = ((h << 5) - h + slug.charCodeAt(i)) | 0;
  return GRADIENTS[Math.abs(h) % GRADIENTS.length];
}

function timeAgo(d: string | null) {
  if (!d) return null;
  const m = Math.floor((Date.now() - new Date(d).getTime()) / 60000);
  if (m < 60) return m + 'm ago';
  const h = Math.floor(m / 60);
  if (h < 24) return h + 'h ago';
  return Math.floor(h / 24) + 'd ago';
}

const ICON_MAP: Record<string, any> = {
  apartment: Building2, house: Home, room: DoorOpen, sublet: BedDouble,
  office: Briefcase, shop: Store, land: TreePine, hostel: Armchair,
};

export function ListingTicket({ listing, icon }: Props) {
  const IconComp = ICON_MAP[icon] || Building2;
  const isLive = listing.livePulseAt && (!listing.livePulseExpiresAt || new Date(listing.livePulseExpiresAt) > new Date());
  const pulse = isLive ? timeAgo(listing.livePulseAt) : null;
  const hasCapture = listing.realityReviewLevel && listing.realityReviewLevel !== 'INCOMPLETE';
  const capDate = listing.realityPublishedAt ? new Date(listing.realityPublishedAt).toLocaleDateString('en-GB', { day: 'numeric', month: 'short', year: 'numeric' }) : null;
  const roomCount = listing.rooms ? listing.rooms.length : 0;

  const handleStar = (e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
    const existing = JSON.parse(localStorage.getItem('dorja_shortlist') || '[]');
    if (!existing.find((s: any) => s.id === listing.id)) {
      existing.push({ id: listing.id, slug: listing.slug, title: listing.title, publicArea: listing.publicArea, priceAmount: listing.priceAmount });
      localStorage.setItem('dorja_shortlist', JSON.stringify(existing));
    }
  };

  return (
    <a href={`/properties/${listing.slug}`} className="listing-card">
      <div className="card-image">
        <div className="card-image-placeholder building" style={{ background: getGradient(listing.slug) }}>
          <IconComp size={48} strokeWidth={1} style={{ color: 'rgba(255,255,255,0.35)' }} />
          <div className="card-image-overlay" />
        </div>
        <div className="card-badges">
          {isLive
            ? <span className="card-badge badge-available"><CheckCircle size={10} /> AVAILABLE</span>
            : <span className="card-badge badge-unconfirmed">UNCONFIRMED</span>}
          {hasCapture && <span className="card-badge badge-captured"><Camera size={10} /> CAPTURED</span>}
        </div>
        <div className="card-intent-badge">{listing.intent === 'RENT' ? 'FOR RENT' : 'FOR SALE'}</div>
        <button onClick={handleStar} style={{ position:'absolute', top:12, right:12, background:'rgba(255,255,255,0.9)', border:'none', borderRadius:'50%', width:32, height:32, display:'flex', alignItems:'center', justifyContent:'center', cursor:'pointer', zIndex:3 }}>
          <Star size={16} style={{ color: 'var(--amber-500)' }} />
        </button>
      </div>

      <div className="card-body">
        <h3 style={{ margin: 0, fontSize: 15, lineHeight: 1.3 }}>{listing.title}</h3>
        <div className="card-price">
          ৳{listing.priceAmount.toLocaleString()}
          {listing.intent === 'RENT' && <span className="card-price-unit"> /month</span>}
        </div>

        <div className="card-location">
          <MapPin size={13} style={{ color: 'var(--sand-400)', flexShrink: 0 }} />
          <span>{listing.publicArea}</span>
          <span className="card-location-dot" />
          <span>{listing.propertyType.replace(/_/g, ' ')}</span>
          {roomCount > 0 && (<><span className="card-location-dot" /><span>{roomCount} rooms</span></>)}
        </div>

        {pulse && (
          <div className="card-pulse">
            <span className="card-pulse-dot" />
            CONFIRMED {pulse}
          </div>
        )}

        <div className="card-footer">
          <span className="card-capture-info">
            {hasCapture ? <><Camera size={11} style={{ verticalAlign: -1 }} /> Seller-captured · {capDate}</> : 'No capture yet'}
          </span>
          <span className="card-action">Open Passport <ArrowRight size={12} style={{ verticalAlign: -2 }} /></span>
        </div>
      </div>
    </a>
  );
}
