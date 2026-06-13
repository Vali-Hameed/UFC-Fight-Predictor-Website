import type { MetadataRoute } from 'next'

export default function robots(): MetadataRoute.Robots {
  const baseUrl = 'https://fightpicks.net'

  return {
    rules: {
      userAgent: '*',
      allow: '/',
      // Disallow crawlers from accessing admin or private profile pages
      disallow: ['/admin/', '/profile/', '/notifications/'],
    },
    sitemap: `${baseUrl}/sitemap.xml`,
  }
}
