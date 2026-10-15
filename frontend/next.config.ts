import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // Docker imajı için yalnız çalışma zamanı dosyalarını üretir (.next/standalone).
  output: "standalone",
  cacheComponents: true,
  partialPrefetching: true,
};

export default nextConfig;
