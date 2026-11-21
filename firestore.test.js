const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ADMIN_UID = "admin_001";
const ADMIN_EMAIL = "osimakram90644@gmail.com";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function sampleProductPayload(creatorUid, active = true) {
  const now = new Date(Date.now() - 2000);
  return {
    sku: "DG-PAN-001",
    name: "Wildflower Raw Honey",
    description: "Unfiltered local blossom honey in glass jar.",
    category: "Pantry",
    unit: "500g Jar",
    priceCents: 1299,
    compareAtPriceCents: 1599,
    stockCount: 24,
    lowStockThreshold: 5,
    featured: true,
    active,
    iconKey: "honey",
    createdBy: creatorUid,
    createdAt: now,
    updatedAt: now,
  };
}

test("Unauthenticated users cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("products").where("active", "==", true).get());
  await assertFails(unauthDb.collection("orders").get());
  await assertFails(unauthDb.collection("store_settings").doc("config").get());
  await assertFails(unauthDb.collection("products").doc("prod_1").set(sampleProductPayload("anon")));
});

test("Email spoofing: unverified admin email is rejected", async () => {
  const spoofedDb = testEnv.authenticatedContext("spoof_uid", {
    email: ADMIN_EMAIL,
    email_verified: false,
  }).firestore();

  await assertFails(spoofedDb.collection("products").doc("prod_1").set(sampleProductPayload("spoof_uid")));
  await assertFails(spoofedDb.collection("admins").doc("spoof_uid").set({
    uid: "spoof_uid",
    email: ADMIN_EMAIL,
    label: "Spoof",
    addedBy: "spoof_uid",
    createdAt: new Date(Date.now() - 1000),
  }));
});

test("Bootstrapped verified admin can create, update, list all products, and manage store settings", async () => {
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();

  await assertSucceeds(adminDb.collection("products").doc("prod_1").set(sampleProductPayload(ADMIN_UID, false)));
  await assertSucceeds(adminDb.collection("products").get());
  await assertSucceeds(adminDb.collection("products").doc("prod_1").update({
    stockCount: 50,
    active: true,
    updatedAt: new Date(Date.now() - 500),
  }));

  await assertSucceeds(adminDb.collection("store_settings").doc("config").set({
    storeName: "Daily Goods",
    announcement: "Fresh harvest deals this week",
    storeHours: "8:00 AM - 9:00 PM Daily",
    storeOpen: true,
    taxRateBps: 825,
    deliveryFeeCents: 499,
    freeDeliveryThresholdCents: 3500,
    updatedAt: new Date(Date.now() - 500),
  }));
});

test("Shopper cannot create/delete products, inflate stock, or read inactive products", async () => {
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();
  await assertSucceeds(adminDb.collection("products").doc("prod_active").set(sampleProductPayload(ADMIN_UID, true)));
  await assertSucceeds(adminDb.collection("products").doc("prod_draft").set(sampleProductPayload(ADMIN_UID, false)));

  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@example.com",
    email_verified: true,
  }).firestore();

  // Filtered active query succeeds; unfiltered query fails
  await assertSucceeds(aliceDb.collection("products").where("active", "==", true).get());
  await assertFails(aliceDb.collection("products").get());
  await assertFails(aliceDb.collection("products").doc("prod_draft").get());

  // Shopper cannot create, delete, or change price/inflate stock
  await assertFails(aliceDb.collection("products").doc("prod_new").set(sampleProductPayload(ALICE_UID, true)));
  await assertFails(aliceDb.collection("products").doc("prod_active").delete());
  await assertFails(aliceDb.collection("products").doc("prod_active").update({
    priceCents: 1,
    updatedAt: new Date(Date.now() - 500),
  }));
  await assertFails(aliceDb.collection("products").doc("prod_active").update({
    stockCount: 99,
    updatedAt: new Date(Date.now() - 500),
  }));

  // Shopper checkout stock decrement succeeds when strictly decreasing
  await assertSucceeds(aliceDb.collection("products").doc("prod_active").update({
    stockCount: 22,
    updatedAt: new Date(Date.now() - 500),
  }));
});

test("User cart, profile PII isolation, and order lifecycle parity", async () => {
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();
  await assertSucceeds(adminDb.collection("products").doc("prod_1").set(sampleProductPayload(ADMIN_UID, true)));

  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@example.com",
    email_verified: true,
  }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, {
    email: "bob@example.com",
    email_verified: true,
  }).firestore();

  const now = new Date(Date.now() - 2000);
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set({
    userId: ALICE_UID,
    displayName: "Alice",
    email: "alice@example.com",
    loyaltyPoints: 0,
    createdAt: now,
    updatedAt: now,
  }));

  // Bob cannot read or list Alice's profile
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").get());

  // Cart item create & isolation
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("cart").doc("prod_1").set({
    productName: "Wildflower Raw Honey",
    sku: "DG-PAN-001",
    unit: "500g Jar",
    iconKey: "honey",
    unitPriceCents: 1299,
    quantity: 2,
    createdAt: now,
    updatedAt: now,
  }));
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("cart").get());

  // Order creation & isolation
  await assertSucceeds(aliceDb.collection("orders").doc("ord_1").set({
    orderNumber: "DG-1001",
    userId: ALICE_UID,
    customerName: "Alice",
    customerEmail: "alice@example.com",
    deliveryAddress: "123 Market St",
    fulfillmentType: "DELIVERY",
    status: "PENDING",
    items: [{
      productId: "prod_1",
      productName: "Wildflower Raw Honey",
      sku: "DG-PAN-001",
      unit: "500g Jar",
      unitPriceCents: 1299,
      quantity: 2,
    }],
    subtotalCents: 2598,
    deliveryFeeCents: 499,
    taxCents: 214,
    totalCents: 3311,
    itemCount: 2,
    createdAt: now,
    updatedAt: now,
  }));

  // Shopper filtered query succeeds; unfiltered or Bob's read fails
  await assertSucceeds(aliceDb.collection("orders").where("userId", "==", ALICE_UID).get());
  await assertFails(aliceDb.collection("orders").get());
  await assertFails(bobDb.collection("orders").doc("ord_1").get());

  // Shopper cannot set status to DELIVERED, only admin can
  await assertFails(aliceDb.collection("orders").doc("ord_1").update({
    status: "DELIVERED",
    updatedAt: new Date(Date.now() - 500),
  }));
  await assertSucceeds(adminDb.collection("orders").doc("ord_1").update({
    status: "PACKING",
    updatedAt: new Date(Date.now() - 500),
  }));
  // Terminal/non-pending status cannot be cancelled by shopper
  await assertFails(aliceDb.collection("orders").doc("ord_1").update({
    status: "CANCELLED",
    updatedAt: new Date(Date.now() - 400),
  }));
});
