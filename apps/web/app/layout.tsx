'use client';

import './globals.css';
import { Compass, Star, Scale, MessageCircle, Calendar, Home, Key, Shield, User } from 'lucide-react';
import { usePathname } from 'next/navigation';

const NAV = [
  { label: 'Explore', href: '/', icon: Compass },
  { label: 'Shortlist', href: '/shortlist', icon: Star },
  { label: 'Compare', href: '/compare', icon: Scale },
  { label: 'Inbox', href: '/inbox', icon: MessageCircle },
  { label: 'Visits', href: '/visits', icon: Calendar },
  { label: 'My Listings', href: '/my-listings', icon: Home },
];

export default function RootLayout({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const isActive = (href: string) => {
    if (href === '/') return pathname === '/';
    return pathname.startsWith(href);
  };

  return (
    <html lang="en">
      <body>
        <nav className="rail">
          <div className="rail-logo">
            <div className="rail-logo-mark">D</div>
            <div className="rail-logo-text">DORJA</div>
          </div>
          <div className="rail-items">
            {NAV.map((item) => (
              <a key={item.href} href={item.href} className={`rail-item ${isActive(item.href) ? 'active' : ''}`}>
                <item.icon size={20} strokeWidth={1.8} />
                <span>{item.label}</span>
              </a>
            ))}
          </div>
          <div className="rail-bottom">
            <a href="/auth" className={`rail-item ${isActive('/auth') ? 'active' : ''}`}>
              <Key size={20} strokeWidth={1.8} />
              <span>Sign In</span>
            </a>
            <a href="/account" className={`rail-item ${isActive('/account') ? 'active' : ''}`}>
              <User size={20} strokeWidth={1.8} />
              <span>Account</span>
            </a>
            <a href="/safety-guide" className={`rail-item ${isActive('/safety-guide') ? 'active' : ''}`}>
              <Shield size={20} strokeWidth={1.8} />
              <span>Safety</span>
            </a>
          </div>
        </nav>
        <main className="main">{children}</main>
      </body>
    </html>
  );
}
