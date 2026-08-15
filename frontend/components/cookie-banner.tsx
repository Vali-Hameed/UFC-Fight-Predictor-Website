"use client";

import { useState, useEffect } from "react";
import Link from "next/link";

export function CookieBanner() {
  const [showBanner, setShowBanner] = useState(false);

  useEffect(() => {
    // Check if user has already accepted or declined cookies
    const hasConsent = localStorage.getItem("cookieConsent");
    const dismissedSession = sessionStorage.getItem("cookieBannerDismissed");
    if (!hasConsent && !dismissedSession) {
      setShowBanner(true);
    }
  }, []);

  const acceptCookies = () => {
    localStorage.setItem("cookieConsent", "true");
    setShowBanner(false);
  };

  const declineCookies = () => {
    localStorage.setItem("cookieConsent", "false");
    setShowBanner(false);
  };

  const dismissBanner = () => {
    sessionStorage.setItem("cookieBannerDismissed", "true");
    setShowBanner(false);
  };

  if (!showBanner) return null;

  return (
    <div className="fixed bottom-0 left-0 right-0 z-50 border-t border-white/10 bg-bg/95 p-4 pt-8 sm:pt-4 backdrop-blur-xl shadow-2xl">
      <button 
        onClick={dismissBanner}
        className="absolute top-2 right-2 sm:top-4 sm:right-4 text-white/50 hover:text-white transition p-1"
        aria-label="Close"
      >
        ✕
      </button>
      <div className="mx-auto flex max-w-7xl flex-col items-center justify-between gap-4 sm:flex-row sm:px-6 lg:px-8">
        <div className="text-sm text-white/80 pr-6 sm:pr-8">
          We use cookies to enhance your experience, analyze site traffic, and serve tailored content. 
          By continuing to browse or closing this banner, you consent to our use of cookies. 
          Read our <Link href="/privacy" className="text-accent hover:underline">Privacy Policy</Link> for more information.
        </div>
        <div className="flex shrink-0 gap-3">
          <button
            onClick={declineCookies}
            className="rounded-lg border border-white/20 bg-transparent px-4 py-2 text-sm font-semibold text-white transition hover:bg-white/10"
          >
            Decline
          </button>
          <button
            onClick={acceptCookies}
            className="rounded-lg bg-accent px-6 py-2 text-sm font-semibold text-white transition hover:bg-accent/90"
          >
            Accept All
          </button>
        </div>
      </div>
    </div>
  );
}
