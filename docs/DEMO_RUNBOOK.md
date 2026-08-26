# 1. Install
cd "C:\Projects\Dorja Homestation"; pnpm install

# 2. Docker
cd "C:\Projects\Dorja Homestation"; docker compose -f infra/docker-compose.yml up -d

# 3. Wait 10s, check healthy
docker ps --format "{{.Names}}: {{.Status}}"

# 4. Seed (NOW WORKS)
cd "C:\Projects\Dorja Homestation\apps\api"; npx tsx prisma/seed.ts

# 5. API
cd "C:\Projects\Dorja Homestation\apps\api"; npx tsx src/server.ts

# 6. (NEW TERMINAL) Web
cd "C:\Projects\Dorja Homestation\apps\web"; npx next dev --port 3000

# 7. (NEW TERMINAL) Mobile
cd "C:\Projects\Dorja Homestation\apps\mobile"; npx expo start
