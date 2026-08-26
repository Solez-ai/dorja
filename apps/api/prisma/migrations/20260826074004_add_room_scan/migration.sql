-- CreateTable
CREATE TABLE "RoomScan" (
    "id" TEXT NOT NULL,
    "listingId" TEXT,
    "userId" TEXT NOT NULL,
    "status" TEXT NOT NULL DEFAULT 'PROCESSING',
    "roomType" TEXT,
    "roomName" TEXT,
    "frameCount" INTEGER NOT NULL DEFAULT 0,
    "coveragePercent" DOUBLE PRECISION,
    "scanData" JSONB,
    "modelUrl" TEXT,
    "thumbnailUrl" TEXT,
    "durationMs" INTEGER,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "RoomScan_pkey" PRIMARY KEY ("id")
);

-- CreateIndex
CREATE INDEX "RoomScan_listingId_idx" ON "RoomScan"("listingId");

-- CreateIndex
CREATE INDEX "RoomScan_userId_idx" ON "RoomScan"("userId");

-- AddForeignKey
ALTER TABLE "RoomScan" ADD CONSTRAINT "RoomScan_listingId_fkey" FOREIGN KEY ("listingId") REFERENCES "Listing"("id") ON DELETE SET NULL ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "RoomScan" ADD CONSTRAINT "RoomScan_userId_fkey" FOREIGN KEY ("userId") REFERENCES "User"("id") ON DELETE RESTRICT ON UPDATE CASCADE;
