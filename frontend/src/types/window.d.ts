declare global {
  interface Window {
    __ENV__?: {
      NEXT_PUBLIC_API_URL: string;
      NEXT_PUBLIC_S3_BUCKET_URL: string;
    };
  }
}

export {};